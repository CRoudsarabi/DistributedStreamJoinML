package distributed_stream_join_with_storm.tests;

import distributed_stream_join_with_storm.bolts.*;
import distributed_stream_join_with_storm.spouts.SimpleSpout;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;

import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SimpleTestTopology {


    public static void main(String[] args){
        //testTopologySimple();
        //testTopologyReader();
        //testTopologyPreprocessor();
        //testTopologyPython(args);
        //testTopologySampler();
        //testTopologyPython2();
        //testTopologyAssigner();
        //testTopologyWithoutFeedback();
        testTopologyWithFeedback();

    }

    public static void testTopologySimple(){

        TopologyBuilder builder = new TopologyBuilder();


        SimpleSpout spout_stream = new SimpleSpout(1000);
        builder.setSpout("spout_stream",spout_stream,1);

        SimpleTestBolt simpleTestBolt = new SimpleTestBolt();
        builder.setBolt("simple_bolt", simpleTestBolt , 10)
                .shuffleGrouping("spout_stream", "store_tuple")
                .allGrouping("spout_stream", "finished");

        Config conf = new Config();
        conf.setDebug(true);

        //on cluster
        conf.setNumWorkers(2);

        LocalCluster cluster =new LocalCluster();
        cluster.submitTopology("test", conf, builder.createTopology());

    }
    public static void testTopologyReader(){

        TopologyBuilder builder = new TopologyBuilder();


        SimpleSpout spout_stream = new SimpleSpout(1000);
        builder.setSpout("spout_stream",spout_stream,1);

        JsonReaderBolt jsonReaderBolt = new JsonReaderBolt(false, true);
        builder.setBolt("reader_bolt", jsonReaderBolt , 2)
                .shuffleGrouping("spout_stream", "store_tuple")
                .allGrouping("spout_stream", "finished");


        Config conf = new Config();
        conf.setDebug(true);

        //on cluster
        conf.setNumWorkers(2);

        LocalCluster cluster =new LocalCluster();
        cluster.submitTopology("test", conf, builder.createTopology());

    }

    public static void testTopologyPreprocessor(){

        TopologyBuilder builder = new TopologyBuilder();


        SimpleSpout spout_stream = new SimpleSpout(10000);
        builder.setSpout("spout_stream",spout_stream,1);

        JsonReaderBolt jsonReaderBolt = new JsonReaderBolt(false, true);
        builder.setBolt("reader_bolt", jsonReaderBolt , 2)
                .shuffleGrouping("spout_stream", "store_tuple")
                .allGrouping("spout_stream", "finished");


        PrecomputerBolt preComputerBolt = new PrecomputerBolt(true);
        builder.setBolt("precomputer_bolt", preComputerBolt , 2)
                .shuffleGrouping("reader_bolt", "process_tuple");

        Config conf = new Config();
        conf.setDebug(false);

        //on cluster
        conf.setNumWorkers(2);

        LocalCluster cluster =new LocalCluster();
        cluster.submitTopology("test", conf, builder.createTopology());

    }

    public static void testTopologySampler(){

        TopologyBuilder builder = new TopologyBuilder();


        SimpleSpout spout_stream = new SimpleSpout(10000);
        builder.setSpout("spout_stream",spout_stream,1);

        JsonReaderBolt jsonReaderBolt = new JsonReaderBolt(true, true);
        builder.setBolt("reader_bolt", jsonReaderBolt , 2)
                .shuffleGrouping("spout_stream", "store_tuple")
                .allGrouping("spout_stream", "finished")
                .allGrouping("sampler_bolt", "merged_kv_pairs");
                //.allGrouping("assigner_bolt", "recalculate_partitions");

        SamplerBolt samplerBolt = new SamplerBolt(1000, 2);
        builder.setBolt("sampler_bolt", samplerBolt, 1)
                .allGrouping("reader_bolt", "sample_tuple" )
                .allGrouping("reader_bolt", "resample" );

        PrecomputerBolt preComputerBolt = new PrecomputerBolt(true);
        builder.setBolt("precomputer_bolt", preComputerBolt , 4)
                .shuffleGrouping("reader_bolt", "process_tuple");

        Config conf = new Config();
        conf.setDebug(false);

        //on cluster
        conf.setNumWorkers(2);

        LocalCluster cluster =new LocalCluster();
        cluster.submitTopology("test", conf, builder.createTopology());

    }

    public static void testTopologyAssigner(){

        TopologyBuilder builder = new TopologyBuilder();


        SimpleSpout spout_stream = new SimpleSpout(10000);
        builder.setSpout("spout_stream",spout_stream,1);

        JsonReaderBolt jsonReaderBolt = new JsonReaderBolt(true, true);
        builder.setBolt("reader_bolt", jsonReaderBolt , 2)
                .shuffleGrouping("spout_stream", "store_tuple")
                .allGrouping("spout_stream", "finished")
                .allGrouping("sampler_bolt", "merged_kv_pairs");
        //.allGrouping("assigner_bolt", "recalculate_partitions");

        SamplerBolt samplerBolt = new SamplerBolt(1000, 2);
        builder.setBolt("sampler_bolt", samplerBolt, 1)
                .allGrouping("reader_bolt", "sample_tuple" )
                .allGrouping("reader_bolt", "resample" );

        PrecomputerBolt preComputerBolt = new PrecomputerBolt(false);
        builder.setBolt("precomputer_bolt", preComputerBolt , 4)
                .shuffleGrouping("reader_bolt", "process_tuple");

        AssignerBolt assignerBolt = new AssignerBolt(10000, true, 2);
        builder.setBolt("assigner_bolt", assignerBolt, 2)
                .shuffleGrouping("reader_bolt", "store_tuple")
                .allGrouping("reader_bolt", "finished")
                .allGrouping("assigner_bolt", "recalculate_partitions");

        Config conf = new Config();
        conf.setDebug(false);

        //on cluster
        conf.setNumWorkers(2);

        LocalCluster cluster =new LocalCluster();
        cluster.submitTopology("test", conf, builder.createTopology());

    }

    public static void testTopologyPython(String[] args){

        TopologyBuilder builder = new TopologyBuilder();


        SimpleSpout spout_stream = new SimpleSpout(10);
        builder.setSpout("spout_stream",spout_stream,1);

        Path path = Paths.get(System.getProperty("user.dir"));
        String directory = path.getParent().toString();
        directory = directory+"\\Code\\";
        String pathToClass = directory+"TestBoltPython.py";
        String pathToENV = directory+"\\venv\\LIB\\site-packages";
        //System.out.println(directory);
        //System.out.println(pathToClass);


        PythonTestBolt pythonTestBolt = new PythonTestBolt(pathToClass);
        Map env = new HashMap();
        env.put("PYTHONPATH", pathToENV);
        pythonTestBolt.setEnv(env);
        builder.setBolt("python", pythonTestBolt, 1)
                .shuffleGrouping("spout_stream", "store_tuple")
                .allGrouping("spout_stream", "finished");


        ReceiverTestBolt receiverTestBolt = new ReceiverTestBolt();
        builder.setBolt("receiver_bolt", receiverTestBolt , 1).shuffleGrouping("python","kv-pairs")
                .allGrouping("python", "finished");

        Config conf = new Config();
        conf.setDebug(true);

        //on cluster
        conf.setNumWorkers(2);

        if(args!= null && args.length > 0){
            conf.setNumWorkers(2);

            try {
                StormSubmitter.submitTopology(args[0], conf, builder.createTopology());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            LocalCluster cluster =new LocalCluster();
            cluster.submitTopology("test", conf, builder.createTopology());
        }


    }

    public static void testTopologyPython2(){

        TopologyBuilder builder = new TopologyBuilder();



        SimpleSpout spout_stream = new SimpleSpout(10000);
        builder.setSpout("spout_stream",spout_stream,1);

        JsonReaderBolt jsonReaderBolt = new JsonReaderBolt(true, true);
        builder.setBolt("reader_bolt", jsonReaderBolt , 2)
                .shuffleGrouping("spout_stream", "store_tuple")
                .allGrouping("spout_stream", "finished")
                .allGrouping("sampler_bolt", "merged_kv_pairs");
        //.allGrouping("assigner_bolt", "recalculate_partitions");

        SamplerBolt samplerBolt = new SamplerBolt(2000, 2);
        builder.setBolt("sampler_bolt", samplerBolt, 1)
                .allGrouping("reader_bolt", "sample_tuple" )
                .allGrouping("reader_bolt", "resample" );

        PrecomputerBolt preComputerBolt = new PrecomputerBolt(true);
        builder.setBolt("precomputer_bolt", preComputerBolt , 4)
                .shuffleGrouping("reader_bolt", "process_tuple");

        Path path = Paths.get(System.getProperty("user.dir"));
        String directory = path.getParent().toString();
        directory = directory+"\\Code\\";
        String pathToClass = directory+"AdvancedTestBoltPython.py";
        String pathToENV = directory+"\\venv\\LIB\\site-packages";
        //System.out.println(directory);
        //System.out.println(pathToClass);


        PythonTestBolt pythonTestBolt = new PythonTestBolt(pathToClass);
        Map env = new HashMap();
        env.put("PYTHONPATH", pathToENV);
        pythonTestBolt.setEnv(env);
        builder.setBolt("python", pythonTestBolt, 1)
                .allGrouping("precomputer_bolt", "store_tuple");

        ReceiverTestBolt receiverTestBolt = new ReceiverTestBolt();
        builder.setBolt("receiver_bolt", receiverTestBolt , 2)
                .shuffleGrouping("python","fused_maps")
                .shuffleGrouping("python","clustering")
                .allGrouping("python", "finished");

        Config conf = new Config();
        conf.setDebug(false);
        conf.put(Config.TOPOLOGY_SUBPROCESS_TIMEOUT_SECS, 360);

        //on cluster
        conf.setNumWorkers(2);

        LocalCluster cluster =new LocalCluster();
        cluster.submitTopology("test", conf, builder.createTopology());

    }

    public static void testTopologyWithoutFeedback(){

        TopologyBuilder builder = new TopologyBuilder();


        SimpleSpout spout_stream = new SimpleSpout(10000);
        builder.setSpout("spout_stream",spout_stream,1);

        JsonReaderBolt jsonReaderBolt = new JsonReaderBolt(true, false);
        builder.setBolt("reader_bolt", jsonReaderBolt , 2)
                .shuffleGrouping("spout_stream", "store_tuple")
                .allGrouping("spout_stream", "finished")
                .allGrouping("sampler_bolt", "merged_kv_pairs");
        //.allGrouping("assigner_bolt", "recalculate_partitions");

        SamplerBolt samplerBolt = new SamplerBolt(1000, 2);
        builder.setBolt("sampler_bolt", samplerBolt, 1)
                .allGrouping("reader_bolt", "sample_tuple" )
                .allGrouping("reader_bolt", "resample" );

        PrecomputerBolt preComputerBolt = new PrecomputerBolt(false);
        builder.setBolt("precomputer_bolt", preComputerBolt , 1)
                .shuffleGrouping("reader_bolt", "process_tuple");



        Path path = Paths.get(System.getProperty("user.dir"));
        String directory = path.getParent().toString();
        directory = directory+"\\Code\\";
        String pathToClass = directory+"ClustererBoltPython.py";
        String pathToENV = directory;

        pathToENV = pathToENV+ "\\venv\\LIB\\site-packages";

        ClustererBolt clustererBolt = new ClustererBolt(3, false, pathToClass);
        Map env = new HashMap();
        env.put("PYTHONPATH", pathToENV);
        clustererBolt.setEnv(env);

        builder.setBolt("clusterer_bolt", clustererBolt, 1)
                    .allGrouping("precomputer_bolt", "store_tuple" );

        AssignerBolt assignerBolt = new AssignerBolt(10000, true, 2);
        builder.setBolt("assigner_bolt", assignerBolt, 2)
                .shuffleGrouping("reader_bolt", "store_tuple")
                .allGrouping("reader_bolt", "finished")
                .allGrouping("clusterer_bolt", "partitions_created")
                .allGrouping("assigner_bolt", "recalculate_partitions");

        FeedbackJoiner feedbackJoinerBolt = new FeedbackJoiner(2, true);
        builder.setBolt("feedback_joiner_bolt", feedbackJoinerBolt, 3)
                .directGrouping("assigner_bolt")
                .allGrouping("assigner_bolt", "all_docs_sent");

        Config conf = new Config();
        conf.setDebug(false);
        conf.put(Config.TOPOLOGY_SUBPROCESS_TIMEOUT_SECS, 1200);

        //on cluster
        conf.setNumWorkers(2);

        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology("test", conf, builder.createTopology());
    }

    public static void testTopologyWithFeedback(){

        TopologyBuilder builder = new TopologyBuilder();


        SimpleSpout spout_stream = new SimpleSpout(5000);
        builder.setSpout("spout_stream",spout_stream,1);

        JsonReaderBolt jsonReaderBolt = new JsonReaderBolt(true, false);
        builder.setBolt("reader_bolt", jsonReaderBolt , 2)
                .shuffleGrouping("spout_stream", "store_tuple")
                .allGrouping("spout_stream", "finished")
                .allGrouping("sampler_bolt", "merged_kv_pairs");
        //.allGrouping("assigner_bolt", "recalculate_partitions");

        SamplerBolt samplerBolt = new SamplerBolt(1000, 2);
        builder.setBolt("sampler_bolt", samplerBolt, 1)
                .allGrouping("reader_bolt", "sample_tuple" )
                .allGrouping("reader_bolt", "resample" );

        PrecomputerBolt preComputerBolt = new PrecomputerBolt(false);
        builder.setBolt("precomputer_bolt", preComputerBolt , 1)
                .shuffleGrouping("reader_bolt", "process_tuple");



        Path path = Paths.get(System.getProperty("user.dir"));
        String directory = path.getParent().toString();
        directory = directory+"\\Code\\";
        String pathToClass = directory+"ClustererBoltPython.py";
        String pathToENV = directory;

        pathToENV = pathToENV+ "\\venv\\LIB\\site-packages";

        ClustererBolt clustererBolt = new ClustererBolt(3, false, pathToClass);
        Map env = new HashMap();
        env.put("PYTHONPATH", pathToENV);
        clustererBolt.setEnv(env);

        builder.setBolt("clusterer_bolt", clustererBolt, 1)
                .allGrouping("precomputer_bolt", "store_tuple" )
                .allGrouping("feedback_joiner_bolt", "cluster_overlap");

        AssignerBolt assignerBolt = new AssignerBolt(10000, true, 2);
        builder.setBolt("assigner_bolt", assignerBolt, 2)
                .shuffleGrouping("reader_bolt", "store_tuple")
                .allGrouping("reader_bolt", "finished")
                .allGrouping("clusterer_bolt", "partitions_created")
                .allGrouping("assigner_bolt", "recalculate_partitions");

        FeedbackJoiner feedbackJoinerBolt = new FeedbackJoiner(2, true);
        builder.setBolt("feedback_joiner_bolt", feedbackJoinerBolt, 3)
                .directGrouping("assigner_bolt")
                .allGrouping("assigner_bolt", "all_docs_sent");

        Config conf = new Config();
        conf.setDebug(false);
        conf.put(Config.TOPOLOGY_SUBPROCESS_TIMEOUT_SECS, 1200);

        //on cluster
        conf.setNumWorkers(2);

        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology("test", conf, builder.createTopology());
    }
}
