package distributed_stream_join_with_storm.bolts;

import org.apache.storm.task.ShellBolt;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;


import java.util.Map;

public class ClustererBolt extends ShellBolt implements IRichBolt {


    public ClustererBolt(int numberOfJoinerBolts, boolean with_feedback, String path){
        super("python", path);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declareStream("partitions_created", new Fields("partitionedKeys", "partitionedClusters"));
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        return null;
    }
}
