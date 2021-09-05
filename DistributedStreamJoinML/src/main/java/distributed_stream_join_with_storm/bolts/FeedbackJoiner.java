package distributed_stream_join_with_storm.bolts;

import distributed_stream_join_with_storm.helpers.Document;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;


import java.util.*;


public class FeedbackJoiner extends BaseBasicBolt {

    HashMap<Long, Long> docsPerCluster;
    HashMap<Set<Long>, Long> overlapBetweenClusters;
    private List<Document> documentsForCurrentWindow;
    private int boltId;
    private int currentWindowsAssignersFinished;
    private int totalNumberofAssigners;
    boolean verbose;


    public FeedbackJoiner(int totalNumberofAssigners, boolean verbose) {
        this.verbose = verbose;
        this.totalNumberofAssigners = totalNumberofAssigners;

    }


    @Override
    public void execute(Tuple tuple, BasicOutputCollector basicOutputCollector) {

        if (tuple.getSourceStreamId().equals("all_docs_sent")){
            currentWindowsAssignersFinished++;
        }
        else{

            ArrayList<String> kvPairs = (ArrayList<String>) tuple.getValue(0);
            Map<String, String>  kvPairsMap = Document.createFromList(kvPairs);
            Set<Long> clusters = (Set<Long> ) tuple.getValue(1);
            Document received = new Document(clusters,kvPairsMap);
            documentsForCurrentWindow.add(received);

            if(verbose){
                if (documentsForCurrentWindow.size() % 500 == 0){
                    System.out.println("Joiner " + boltId+" has received " +documentsForCurrentWindow.size()+ " documents");
                    /*
                    System.out.println("With clusters: " + clusters);
                    System.out.println(clusters.getClass());
                    for (Object cluster : clusters){
                        System.out.println(cluster.getClass());
                    }*/
                }

            }
            for (Long cluster : clusters){
                if (docsPerCluster.containsKey(cluster)){
                    Long oldValue = docsPerCluster.get(cluster);
                    docsPerCluster.put(cluster, (oldValue+1));
                }
                else {
                    docsPerCluster.put(cluster, 1L);
                }
            }

        }
        if(currentWindowsAssignersFinished >= totalNumberofAssigners) {
            if(verbose){
                System.out.println("Joiner " + boltId+" starting the Join");
            }
            performJoin(1);
            if(verbose && !docsPerCluster.isEmpty()){
                System.out.println("Joiner " + boltId+" docs per cluster : " + docsPerCluster);
                System.out.println("Joiner " + boltId+" Overlap : " + encodeOverlapMapToString(overlapBetweenClusters));
            }
            if (!docsPerCluster.isEmpty()) {
                basicOutputCollector.emit("cluster_overlap", new Values(docsPerCluster, encodeOverlapMapToString(overlapBetweenClusters)));
            }
            currentWindowsAssignersFinished = 0;
            docsPerCluster = new HashMap<>();
            overlapBetweenClusters = new HashMap<>();
            documentsForCurrentWindow= new ArrayList<>();
        }

    }

    @Override
    public void prepare(Map stormConf, TopologyContext context) {
        this.currentWindowsAssignersFinished = 0;
        this.docsPerCluster = new HashMap<>();
        this.overlapBetweenClusters = new HashMap<>();
        this.documentsForCurrentWindow= new ArrayList<>();
        this.boltId = context.getThisTaskId();
    }

    public void performJoin(int requiredMatches){
        for (Document doc1: documentsForCurrentWindow){
            for (Document doc2: documentsForCurrentWindow){
                int matches = Document.join(doc1,doc2);
                //TODO parameterise
                if (matches >= requiredMatches){
                    for (Long cluster: doc1.getPartOfClusters() ) {
                        for (Long cluster2: doc2.getPartOfClusters()) {
                            Set<Long> clusters = new HashSet();
                            clusters.add(cluster);
                            clusters.add(cluster2);
                            if (overlapBetweenClusters.containsKey(clusters)){
                                Long oldValue = overlapBetweenClusters.get(clusters);
                                overlapBetweenClusters.put(clusters, (oldValue+1));
                            }
                            else {
                                overlapBetweenClusters.put(clusters, 1L);
                            }
                        }

                    }

                }
            }
        }
    }


    // this String can be converted to a dictionary in python
    public static String encodeOverlapMapToString(HashMap<Set<Long>, Long> map){
        String mapString = "{";
        for (Map.Entry<Set<Long>, Long> entry : map.entrySet() ) {
            Set<Long> clusters = entry.getKey();
            String set = "'";
            for (Long i: clusters
                 ) {
                set += i +"-";
            }
            set = set.substring(0, set.length() -1 );
            set+= "'";
            Long overlap = entry.getValue();
            mapString += " "+set+":"+overlap+",";
        }
        mapString = mapString.substring(0, mapString.length() -1 );
        mapString += "}";
        return mapString;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declareStream("join_performed", new Fields("msg"));
        outputFieldsDeclarer.declareStream("cluster_overlap", new Fields("cluster_sizes","cluster_weights"));
    }
}
