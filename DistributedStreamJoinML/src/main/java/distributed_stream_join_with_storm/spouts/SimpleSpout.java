package distributed_stream_join_with_storm.spouts;

import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class SimpleSpout extends BaseRichSpout {
    SpoutOutputCollector _collector;
    Iterator iterator;

    int numberOfDocumentsToEmit;
    int numberOfDocumentsEmitted;

    //boolean smthWentWrong;
    boolean finished_sent;

    public SimpleSpout(int numberOfDocumentsToEmit){
        this.numberOfDocumentsToEmit = numberOfDocumentsToEmit;
    }

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
        this._collector = spoutOutputCollector;

        this.numberOfDocumentsEmitted = 0;
        //this.smthWentWrong =false;
        finished_sent = false;


        JSONParser jsonParser = new JSONParser();

        JSONArray jsonArray;
        String path ="../Code/nobench/nobench_data.json";
        try {
            jsonArray = (JSONArray) jsonParser.parse(new FileReader(path));

            this.iterator = jsonArray.iterator();
        } catch (IOException | ParseException e) {
            //this.smthWentWrong = true;
            e.printStackTrace();
        }

        try {
            long time = 1*1000;
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void nextTuple() {
        /*System.out.println("document emmited: "+numberOfDocumentsEmitted);
        if (smthWentWrong){
            System.out.println("Something went wrong when opening the file");
        }*/

        if(numberOfDocumentsEmitted >= numberOfDocumentsToEmit){
            if (!finished_sent){
                this._collector.emit("finished", new Values("finished"));
                finished_sent = true;
            }

            return;
        }
        if(iterator.hasNext()){
            if (numberOfDocumentsEmitted % 10_000 == 0 && numberOfDocumentsEmitted != 0){
                try {
                    long time = 1*60*1000;
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            JSONObject document = (JSONObject) iterator.next();

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
