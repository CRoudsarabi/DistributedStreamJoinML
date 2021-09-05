package distributed_stream_join_with_storm.spouts;

import com.opencsv.CSVReader;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;

import java.io.FileReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

public class CSVSpout extends BaseRichSpout {

    SpoutOutputCollector _collector;
    Iterator iterator;
    String[] attributes;

    int numberOfDocumentsToEmit;
    int numberOfDocumentsEmitted;

    boolean finished_sent;

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
        this._collector = spoutOutputCollector;
        this.numberOfDocumentsEmitted = 0;
        finished_sent = false;

        String path ="../Code/beg/BEG_2000000.csv";

        try (CSVReader reader = new CSVReader(new FileReader(path))) {
            iterator=reader.iterator();
            attributes = (String[]) iterator.next();
        }
        catch (Exception e){

        }
    }

    public CSVSpout(int numberOfDocumentsToEmit){
        this.numberOfDocumentsToEmit = numberOfDocumentsToEmit;
    }

    @Override
    public void nextTuple() {

        if(numberOfDocumentsEmitted >= numberOfDocumentsToEmit){
            if (!finished_sent){
                this._collector.emit("finished", new Values("finished"));
                finished_sent = true;
            }

            return;
        }
        if(iterator.hasNext()){
            String [] values = (String[]) iterator.next();
            JSONObject document = new JSONObject();
            for (int j=0; j< values.length; j++) {
                if (!values[j].equals("-")) {
                    document.put(attributes[j],values[j]);
                }
            }

            this._collector.emit("store_tuple", new Values(document, (numberOfDocumentsEmitted+1)));
            try {
                long time = 1;
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            numberOfDocumentsEmitted++;
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declareStream("store_tuple", new Fields("document","id"));
        outputFieldsDeclarer.declareStream("finished", new Fields("finished"));
    }
}
