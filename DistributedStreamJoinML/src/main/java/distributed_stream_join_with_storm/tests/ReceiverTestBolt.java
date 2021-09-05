package distributed_stream_join_with_storm.tests;

import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;

public class ReceiverTestBolt extends BaseBasicBolt {
    @Override
    public void execute(Tuple tuple, BasicOutputCollector basicOutputCollector) {
        if(tuple.getSourceStreamId().equals("kv-pairs")){
            System.out.println(" received a tuple " +tuple.getValue(0) );
            System.out.println(" with id " +tuple.getValue(1) );
        }
        else if (tuple.getSourceStreamId().equals("finished")){
            System.out.println(" received a the finished message" );
        }
        else if (tuple.getSourceStreamId().equals("fused_maps")){
            System.out.println(" maps received");
            System.out.println(tuple.getValue(0).getClass() );
            //System.out.println(" occurrences: " +tuple.getValue(0) );
            //System.out.println(" co-occurrences: " +tuple.getValue(1) );
        }
        else if (tuple.getSourceStreamId().equals("clustering")){
            System.out.println(" clustering received");
            System.out.println(" labelsList");
            System.out.println(tuple.getValue(0).getClass() );
            System.out.println(tuple.getValue(0));
            System.out.println(tuple.getValue(0).toString().length());
            System.out.println("av_to_clusters");
            System.out.println(tuple.getValue(1));
            System.out.println(tuple.getValue(1).getClass() );
            //JSONArray jsonArray = (JSONArray) jsonObject.get("contact");

            //System.out.println(" occurrences: " +tuple.getValue(0) );
            //System.out.println(" co-occurrences: " +tuple.getValue(1) );
        }



    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        //outputFieldsDeclarer.declareStream();

    }
}
