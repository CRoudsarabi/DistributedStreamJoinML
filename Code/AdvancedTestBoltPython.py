import storm
import StormPartitioning as par
import StormClustering as clu
import UntilityFunctions as uf


class AdvancedTestBoltPython(storm.BasicBolt):
    outputs = ['kv-pairs', 'id']

    def initialize(self, conf, context):
        self.counter = 0
        self._conf = conf
        self._context = context
        self.attributeValuePairs = {}
        self.attributeValuePairsCoOccurrence = {}
        self.agent = None
        self.clustering = None

    def process(self, tuple):

        kv_count = tuple.values[0]
        kv_CoOccurences = tuple.values[1]
        #kv_count = eval(tuple.values[0])
        #kv_CoOccurences = eval(tuple.values[1])



        self.addToMap(kv_count, kv_CoOccurences)
        self.counter += 1

        if self.counter >= 500:
            storm.emit([self.attributeValuePairs, uf.convertToStringMap(self.attributeValuePairsCoOccurrence)], 'fused_maps')
            self.clustering = clu.cluster(self.attributeValuePairs,  self.attributeValuePairsCoOccurrence, 'aggl')
            labelsList = self.clustering.labels_.tolist()
            av_to_cluster = dict(zip(self.attributeValuePairs.keys(), labelsList))
            #cluster_sizes = par.estimateSizes(self.clustering)
            self.counter = 0


            storm.emit([labelsList, av_to_cluster], 'clustering')



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





AdvancedTestBoltPython().run()


