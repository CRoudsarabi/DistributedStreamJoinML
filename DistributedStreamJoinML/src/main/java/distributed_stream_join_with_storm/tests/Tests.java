package distributed_stream_join_with_storm.tests;

import com.opencsv.CSVReader;
import distributed_stream_join_with_storm.bolts.FeedbackJoiner;
import distributed_stream_join_with_storm.helpers.Document;
import distributed_stream_join_with_storm.helpers.MapTuple;
import distributed_stream_join_with_storm.helpers.SamplingHelper;
import distributed_stream_join_with_storm.bolts.AssignerBolt;
import org.json.simple.JSONArray;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;


public class Tests {


    public static void main(String[] args){

        //testKVcalc();
        //testSampling();
        //testFile();
        //mapTest();
        //testCSV();
        //testJoin();
        //testSampling2();
        //testEncoding();
        testInvertedList();
    }


    public static void testFile()  {

        JSONParser jsonParser = new JSONParser();
        System.out.println(System.getProperty("user.dir"));
        try{
            JSONArray jsonArray = (JSONArray) jsonParser.parse(new FileReader("../Code/nobench/nobench_data.json"));
            Iterator iterator = jsonArray.iterator();
            if (iterator.hasNext()) {
                JSONObject document = (JSONObject) iterator.next();
                System.out.println(document);
            }

        }catch (Exception e){
            System.out.println(e.getMessage());
        }

    }

    public static void testJoin() {
        Set<Long> partOfClusters = new HashSet<>();
        partOfClusters.add(1L);
        partOfClusters.add(2L);
        partOfClusters.add(3L);
        Map<String, String> keyValuePairs = new HashMap<>();
        keyValuePairs.put("a","2");
        keyValuePairs.put("b","3");
        keyValuePairs.put("c","1");
        Document d1 = new Document(partOfClusters,keyValuePairs);

        Set<Long> partOfClusters2 = new HashSet<>();
        partOfClusters2.add(4L);
        partOfClusters2.add(5L);
        partOfClusters2.add(3L);
        Map<String, String> keyValuePairs2 = new HashMap<>();
        keyValuePairs2.put("a","2");
        keyValuePairs2.put("b","3");
        keyValuePairs2.put("c","3");
        Document d2 = new Document(partOfClusters2,keyValuePairs2);

        int matches = Document.join(d1,d2);
        System.out.println(matches);
    }

    public static void testCSV() {
        String path = "../Code/beg/BEG_2000000.csv";

        try (CSVReader reader = new CSVReader(new FileReader(path))) {
            Iterator iterator = reader.iterator();
            String [] attributes= (String[]) iterator.next();
            for (int i=0; i <= 10; i++){
                String [] values = (String[]) iterator.next();
                JSONObject jo = new JSONObject();
                for (int j=0; j< values.length; j++) {
                    if (!values[j].equals("-")) {
                        jo.put(attributes[j],values[j]);
                    }
                }
                System.out.println(jo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testKVcalc(){
        ArrayList<String> keyValuePairs= sampleDocument();
        int numberOfPairs = keyValuePairs.size();
        MapTuple mt = MapTuple.calcCountAndCoOccurances(keyValuePairs);
        System.out.println(mt.getKeyValuesCoOccurences());
        System.out.println(mt.getKeyValuesCount());
        //These next two values need to be equal
        System.out.println(mt.getKeyValuesCoOccurences().size());
        System.out.println(numberOfPairs*(numberOfPairs-1)/2);




    }

    public static void testSampling(){

        HashMap<String, Integer> occuranceCount = new HashMap<>();


        for (int i= 0; i< 1000; i++){
            ArrayList<String> keyValuePairs= generateSampleDocument();
            SamplingHelper.addToMap(occuranceCount,keyValuePairs);
        }

        System.out.println(occuranceCount);
        System.out.println(occuranceCount.size());
        System.out.println(occuranceCount.get("a:0"));
        System.out.println(occuranceCount.get("a:1"));


        ArrayList<String> mergedPairs = SamplingHelper.mergePairs(occuranceCount,1000,2);
        System.out.println(mergedPairs);

    }

    public static void testSampling2(){

        HashMap<String, Integer> occuranceCount = new HashMap<>();


        for (int i= 0; i< 1000; i++){
            ArrayList<String> keyValuePairs= generateSampleDocument();
            SamplingHelper.addToMap(occuranceCount,keyValuePairs);
        }

        System.out.println(occuranceCount);
        System.out.println(occuranceCount.size());
        System.out.println(occuranceCount.get("a:0"));
        System.out.println(occuranceCount.get("a:1"));


        ArrayList<String> mergedPairs = SamplingHelper.mergePairs(occuranceCount,1000,2);
        System.out.print("mergedPairs: ");
        System.out.println(mergedPairs);
        System.out.println("Sample Doc 1: ");
        System.out.println(SamplingHelper.calculateFinalPairs(generateSampleDocument(), mergedPairs));
        System.out.println("Sample Doc 2: ");
        System.out.println(SamplingHelper.calculateFinalPairs(generateSampleDocument(), mergedPairs));

    }

    public static void testEncoding(){
        /*
        HashMap<Integer, Integer> clusterSizes = new HashMap<>();
        clusterSizes.put(0,320);
        clusterSizes.put(1,450);
        clusterSizes.put(2,510);
        clusterSizes.put(3,870);
        clusterSizes.put(4,670);

        String encodedString = FeedbackJoiner.encodeSizesMapToString(clusterSizes);
        System.out.println(encodedString);

         */


        HashMap<Set<Long>, Long> clusterOverlap = new HashMap<>();
        Arrays.asList("1","2");
        clusterOverlap.put(new HashSet<>(Arrays.asList(1L,2L)) ,320L);
        clusterOverlap.put(new HashSet<>(Arrays.asList(1L,3L)),450L);
        clusterOverlap.put(new HashSet<>(Arrays.asList(1L,4L)),510L);
        clusterOverlap.put(new HashSet<>(Arrays.asList(2L,3L)),870L);
        clusterOverlap.put(new HashSet<>(Arrays.asList(2L,4L)),670L);

        String encodedString2 = FeedbackJoiner.encodeOverlapMapToString(clusterOverlap);
        System.out.println(encodedString2);

    }

    public static void mapTest(){

        ArrayList<String> keyValuePairs = new ArrayList<String>();
        keyValuePairs.add("lorem");
        keyValuePairs.add("ipsum");
        keyValuePairs.add("dolor");
        keyValuePairs.add("sit");
        keyValuePairs.add("amet");

        HashMap<String, Integer> partitionedKeys = new HashMap<>();
        partitionedKeys.put("lorem", 3);
        partitionedKeys.put("ipsum", 2);
        partitionedKeys.put("dolor", 5);
        partitionedKeys.put("amet", 1);

        for (String pair: keyValuePairs) {
            Integer id = partitionedKeys.get(pair);
            System.out.println(id);
            System.out.println(id==null);
        }

    }

    public static void testInvertedList(){
        HashMap<String, List<Long>> list = new HashMap<>();

        list.put("0", Arrays.asList(1L, 2L, 3L, 4L));
        list.put("1", Arrays.asList(5L, 6L, 7L, 8L));

        HashMap<Long, Long> invertedList = AssignerBolt.invertList(list);
        System.out.println(invertedList);
    }


    //not a test but a helper function
    private static ArrayList<String> sampleDocument(){
        ArrayList<String> keyValuePairs= new ArrayList<String>();

        keyValuePairs.add("a:1");
        keyValuePairs.add("b:1");
        keyValuePairs.add("d:2");
        keyValuePairs.add("c:3");
        keyValuePairs.add("e:4");
        keyValuePairs.add("f:6");
        keyValuePairs.add("g:9");
        keyValuePairs.add("p:7");
        keyValuePairs.add("l:4");
        keyValuePairs.add("k:2");
        keyValuePairs.add("z:7");
        keyValuePairs.add("x:4");
        keyValuePairs.add("y:2");
        return keyValuePairs;
    }



    //not a test but a helper function
    private static ArrayList<String> generateSampleDocument(){
        ArrayList<String> keyValuePairs= new ArrayList<String>();
        Random r = new Random();
        char c = 'a';
        keyValuePairs.add("a:" + (r.nextInt(2)) );

        for(int i =0 ; i < 10; i++){
            int d =r.nextInt(2)+1;
            //System.out.println(d);
            c = (char)(d + c);

            keyValuePairs.add(c+":"+ (r.nextInt(10)));
        }
        System.out.println(keyValuePairs);

        return keyValuePairs;
    }



}
