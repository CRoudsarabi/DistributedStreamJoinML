package distributed_stream_join_with_storm.tests;

import org.apache.storm.task.ShellBolt;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;

import java.util.Map;

public class PythonTestBolt extends ShellBolt implements IRichBolt {

    public PythonTestBolt(String path){

        super("python", path);
    }
    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declareStream("kv-pairs", new Fields("kv-pairs", "id"));
        outputFieldsDeclarer.declareStream("fused_maps", new Fields("occurrences", "co-occurrences"));
        outputFieldsDeclarer.declareStream("clustering", new Fields("labelsList", "av_to_cluster"));
        outputFieldsDeclarer.declareStream("finished", new Fields("finished"));
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        return null;
    }
}
