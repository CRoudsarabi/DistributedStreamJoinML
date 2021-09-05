package distributed_stream_join_with_storm.bolts;


import distributed_stream_join_with_storm.helpers.SamplingHelper;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;


import java.util.*;


public class JsonReaderBolt extends BaseBasicBolt {

    //decides if we should sent the tuples to the sampler
    boolean samplingStage;

    ArrayList<String> mergedKVpairs;
    HashMap<Integer, ArrayList<String>> documentsToBeSentAfterSampleling;
    int tuplesEmitted;
    int componentId;

    //for debugging purposes
    boolean verbose;

    //we do not want to sent tuples to the Precomputer bolt if the partitions have been created
    boolean partitionsCreated = false;

    public JsonReaderBolt(boolean samplingStage, boolean verbose) {
        this.partitionsCreated=false;
        this.samplingStage = samplingStage;
        this.verbose = verbose;
    }

    @Override
    public void prepare(Map config, TopologyContext context) {
        this.tuplesEmitted =0;
        this.componentId = context.getThisTaskId();
    }

    @Override
    public void execute(Tuple tuple, BasicOutputCollector basicOutputCollector) {
        switch (tuple.getSourceStreamId()){
            case "store_tuple":
                JSONObject jsonObject = (JSONObject) tuple.getValue(0);
                int document_id = (int) tuple.getValue(1);

                ArrayList<String> keyValuePairs = new ArrayList<>();
                Set keys = jsonObject.keySet();
                Iterator iterator = keys.iterator();

                while(iterator.hasNext()){
                    String key = (String) iterator.next();
                    Object value = jsonObject.get(key);
                    keyValuePairs.add(key + "&" + value);
                }
                if (samplingStage){
                    basicOutputCollector.emit("sample_tuple", new Values(keyValuePairs,document_id));
                }
                else{
                    ArrayList<String> finalPairs = SamplingHelper.calculateFinalPairs(keyValuePairs, this.mergedKVpairs);

                    if(verbose){
                        tuplesEmitted++;
                        if ((tuplesEmitted) % 100 == 0) {
                            System.out.println("Reader " + componentId + " emitted: " + (tuplesEmitted) + " tuples");
                        }
                    }
                    basicOutputCollector.emit("store_tuple", new Values(finalPairs,document_id));
                    if(!partitionsCreated){
                        basicOutputCollector.emit("process_tuple", new Values(finalPairs,document_id));
                    }

                    //System.out.println("Reader "+componentId+ " emitted: "+ (tuplesEmitted++) + " tuples");
                }
                break;

            case "merged_kv_pairs":
                ArrayList<String> merged_pairs = (ArrayList<String>) tuple.getValue(0);

                if(verbose){
                    System.out.println("Sampling Stage finished. Reader "  + this.componentId +  " has received merged pairs");
                    System.out.println("The merged pairs are: " + merged_pairs);
                }


                this.mergedKVpairs = merged_pairs;
                this.samplingStage = false;
                if (documentsToBeSentAfterSampleling != null && documentsToBeSentAfterSampleling.size() != 0){
                    for (Map.Entry<Integer, ArrayList<String>> entry: documentsToBeSentAfterSampleling.entrySet()) {
                        //sleep could be added here
                        ArrayList<String> finalPairs = SamplingHelper.calculateFinalPairs(entry.getValue(), merged_pairs);

                        if(verbose){
                            tuplesEmitted++;
                            if ((tuplesEmitted) % 100 == 0) {
                                System.out.println("Reader " + componentId + " emitted: " + (tuplesEmitted) + " tuples");
                            }
                        }

                        basicOutputCollector.emit("store_tuple", new Values(finalPairs,entry.getKey()));
                        if(!partitionsCreated){
                            basicOutputCollector.emit("process_tuple", new Values(finalPairs,entry.getKey()));
                        }

                        try {
                            long time = 1;
                            Thread.sleep(time);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;



            case "finished":
                basicOutputCollector.emit("finished", new Values("finished"));
                return;

            case "recalculate_partitions":
                basicOutputCollector.emit("resample", new Values("resample"));
                samplingStage = true;
                break;

            case "partitions_created":
                partitionsCreated = true;
                break;

            default:
                break;
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declareStream("store_tuple", new Fields("kv-pairs","id"));
        outputFieldsDeclarer.declareStream("process_tuple", new Fields("kv-pairs","id"));
        outputFieldsDeclarer.declareStream("sample_tuple", new Fields("kv-pairs","id"));
        outputFieldsDeclarer.declareStream("resample", new Fields("resample"));
        outputFieldsDeclarer.declareStream("finished", new Fields("finished"));
    }
}