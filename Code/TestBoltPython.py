import storm


class TestBoltPython(storm.BasicBolt):
    outputs = ['kv-pairs','id']

    def initialize(self, conf, context):
        self._conf = conf;
        self._context = context;

    def process(self, tuple):
        listOfKVpairs = tuple.values[0]


        if len(tuple.values) > 1 :
            id = tuple.values[1]
            #if clusterer is not None:
                #id = 'success'
            #else:
                #id = 'error'
            #id = np.zeros((3, 4)).tolist()

            storm.emit([listOfKVpairs, id], 'kv-pairs')
        else:
            storm.emit([listOfKVpairs], 'finished')





TestBoltPython().run()