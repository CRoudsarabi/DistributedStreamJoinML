package distributed_stream_join_with_storm.bolts;

import distributed_stream_join_with_storm.helpers.MapTuple;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import java.util.ArrayList;
import java.util.Map;


public class PrecomputerBolt extends BaseBasicBolt {

    int tuplesEmitted;
    int componentId;

    //for debugging purposes
    boolean verbose;

    public PrecomputerBolt(boolean verbose){
        this.verbose = verbose;
        this.tuplesEmitted =0;
    }

    @Override
    public void prepare(Map config, TopologyContext context) {

        this.componentId = context.getThisTaskId();
    }

    @Override
    public void execute(Tuple tuple, BasicOutputCollector basicOutputCollector) {
        if(tuple.getSourceStreamId().equals("process_tuple")){

            ArrayList<String> keyValuePairs = (ArrayList<String>) tuple.getValue(0);
            int document_id = (int) tuple.getValue(1);
            int numberOfPairs = keyValuePairs.size();
            //calculation can be found in helpers.MapTuple class
            MapTuple mt = MapTuple.calcCountAndCoOccurances(keyValuePairs);
            if (verbose) {
                tuplesEmitted++;
                if ((tuplesEmitted) % 100 == 0) {
                    System.out.println("PreComputer " + componentId + " emitted: " + (tuplesEmitted) + " tuples");
                    System.out.println(mt.getKeyValuesCount()+" "+mt.getKeyValuesCoOccurences()+" "+document_id);
                }
                //System.out.println(mt.getKeyValuesCount()+" "+mt.getKeyValuesCoOccurences()+" "+document_id);
            }

            basicOutputCollector.emit("store_tuple", new Values(mt.getKeyValuesCount(),mt.getKeyValuesCoOccurences(),document_id));
        }
    }



    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declareStream("store_tuple", new Fields("kv-count", "kv-CoOccurences", "id"));
    }
}


