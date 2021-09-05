import StormClustering as sclust
import StormPartitioning as spar
import UntilityFunctions as uf


def testCreatePartition():
    clusterSizes = {'a': 1, 'b': 2,'c': 4, 'd': 6, 'e': 9}

    partition, clusters = spar.createPartition(None, clusterSizes)
    print(partition)
    print(clusters)

def testCluster():
    occurrences = {'a': 3, 'b': 5, 'c': 7, 'd': 5, 'e': 4, 'f': 2, 'g': 1}
    coocurrences = {frozenset(['a', 'b']): 4, frozenset(['a', 'c']): 7, frozenset(['d', 'e']): 2,
                    frozenset(['d', 'f']): 3, frozenset(['d', 'g']): 5}
    clustering = sclust.cluster(occurrences, coocurrences, 'aggl', 2)

    print(clustering.labels_)

def testCluster2():
    occurrences = {}
    coocurrences = {}
    kv_count =  {'a': 3, 'b': 5, 'c': 7, 'd': 5, 'e': 4, 'f': 2, 'g': 1}
    kv_CoOccurences = {'a&c': 4, 'a&b': 7, 'd&e': 4, 'd&f': 7, 'd&g': 4, 'e&g': 7}

    addToMap(occurrences, coocurrences, kv_count, kv_CoOccurences)

    print(occurrences)
    print(coocurrences)
    clustering = sclust.cluster(occurrences, coocurrences, 'aggl', 2)

    print(clustering.labels_)

    print(convertToStringMap(coocurrences))

def testCluster3():
    occurrences = {}
    coocurrences = {}
    kv_count =  {'a': 3, 'b': 5, 'c': 7, 'd': 5, 'e': 4, 'f': 2, 'g': 1}
    kv_CoOccurences = {'a&c': 4, 'a&b': 7, 'd&e': 4, 'd&f': 7, 'd&g': 4, 'e&g': 7}

    addToMap(occurrences, coocurrences, kv_count, kv_CoOccurences)

    print(occurrences)

    clustering = sclust.cluster(occurrences, coocurrences, 'aggl', 2)

    print(clustering.labels_)

    labelsList = clustering.labels_.tolist()
    av_to_cluster = dict(zip(occurrences.keys(), labelsList))

    print(av_to_cluster)



def testClusterAndPartition():
    occurrences = {}
    coocurrences = {}
    kv_count =  {'a': 3, 'b': 5, 'c': 7, 'd': 5, 'e': 4, 'f': 2, 'g': 1}
    kv_CoOccurences = {'a&c': 4, 'a&b': 7, 'd&e': 4, 'd&f': 7, 'd&g': 4, 'e&g': 7}

    addToMap(occurrences, coocurrences, kv_count, kv_CoOccurences)

    print(occurrences)
    print(coocurrences)
    clustering = sclust.cluster(occurrences, coocurrences, 'aggl', 2)

    print(clustering.labels_)
    parallelism = 3
    maxLoad = 10000

    cluster_sizes = spar.estimateSizes(clustering)
    agent = spar.trainAgent(parallelism, maxLoad, clustering, cluster_sizes, None)
    partitions = agent.createPartitions(clustering, parallelism, maxLoad, cluster_sizes)

def testDecode():
    occurrences = {}
    coocurrences = {}
    kv_count =  {'a': 3, 'b': 5, 'c': 7, 'd': 5, 'e': 4, 'f': 2, 'g': 1}
    kv_CoOccurences = {'a&c': 4, 'a&b': 7, 'd&e': 4, 'd&f': 7, 'd&g': 4, 'e&g': 7}

    addToMap(occurrences, coocurrences, kv_count, kv_CoOccurences)


    clustering = sclust.cluster(occurrences, coocurrences, 'aggl', 2)


    parallelism = 3
    maxLoad = 10000

    cluster_sizes = spar.estimateSizes(clustering)
    agent = spar.trainAgent(parallelism, maxLoad, clustering, cluster_sizes, None)
    partitions = agent.createPartitions(clustering, parallelism, maxLoad, cluster_sizes)
    print(partitions.stateForOverlap)
    for key in partitions.stateForOverlap.keys():
        print(type(key))
    for value in partitions.stateForOverlap.values():
        for element in value:
            print(type(element))
    partitionedClusters = uf.decode(partitions.stateForOverlap)
    for key in partitionedClusters.keys():
        print(type(key))
    for value in partitionedClusters.values():
        for element in value:
            print(type(element))


def addToMap(occurrences, coocurrences, kv_count, kv_CoOccurences):
    for key in kv_count:
        if key in occurrences:
            occurrences[key] += kv_count[key]
        else:
            occurrences[key] = kv_count[key]
    for key in kv_CoOccurences:
        pair = frozenset(key.split('&'))
        if pair in coocurrences:
            coocurrences[pair] += kv_CoOccurences[key]
        else:
            coocurrences[pair] = kv_CoOccurences[key]

def convertToStringMap(attributeValuePairsCoOccurrence):
    cooccurences = {}
    for set in attributeValuePairsCoOccurrence:
        cooccurences['&'.join(list(set))] = attributeValuePairsCoOccurrence[set]
    return cooccurences

#testCreatePartition()

#testCluster()
#testCluster2()
#testClusterAndPartition()
#testCluster3()
testDecode()