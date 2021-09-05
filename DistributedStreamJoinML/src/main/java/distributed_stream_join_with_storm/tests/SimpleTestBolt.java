package distributed_stream_join_with_storm.tests;

import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Tuple;

public class SimpleTestBolt extends BaseBasicBolt {

    int test_counter;

    public SimpleTestBolt(){
        this.test_counter =0;
    }

    @Override
    public void execute(Tuple tuple, BasicOutputCollector basicOutputCollector) {
        if(tuple.getSourceStreamId().equals("store_tuple")){
            int document_id = (int) tuple.getValue(1);
            System.out.println(document_id);
            this.test_counter++;
        }
        if(tuple.getSourceStreamId().equals("finished")){

            System.out.println("processed: "+test_counter+" tuples");
        }

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {

    }
}
