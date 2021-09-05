import storm
import StormPartitioning as par
import StormClustering as clu
import UntilityFunctions as uf


class ClustererBoltPython(storm.BasicBolt):
    outputs = ['partitions']

    def initialize(self, conf, context):
        self.counter = 0
        self.feedbackCounter = 0
        self.attributeValuePairs = {}
        self.attributeValuePairsCoOccurrence = {}
        self.feedbackSizes = {}
        self.feedbackOverlap = {}
        self.parallelism = 3
        self.maxLoad = 10000
        self.episodes = 300
        self.samplesize = 500
        self.clusteringType = 'aggl'
        self.withFeedback = True
        self._conf = conf
        self._context = context
        self.agent = None
        #self.agent = par.trainAgent(self.parallelism, self.maxLoad,)
        self.clustering = None
        self.av_to_cluster = {}
        self.caluculating = False

    def process(self, tuple):

        # length = 3 means the tuple comes from the preprocessor
        if len(tuple.values) == 3:
            kv_count = tuple.values[0]
            kv_CoOccurences = tuple.values[1]
            id = tuple.values[2]

            if (self.counter == self.samplesize and (not self.caluculating)):
                self.calculating = True
                self.clustering = clu.cluster(self.attributeValuePairs, self.attributeValuePairsCoOccurrence, self.clusteringType, 6)
                labelsList = self.clustering.labels_.tolist()
                self.av_to_cluster = dict(zip(self.attributeValuePairs.keys(), labelsList))
                cluster_sizes = par.estimateSizes(self.clustering)
                self.agent = par.trainAgent(self.parallelism, self.maxLoad, self.episodes, self.clustering, cluster_sizes, None)
                partitions = self.agent.createPartitions(self.clustering, self.parallelism, self.maxLoad, cluster_sizes)
                partitionedClusters = uf.decode(partitions.stateForOverlap)
                storm.emit([self.av_to_cluster, partitionedClusters], 'partitions_created')
            else:
                self.addToMap(kv_count, kv_CoOccurences)
                self.counter += 1

        # length = 2 means the tuple comes from the joiner and is feedback
        elif len(tuple.values) == 2:
            cluster_sizes = tuple.values[0]
            cluster_weights = eval(tuple.values[1])

            if self.withFeedback:
                self.combineFeedback(cluster_sizes, cluster_weights)
                self.feedbackCounter += 1
            if self.counter >= self.parallelism:
                self.agent = par.trainAgent(self.parallelism, self.maxLoad, self.clustering, self.feedbackSizes, self.feedbackOverlap)
                partitions = self.agent.createPartitions(self.clustering, self.parallelism, self.maxLoad, self.feedbackSizes, self.feedbackOverlap)
                self.feedbackCounter = 0
                partitionedClusters = uf.decode(partitions.stateForOverlap)
                storm.emit([self.av_to_cluster, partitionedClusters], 'partitions_created')
        #length = 1 means the tuple comes from the assigner and is the recalculate partitions message
        elif len(tuple.values) == 1:
            #self.calculating = False
            self.counter = 0
            self.attributeValuePairs = []
            self.attributeValuePairsCoOccurrence = []


    def addToMap(self, kv_count, kv_CoOccurences):
        for key in kv_count:
            if key in self.attributeValuePairs:
                self.attributeValuePairs[key] += kv_count[key]
            else:
                self.attributeValuePairs[key] = 1
        for key in kv_CoOccurences:
            pair = frozenset(key.split('$'))
            if pair in self.attributeValuePairsCoOccurrence:
                self.attributeValuePairsCoOccurrence[pair] += kv_CoOccurences[key]
            else:
                self.attributeValuePairsCoOccurrence[pair] = 1

    def combineFeedback(self, cluster_sizes, cluster_weights):
        for key in cluster_sizes:
            if key in self.feedbackSizes:
                self.feedbackSizes[key] += cluster_sizes[key]
            else:
                self.feedbackSizes[key] = cluster_sizes[key]
        for key in cluster_weights:
            pair = frozenset(key.split('-'))
            if pair in self.feedbackOverlap:
                self.feedbackOverlap[pair] += cluster_weights[key]
            else:
                self.feedbackOverlap[pair] = cluster_weights[key]


ClustererBoltPython().run()