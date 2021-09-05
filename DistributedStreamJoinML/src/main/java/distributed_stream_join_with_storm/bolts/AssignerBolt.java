package distributed_stream_join_with_storm.bolts;


import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.util.*;

public class AssignerBolt extends BaseBasicBolt {

    boolean partitionReceived;
    HashMap<String, Long> partitionedKeys;
    HashMap<Long, Long> partitionedClusters;
    ArrayList<ArrayList<String>> documentsToBeAssigned;
    int check_quality_every;
    int doc_counter;
    double fractionOfMissingAvpairs;
    double resample_threshold;
    boolean verbose;
    int componentId;
    List<Integer> joinerBoltIds;
    private int currentWindowsReadersFinished;
    private int totalNumberofReaders;


    public AssignerBolt(int check_quality_every, boolean verbose,int totalNumberofReaders) {
        this.totalNumberofReaders = totalNumberofReaders;
        this.verbose = verbose;
        this.partitionReceived = false;
        this.doc_counter = 0;
        this.fractionOfMissingAvpairs = 0.0;
        this.resample_threshold = 0.2;
        this.check_quality_every = check_quality_every;
        this.documentsToBeAssigned = new ArrayList<>();
    }

    @Override
    public void prepare(Map config, TopologyContext context) {
        this.currentWindowsReadersFinished = 0;
        this.joinerBoltIds = context.getComponentTasks("feedback_joiner_bolt");
        //System.out.println(this.joinerBoltIds);
        this.componentId = context.getThisTaskId();
    }

    @Override
    public void execute(Tuple tuple, BasicOutputCollector basicOutputCollector) {

        //System.out.println(this.joinerBoltIds);
        if(doc_counter % check_quality_every == 0 && doc_counter != 0){
            if ((fractionOfMissingAvpairs/doc_counter) > resample_threshold){
                basicOutputCollector.emit("recalculate_partitions", new Values());
            }
            fractionOfMissingAvpairs = 0.0;
        }

        switch (tuple.getSourceStreamId()){
            case "store_tuple":
                ArrayList<String> keyValuePairs = (ArrayList<String>) tuple.getValue(0);
                int document_id = (int) tuple.getValue(1);

                if(verbose){
                    if ((documentsToBeAssigned.size()) % 1000 == 0) {
                        System.out.println("Assigner " + componentId + " received " + (documentsToBeAssigned.size()) + " tuples");
                    }
                }

                if(partitionReceived){
                    Set<Long>  clusters = calculateClusters(keyValuePairs);
                    Set<Long>  joiner_ids = calculateJoinerIDs(clusters);
                    for (Long id: joiner_ids) {
                        int joinerID;
                        if(joinerBoltIds.size() <= id.intValue()){
                            joinerID = joinerBoltIds.get(joinerBoltIds.size()-1) ;
                        }
                        else{
                            joinerID = joinerBoltIds.get(id.intValue()) ;;
                        }

                        basicOutputCollector.emitDirect(joinerID, new Values(keyValuePairs, clusters));
                    }
                    doc_counter++;
                }
                else{
                    documentsToBeAssigned.add(keyValuePairs);
                }
                if(doc_counter >= check_quality_every){
                    boolean need_to_recalculate = testPerformance();
                    if(need_to_recalculate){
                        basicOutputCollector.emit("recalculate_partitions", new Values());
                    }

                }
                break;
            case "partitions_created":

                //Key assigned to Cluster
                partitionedKeys = (HashMap<String, Long>) tuple.getValue(0);
                //Cluster assigned to partition (joiner)
                HashMap<String, List<Long>> partitionedClustersReceived = (HashMap<String, List<Long>>) tuple.getValue(1);
                partitionReceived=true;
                partitionedClusters = invertList(partitionedClustersReceived);
                if(verbose) {
                    System.out.println("partitions created has been received !");
                    System.out.println(partitionedClusters);

                }

                for (ArrayList<String> document: documentsToBeAssigned) {
                    Set<Long>  clusters = calculateClusters(document);
                    Set<Long>  joiner_ids = calculateJoinerIDs(clusters);

                    for (Long id: joiner_ids) {
                        int joinerID;
                        if(joinerBoltIds.size() <= id.intValue()){
                            joinerID = joinerBoltIds.get(joinerBoltIds.size()-1) ;
                        }
                        else{
                            joinerID = joinerBoltIds.get(id.intValue()) ;;
                        }
                        basicOutputCollector.emitDirect(joinerID, new Values(document, clusters));
                    }
                    doc_counter++;

                    if (verbose){
                        if ((doc_counter % 2000) == 0){
                            System.out.println("Assigner " + componentId + " has sent " + (doc_counter) + " documents");
                            System.out.println("Joiner Bolt ids:"+this.joinerBoltIds);
                            System.out.println("Clusters: " + clusters);
                            System.out.println("Joiner Ids: " + joiner_ids);
                            System.out.println("Clusters to Joiner: " + partitionedClusters);
                            for (Long id: joiner_ids) {
                                System.out.println("Joiner ID maps to JoinerBolt ID:  "+joinerBoltIds.get(id.intValue()));
                            }
                        }
                    }

                    try {
                        long time = 1;
                        Thread.sleep(time);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                documentsToBeAssigned = new ArrayList<>();
                break;
            case "finished":
                currentWindowsReadersFinished++;
                break;
            default:
                break;

        }
        if ((currentWindowsReadersFinished >= totalNumberofReaders) && documentsToBeAssigned.isEmpty()){
            basicOutputCollector.emit("all_docs_sent", new Values("finished"));
        }


    }

    public static HashMap<Long, Long> invertList(HashMap<String, List<Long>> partitionedClustersReceived) {
        HashMap<Long, Long> invertedList = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : partitionedClustersReceived.entrySet()){
            for (Long l : entry.getValue()){
                invertedList.put(l, Long.parseLong(entry.getKey()));
            }
        }

        return invertedList;
    }

    private boolean testPerformance() {
        return false;
    }

    private Set<Long> calculateClusters(ArrayList<String> keyValuePairs) {

        int numberOfMissingAVpairs = 0;
        Set<Long> clusters = new HashSet<>();
        for (String pair: keyValuePairs) {
            Long id = partitionedKeys.get(pair);
            if (id==null){
                numberOfMissingAVpairs++;
            }
            else{
                clusters.add(id);
            }
        }
        this.fractionOfMissingAvpairs += (numberOfMissingAVpairs/ keyValuePairs.size());
        return clusters;
    }

    private Set<Long> calculateJoinerIDs(Set<Long> keyValuePairs) {

        Set<Long> joiner_ids = new HashSet<>();
        for (Long cluster : keyValuePairs) {
            Long id = partitionedClusters.get(cluster);
            if (id==null){
                //this would be an error since a cluster would not have been assigned to a partition
            }
            else{
                joiner_ids.add(id);
            }
        }
        return joiner_ids;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(true,new Fields("document-content","document-name"));
        outputFieldsDeclarer.declareStream("recalculate_partitions", new Fields("msg"));
        outputFieldsDeclarer.declareStream("partitions_created", new Fields("msg"));
        outputFieldsDeclarer.declareStream("all_docs_sent",new Fields("msg"));

    }
}
