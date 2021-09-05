package distributed_stream_join_with_storm.helpers;

import java.util.*;

public class SamplingHelper {



    public static ArrayList<String> calculateFinalPairs( ArrayList<String> currentKVpairs, ArrayList<String> mergedKVpairs){

        if (mergedKVpairs == null || mergedKVpairs.size()==0){
            return currentKVpairs;
        }

        ArrayList<String> resultKVpairs = new ArrayList<>();
        Map<String,String[]> mergedKVpairsMap =new HashMap<>();
        Map<String,String> currentKVpairsMap =new HashMap<>();

        for (String mergedpair : mergedKVpairs) {
            String[] tokens = mergedpair.split("-");
            String[] remainder = Arrays.copyOfRange(tokens, 1, tokens.length);
            mergedKVpairsMap.put(tokens[0],remainder);
        }

        for (String currentpair : currentKVpairs) {
            String[] tokens = currentpair.split("&");
            currentKVpairsMap.put(tokens[0],tokens[1]);
        }

        ArrayList<String> toBeRemovedAfterIteration = new ArrayList<>();
        for (Map.Entry<String, String> pair: currentKVpairsMap.entrySet()) {
            boolean foundInMergedList= false;
            String resultPairAttribute = pair.getKey();
            String resultPairValue = pair.getValue();
            for (Map.Entry<String, String[]> entry: mergedKVpairsMap.entrySet()) {

                if (pair.getKey().equals(entry.getKey())) {

                    foundInMergedList = true;
                    for (String s : entry.getValue()) {
                        if(currentKVpairsMap.containsKey(s)){
                            resultPairAttribute += "-"+s;
                            resultPairValue += "-"+currentKVpairsMap.get(s);
                            //this would optimize but cannot modify list/map we are currently interating through
                            //currentKVpairsMap.remove(s);
                            //currentKVpairs.remove(s+":"+currentKVpairsMap.get(s));
                            toBeRemovedAfterIteration.add(s+":"+currentKVpairsMap.get(s));
                        }
                        else{
                            resultPairAttribute += "-"+ s;
                            resultPairValue += "-null";
                        }
                    }
                }

            }
            if (foundInMergedList){
                resultKVpairs.add(resultPairAttribute+"&"+resultPairValue);
            }
            else{
                resultKVpairs.add(pair.getKey()+"&"+ pair.getValue());
            }

        }
        resultKVpairs.removeAll(toBeRemovedAfterIteration);
        return resultKVpairs;
    }

    public static HashMap<String, Integer>  addToMap( HashMap<String, Integer> occuranceCount, ArrayList<String> keyValuePairs){
        for (String kv_pair : keyValuePairs) {
            if(occuranceCount.containsKey(kv_pair)){
                Integer oldValue= occuranceCount.get(kv_pair);
                occuranceCount.put(kv_pair, oldValue+1);
            }
            else {
                occuranceCount.put(kv_pair,1);
            }
        }
        return occuranceCount;
    }

    public static ArrayList<String> mergePairs(HashMap<String, Integer> occuranceCount,int numberOfDocumentAlreadySampled, int numberOfJoinerBolts){
        ArrayList<String> mergedKVpairs = new ArrayList<>();
        int maximumPartitionSize = (numberOfDocumentAlreadySampled/(numberOfJoinerBolts));
        Set<String> commonAttributes = new HashSet<>();
        for (Map.Entry<String, Integer> entry: occuranceCount.entrySet()) {
            if(entry.getValue() > maximumPartitionSize){
                String[] tokens = entry.getKey().split("&");
                commonAttributes.add(tokens[0]);
            }
        }


        //edge case if the is only one common attribute. Need to have atleast 2
        while (commonAttributes.size() == 1){
            //System.out.println(commonAttributes.size() );
            Map.Entry<String, Integer> maxEntry = Collections.max(occuranceCount.entrySet(), (Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) -> e1.getValue()
                    .compareTo(e2.getValue()));
            String[] tokens = maxEntry.getKey().split("&");
            //System.out.println(tokens[0] );
            commonAttributes.add(tokens[0]);
            occuranceCount.remove(maxEntry.getKey());
            //maxEntry.getValue();
        }



        while(commonAttributes.size() > 0){
            String firstAttribute = (String) commonAttributes.toArray()[0];
            commonAttributes.remove(firstAttribute);
            String secondAttribute = (String) commonAttributes.toArray()[0];
            commonAttributes.remove(secondAttribute);
            String merged = firstAttribute+"-"+secondAttribute;

            //when the number of common attributes is uneven, we have to combine 3 attributes into one. Otherwise one would be left without a partner.
            if(commonAttributes.size() == 1){
                String thirdAttribute = (String) commonAttributes.toArray()[0];
                commonAttributes.remove(thirdAttribute);
                merged = merged+"-"+thirdAttribute;
            }

            mergedKVpairs.add(merged);

        }

        return mergedKVpairs;
    }






}
