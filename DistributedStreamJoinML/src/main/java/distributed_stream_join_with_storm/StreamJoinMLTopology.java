package distributed_stream_join_with_storm;


import distributed_stream_join_with_storm.bolts.*;
import distributed_stream_join_with_storm.spouts.CSVSpout;
import distributed_stream_join_with_storm.spouts.SimpleSpout;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;


public class StreamJoinMLTopology {


    private static boolean nobench = true;
    private static int numberOfDocumentsToEmit = 30_000;
    private static int numberOfJoinerBolts = 3;
    private static int numberOfAssignerBolts = 2;
    private static int numberOfReaderBolts = 2;
    private static int numberOfDocumentsToBeSampled = 1000;
    private static int qualityCheckPeriod = 20_000;
    private static boolean SamplingStage = true;
    private static boolean UseVirtualEnv = true;

    /* Important:
    * Need to set values for parallelism, number of traning episodes, with feedback, and clustering type in ClustererBolt.py
    *  */
    private static boolean with_feedback = true;

    public static void main(String[] args){

        TopologyBuilder builder = new TopologyBuilder();

        if(nobench){
            SimpleSpout spout_stream = new SimpleSpout(numberOfDocumentsToEmit);
            builder.setSpout("spout_stream",spout_stream,1);
        }
        else{
            //real world data
            CSVSpout spout_stream = new CSVSpout(numberOfDocumentsToEmit);
            builder.setSpout("spout_stream",spout_stream,1);
        }


        JsonReaderBolt jsonReaderBolt = new JsonReaderBolt(SamplingStage, false);
        builder.setBolt("reader_bolt", jsonReaderBolt , numberOfReaderBolts)
                .shuffleGrouping("spout_stream", "store_tuple")
                .allGrouping("spout_stream", "finished")
                .allGrouping("sampler_bolt", "merged_kv_pairs")
                .allGrouping("assigner_bolt", "recalculate_partitions")
                .allGrouping("assigner_bolt", "partitions_created");

        SamplerBolt samplerBolt = new SamplerBolt(numberOfDocumentsToBeSampled, numberOfJoinerBolts);
        builder.setBolt("sampler_bolt", samplerBolt, 1)
                .allGrouping("reader_bolt", "sample_tuple" )
                .allGrouping("reader_bolt", "resample");

        PrecomputerBolt preComputerBolt = new PrecomputerBolt(false);
        builder.setBolt("precomputer_bolt", preComputerBolt , 2)
                .shuffleGrouping("reader_bolt", "process_tuple");


        Path path = Paths.get(System.getProperty("user.dir"));
        String directory = path.getParent().toString();
        directory = directory+"\\Code\\";
        String pathToClass = directory+"ClustererBoltPython.py";
        String pathToENV = directory;
        if (UseVirtualEnv){
            pathToENV = pathToENV+ "\\venv\\LIB\\site-packages";
        }
        //using clusterer Bolt implemented in python
        ClustererBolt clustererBolt = new ClustererBolt(numberOfJoinerBolts, with_feedback, pathToClass);
        Map env = new HashMap();
        env.put("PYTHONPATH", pathToENV);
        clustererBolt.setEnv(env);
        if(with_feedback){
            builder.setBolt("clusterer_bolt", clustererBolt, 1)
                    .allGrouping("precomputer_bolt", "store_tuple")
                    .allGrouping("assigner_bolt", "recalculate_partitions")
                    .allGrouping("feedback_joiner_bolt", "cluster_overlap");
        }
        else{
            builder.setBolt("clusterer_bolt", clustererBolt, 1)
                    .allGrouping("precomputer_bolt", "store_tuple" )
                    .allGrouping("assigner_bolt", "recalculate_partitions");
        }

        AssignerBolt assignerBolt = new AssignerBolt(qualityCheckPeriod, false, numberOfReaderBolts);
        builder.setBolt("assigner_bolt", assignerBolt, numberOfAssignerBolts)
                .shuffleGrouping("reader_bolt", "store_tuple")
                .allGrouping("reader_bolt", "finished")
                .allGrouping("clusterer_bolt", "partitions_created")
                .allGrouping("assigner_bolt", "recalculate_partitions");

        FeedbackJoiner feedbackJoinerBolt = new FeedbackJoiner(numberOfAssignerBolts, false);
        builder.setBolt("feedback_joiner_bolt", feedbackJoinerBolt, numberOfJoinerBolts)
                .directGrouping("assigner_bolt")
                .allGrouping("assigner_bolt", "all_docs_sent");


        /**
         * initialization of FPTreeJoinerBolt
        FPTreeJoinerBolt fpTreeJoinerBolt = new FPTreeJoinerBolt(2);
        builder.setBolt("fp_tree_joiner_bolt", fpTreeJoinerBolt, numberOfJoinerBolts)
                //receive documents from the AssignerBolt
                .directGrouping("assigner_bolt")
                //FPTreeJoinerBolt performs the join algorithm once all the documents for the window have been computed (the same can be implemented with tick tuples)
                .allGrouping("assigner_bolt", CommunicationMessages.assigner_all_local_docs_sent);
        */


        Config conf = new Config();
        conf.setDebug(true);


        // needs to be tuned depending how long the learning takes to prevent a timeout
        conf.put(Config.TOPOLOGY_SUBPROCESS_TIMEOUT_SECS, 1200);

        //on cluster
        if(args!= null && args.length > 0){
            conf.setNumWorkers(2);
            try {
                StormSubmitter.submitTopology(args[0], conf, builder.createTopology());
            } catch (AlreadyAliveException | InvalidTopologyException | AuthorizationException e) {
                e.printStackTrace();
            }
        }
        //local
        else{
            conf.setMaxTaskParallelism(3);
            LocalCluster cluster = new LocalCluster();

            cluster.submitTopology("stream-join-ml", conf, builder.createTopology());

            try {
                Thread.sleep(5*60*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            cluster.shutdown();
        }

    }

}
