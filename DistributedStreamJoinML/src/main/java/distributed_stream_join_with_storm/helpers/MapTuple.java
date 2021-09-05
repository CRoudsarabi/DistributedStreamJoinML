package distributed_stream_join_with_storm.helpers;

import java.util.ArrayList;
import java.util.HashMap;


public class MapTuple {

    private HashMap<String, Integer> keyValuesCount;
    private HashMap<String, Integer> keyValuesCoOccurences;

    public MapTuple(HashMap<String, Integer> keyValuesCount, HashMap<String, Integer> keyValuesCoOccurences){
        this.keyValuesCount = keyValuesCount;
        this.keyValuesCoOccurences = keyValuesCoOccurences;
    }
    public HashMap<String, Integer> getKeyValuesCoOccurences() {
        return keyValuesCoOccurences;
    }

    public HashMap<String, Integer> getKeyValuesCount() {
        return keyValuesCount;
    }

    public static MapTuple calcCountAndCoOccurances(ArrayList<String> keyValuePairs){

        HashMap<String, Integer> keyValuesCount = new HashMap();
        HashMap<String, Integer> keyValuesCoOccurences = new HashMap();
        ArrayList<String> copiedList = new ArrayList<>(keyValuePairs);

        for (String pair : keyValuePairs) {
            if(keyValuesCount.containsKey(pair)){
                Integer oldValue= keyValuesCount.get(pair);
                keyValuesCount.put(pair, oldValue+1);
            }
            else {
                keyValuesCount.put(pair,1);
            }

            for (String pair2: copiedList) {
                if (!pair.equals(pair2)){
                    String pair3 = (pair +"$"+pair2);
                    if(keyValuesCoOccurences.containsKey(pair3)){
                        Integer oldValue= keyValuesCoOccurences.get(pair3);
                        keyValuesCoOccurences.put(pair3, oldValue+1);
                    }
                    else {
                        keyValuesCoOccurences.put(pair3,1);
                    }
                }
            }
            copiedList.remove(pair);

        }
        return new MapTuple(keyValuesCount,keyValuesCoOccurences);
    }

}
