package distributed_stream_join_with_storm.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Document {

    private int docID;
    private Set<Long> partOfClusters;
    private Map<String, String> keyValuePairs;

    public Document(Set<Long> partOfClusters,Map<String, String> keyValuePairs){
        this.keyValuePairs = keyValuePairs;
        this.partOfClusters = partOfClusters;
    }

    public  Set<Long> getPartOfClusters(){
        return partOfClusters;
    }

    public static Map<String, String> createFromList(ArrayList<String> inputList){
        Map<String, String> output = new HashMap<>();
        for (String s: inputList){
            String[] pair= s.split("&");
            output.put(pair[0],pair[1]);
        }
        return output;
    }

    public static int join(Document doc1, Document doc2){
        int matches = 0;
        for (Map.Entry<String, String> entry: doc1.keyValuePairs.entrySet()) {
            for (Map.Entry<String, String> entry2: doc2.keyValuePairs.entrySet()) {
                if(entry.getKey() == entry2.getKey() && entry.getValue() == entry2.getValue()){
                    matches++;
                }
            }

        }
        return matches;
    }

}
