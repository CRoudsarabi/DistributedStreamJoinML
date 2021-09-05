package distributed_stream_join_with_storm.bolts;

import distributed_stream_join_with_storm.helpers.SamplingHelper;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.util.ArrayList;
import java.util.HashMap;

public class SamplerBolt extends BaseBasicBolt {
    int numberOfDocumentToBeSampled;
    int numberOfDocumentAlreadySampled;
    ArrayList<String> mergedKVpairs;
    HashMap<String, Integer> occuranceCount;
    int numberOfJoinerBolts;
    boolean samplingFinished;

    public SamplerBolt(int numberOfDocumentToBeSampled, int numberOfJoinerBolts){
        this.numberOfDocumentToBeSampled = numberOfDocumentToBeSampled;
        numberOfDocumentAlreadySampled = 0;
        this.occuranceCount = new HashMap<>();
        this.numberOfJoinerBolts = numberOfJoinerBolts;
        this.samplingFinished = false;

    }
    @Override
    public void execute(Tuple tuple, BasicOutputCollector basicOutputCollector) {
        if(tuple.getSourceStreamId().equals("sample_tuple")) {
            ArrayList<String> keyValuePairs = (ArrayList<String>) tuple.getValue(0);
            this.occuranceCount = SamplingHelper.addToMap(occuranceCount, keyValuePairs);
            numberOfDocumentAlreadySampled++;

            if (numberOfDocumentAlreadySampled > numberOfDocumentToBeSampled && !samplingFinished) {
                this.mergedKVpairs = SamplingHelper.mergePairs(occuranceCount, numberOfDocumentAlreadySampled, numberOfJoinerBolts);
                basicOutputCollector.emit("merged_kv_pairs", new Values(this.mergedKVpairs));
                samplingFinished = true;
            }
        }
        else if(tuple.getSourceStreamId().equals("resample")) {
            numberOfDocumentAlreadySampled = 0;
            this.occuranceCount = new HashMap<>();
            this.samplingFinished = false;
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {

        outputFieldsDeclarer.declareStream("merged_kv_pairs", new Fields("finished"));

    }
}
