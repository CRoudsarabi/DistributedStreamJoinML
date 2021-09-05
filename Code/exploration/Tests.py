import numpy as np
import matplotlib.pyplot as plt
import json
import pickle
import PartitioningQLearning as pql
import PartinioningDeepQLearning as pdql
import PartinioningDQLOptimized as pdqlo
import JSONDataTest as jdt
import ProcessBenchmark as pb
import SimpleClustering as clust
import HdbscanClustering as hclust


def testLoadBalance():
    print(pql.calculateLoadBalance(np.array([1, 2, 3])))
    print(pql.calculateLoadBalance(np.array([7, 7, 4])))
    print(pql.calculateLoadBalance(np.array([7, 6, 5])))

def testRandomArray():
    partitions =np.random.randint(low=1, high=5, size=(6))
    print(partitions)

def testSimpleBenchmark():
    print(pql.greedyAssignment(np.array([3, 3, 2, 2, 4, 4]), 3))

def testMain():
    print(pql.main())

def testDeepQLMain():
    print(pdql.main())

def testDeepQLOMain():
    print(pdqlo.main())

def testRandom():
    for i in range(100):
        print(np.random.random())

def testJSON():
    jdt.main()

def testProcessBenchmark():
    attributeValuePairs, attributeValuePairsCoOccurrence = pb.processBenchmark()
    for entry in attributeValuePairsCoOccurrence:
        if attributeValuePairsCoOccurrence[entry] > 10:
            print(set(entry).__str__() + " : " + attributeValuePairsCoOccurrence[entry].__str__())
    #print(attributeValuePairs)
    #print(len(attributeValuePairs))
    return (attributeValuePairs, attributeValuePairsCoOccurrence)

def testClustering(attributeValuePairs, attributeValuePairsCoOccurrence):
    clust.main(attributeValuePairs, attributeValuePairsCoOccurrence)

def testHdbscanClustering(attributeValuePairs, attributeValuePairsCoOccurrence):
    hclust.main(attributeValuePairs, attributeValuePairsCoOccurrence)

def testSaveToJson(dict, filename):
    jsonDict = json.dumps(dict)
    f = open(filename, "w")
    f.write(jsonDict)
    f.close()

def testSaveWithPickle(dict, filename):
    f = open(filename, "wb")
    pickle.dump(dict,f)
    f.close()

def testLoadWithPickle(filename):
    file = open(filename, "rb")
    loaded = pickle.load(file)
    file.close()
    return loaded





#attributeValuePairs, attributeValuePairsCoOccurrence = testProcessBenchmark()
#testSaveWithPickle(attributeValuePairs, "processed\\avpairs.pkl")
#testSaveWithPickle(attributeValuePairsCoOccurrence, "processed\\avpcoocurrence.pkl")

#attributeValuePairs = testLoadWithPickle("processed\\avpairs.pkl")
#print("loaded attributeValuePairs")
#attributeValuePairsCoOccurrence = testLoadWithPickle("processed\\avpcoocurrence.pkl")
#print("loaded attributeValuePairsCoOccurrence")
#testHdbscanClustering(attributeValuePairs, attributeValuePairsCoOccurrence)

#testClustering(attributeValuePairs, attributeValuePairsCoOccurrence)
#testClustering()
#testJSON()
#testDeepQLOMain()
#testDeepQLMain()
#testMain()
#testSimpleBenchmark()
#testLoadBalance()
#testRandomArray()
#testRandom()

