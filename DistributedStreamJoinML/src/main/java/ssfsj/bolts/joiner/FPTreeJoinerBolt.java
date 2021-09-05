package ssfsj.bolts.joiner;

import org.apache.log4j.Logger;
import org.apache.storm.metric.api.CountMetric;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import ssfsj.fpTree.FpTreeInstantiator;
import ssfsj.utils.CommunicationMessages;
import ssfsj.utils.JoinResultsCsvOutput;
import ssfsj.utils.KeyValuePair;

import java.util.*;

public class FPTreeJoinerBolt extends BaseBasicBolt {
    /**
     * logger for the FPTreeJoinerBolt
     */
    private static final Logger LOG = Logger.getLogger(FPTreeJoinerBolt.class);
    /**
     * ID of the instance of the bolt
     */
    private int boltId;
    /**
     * map for storing the documents and their set of key-value pairs
     */
    HashMap<String, HashSet<KeyValuePair>> elementsPerDocument;
    /**
     * Metric for the number documents received.
     */
    CountMetric numberOfDocumentsReceivedForJoin;
    /**
     * object of the FpTreeInstantiation class through which the FPTree can be generated and joinable documents can be found
     */
    FpTreeInstantiator fpTreeInstantiator;
    /**
     * id of the next csv file that needs to be generated by an instance of the FPTreeJoinerBolt
     */
    int nextCsvFileId;
    /**
     * indicating the number of AssignerBolt instances
     */
    int numberOfAssigners;
    /**
     * the number of documents used from every window
     */
    private int totalNumberOfDocsForEveryWindow;
    /**
     * number of instances for the AssignerBolt
     */
    private int numberOfFinishedMsgsAssignerBolt;

    public FPTreeJoinerBolt(int numberOfAssigners){
        this.fpTreeInstantiator = new FpTreeInstantiator();
        this.elementsPerDocument = new HashMap<>();
        this.nextCsvFileId = 1;
        this.numberOfAssigners = numberOfAssigners;
        this.totalNumberOfDocsForEveryWindow = 0;
        this.numberOfFinishedMsgsAssignerBolt = 0;
    }

    /**
     * this method is called before the topology is finished
     */
    @Override
    public void cleanup() {

        LOG.info("FPTreeJoinerBolt " + this.boltId + " covered " + this.elementsPerDocument.size() + " documents");
        LOG.info("FPTreeJoinerBolt " + this.boltId + " received " + this.numberOfDocumentsReceivedForJoin.getValueAndReset() + " documents.");
        LOG.info("FPTreeJoinerBolt " + this.boltId + " performed join for " + this.totalNumberOfDocsForEveryWindow +" documents.");

        /**
         * PRINT ALL THE JOINABLE DOCUMENTS FOUND BY EVERY INSTANCE OF THE JOINER BOLT
         */
//        System.out.println("FPTreeJoinerBolt " + this.boltId + " joinable documents:");
//        //create the tree of the joinable documents
//        this.createFpTree();
//        //find all joinable documents
//        Map<String,Set<String>> joinableDocs = this.findJoinableDocuments();
//        for(String docId : joinableDocs.keySet()){
//            System.out.println("FPTreeJoinerBolt " + this.boltId + " - doc " + docId +" -> " + joinableDocs.get(docId).toString());
//        }
        //output the joinable results to csv file
//        this.outputJoinableResultsToCsvFile();//ONLY FOR TESTING PURPOSE COMMENT WHEN USING ON SERVER
        /**
         * END PRINT
         */
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context) {
        this.boltId = context.getThisTaskId();
        this.numberOfDocumentsReceivedForJoin = new CountMetric();
    }

    @Override
    public void execute(Tuple input, BasicOutputCollector collector) {
        /**
         * the same can be accomplished by receiving a tick tuple at every x seconds and performing the join
         * for the current documents. Current decision is to perform the join for every window and for
         * that reason the number of all_local_docs_sent message is calculated and when all AssignerBolt instances
         * send it, the join is performed.
         */
        if(input.getSourceStreamId().equals(CommunicationMessages.assigner_all_local_docs_sent)){
            /**
             * increment the number of msgs sent from the AssignerBolt for finishing the current window
             */
            this.numberOfFinishedMsgsAssignerBolt += 1;
        } else {
            /**
             * get the information sent by the AssignerBolt
             */
            List<String> kvPairsForDocument = (List<String>) input.getValue(0);
            String documentName = input.getStringByField("document-name");
            //increment the number of tuples(messages) received
            this.numberOfDocumentsReceivedForJoin.incr();
            /**
             * store the document
             */
            HashSet<KeyValuePair> allKvPairsForDoc = new HashSet<>();
            for (String kvPair : kvPairsForDocument) {
                //it means that there is a joined key
                if(kvPair.contains("|")){
                    String[] splitKvPairs = kvPair.split("\\|");
                    ArrayList<String> allKeysAndValues = new ArrayList<>();
                    for(String element : splitKvPairs){
                        if(element.contains("-")){
                            String[] splitElement = element.split("-");
                            allKeysAndValues.add(splitElement[0].trim());
                            allKeysAndValues.add(splitElement[1].trim());
                        }else{
                            allKeysAndValues.add(element);
                        }
                    }
                    /**
                     * create objects by combining the correct keys and values
                     */
                    for(int i=0;i<(allKeysAndValues.size()/2);i++){
                        KeyValuePair kvPairObject = new KeyValuePair(allKeysAndValues.get(i),allKeysAndValues.get(i+allKeysAndValues.size()/2));
                        allKvPairsForDoc.add(kvPairObject);
                    }

                }else {//it is just a regular key-value pair (not consolidated)
                    String[] splitKvPair = kvPair.split("-");
                    KeyValuePair kvPairObj = new KeyValuePair(splitKvPair[0], splitKvPair[1]);
                    allKvPairsForDoc.add(kvPairObj);
                }
            }

            //update the current documents
            this.elementsPerDocument.put(documentName, allKvPairsForDoc);
        }

        /**
         * if all AssignerBolts have finished the window find the joinable documents
         */
        if(this.numberOfFinishedMsgsAssignerBolt==this.numberOfAssigners){
            this.createTreeAndComputeJoin();

            //reset the number of msgs received from the AssignerBolt
            this.numberOfFinishedMsgsAssignerBolt = 0;
            //reset the documents used for the join
            this.elementsPerDocument = new HashMap<>();

            collector.emit(CommunicationMessages.joiner_join_performed,new Values(CommunicationMessages.joiner_join_performed));
        }
    }

    /**
     * Method for creating the FPTree
     */
    private void createFpTree(){
        long timeForCreatingTree = System.currentTimeMillis();
        this.fpTreeInstantiator.createFpTree(this.elementsPerDocument);
        timeForCreatingTree = System.currentTimeMillis()-timeForCreatingTree;

        LOG.info("FPTreeJoinerBolt " + this.boltId + " time for creating tree " + timeForCreatingTree + " MS.");
        LOG.info("FPTreeJoinerBolt " + this.boltId + " found " + this.fpTreeInstantiator.getNumOfKeysInEveryDoc() + " keys that appear in all docs!!!");
    }

    /**
     * Method for finding the joinable documents from the partitions of
     * documents that every instance of FPTreeJoinerBolt is responsible for.
     * Instead of having a serial execution this method can parallelized by having
     * multiple threads that work on separate parts of the tree.
     * @return
     */
    private Map<String,Set<String>> findJoinableDocuments(){
        return this.fpTreeInstantiator.findJoinableDocumentsThroughThreads();
    }

    /**
     * Method for storing the joinable documents in a CSV file.
     */
    private void createTreeAndComputeJoin(){
        LOG.info("FPTreeJoinerBolt " + this.boltId +" started joining with " + this.elementsPerDocument.keySet().size());
        this.totalNumberOfDocsForEveryWindow+=this.elementsPerDocument.size();

        //create the FPTree
        this.createFpTree();

        long timeForFindingJoinable = System.currentTimeMillis();
        //find all of the joinable documents through threads
        Map<String,Set<String>> joinableDocs = this.findJoinableDocuments();
        timeForFindingJoinable = System.currentTimeMillis() - timeForFindingJoinable;
        LOG.info("FPTreeJoinerBolt " + this.boltId + " time for finding joinable docs " + timeForFindingJoinable + " MS.");

        /**
         * uncomment for outputting the joinable results to a csv file
         */
//        JoinResultsCsvOutput csvOutput = new JoinResultsCsvOutput(this.boltId);
//        //used in order to be able to store the historical information about the joins performed by the bolt
//        this.nextCsvFileId += 1;
//        csvOutput.createCsvFile(joinableDocs);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declareStream(CommunicationMessages.joiner_join_performed, new Fields("msg"));
    }
}
