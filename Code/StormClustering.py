from tqdm import tqdm # Progress meter
# import pandas as pd
import matplotlib.pyplot as plt
import hdbscan
import pickle
from sklearn.cluster import AgglomerativeClustering
from sklearn.cluster import AffinityPropagation
# from sklearn.cluster import DBSCAN
import numpy as np



def cluster(attributeValuePairs, attributeValuePairsCoOccurrence, type, numberOfClusters = 20):
    n =len(attributeValuePairs)
    # print(f'length of attributeValuePairs: {n}')
    # print(f'length of attributeValuePairsCoOccurrence: {len(attributeValuePairsCoOccurrence)}')
    # print(attributeValuePairsCoOccurrence)
    pruning = False
    verbose = False
    loadDistance = False

    clustering = None
    if type == 'aggl':
        clustering = agglClustering(attributeValuePairs, attributeValuePairsCoOccurrence, loadDistance, pruning,
                                       verbose, numberOfClusters)
    elif type == 'hdbscan':
        clustering = hdbscanClustering(attributeValuePairs, attributeValuePairsCoOccurrence, loadDistance, pruning,
                                       verbose)

    return clustering


def hdbscanClustering(attributeValuePairs, attributeValuePairsCoOccurrence, loadDistance, pruning, verbose):
    if loadDistance:
        distanceMatrix = loadDistanceMatrix("processed\\dMatrix.pkl")
    else:
        distanceMatrix = recalculateDistance(attributeValuePairs, attributeValuePairsCoOccurrence)
        # print(distanceMatrix)
        # print("------------")
        tril = np.tril_indices_from(distanceMatrix, -1)  # take lower & upper triangle's indices
        triu = np.triu_indices_from(distanceMatrix, 1)  # (without diagonal)

        distanceMatrix[tril] = distanceMatrix[triu]

        #saveDistanceMatrix(distanceMatrix, "processed\\dMatrix.pkl")

    if(verbose):
        print(distanceMatrix)
        print("starting clustering")
    # prediction_data=True, for soft clustering, doesnt work with precomputed metric
    # min_cluster_size = 100, min_samples = 80,
    clusterer = hdbscan.HDBSCAN(min_samples=40,  metric='precomputed')
    clusterer.fit(distanceMatrix)

    if(verbose):
        print("finished clustering")
    # if pruning:
        # distanceMatrix = pruneMatrix(distanceMatrix, 10)

    return clusterer

def agglClustering(attributeValuePairs, attributeValuePairsCoOccurrence, loadDistance, pruning, verbose, numberOfClusters):
    if loadDistance:
        distanceMatrix = loadDistanceMatrix("processed\\dMatrix.pkl")
    else:
        distanceMatrix = recalculateDistance(attributeValuePairs, attributeValuePairsCoOccurrence)
        # print(distanceMatrix)
        # print("------------")
        tril = np.tril_indices_from(distanceMatrix, -1)  # take lower & upper triangle's indices
        triu = np.triu_indices_from(distanceMatrix, 1)  # (without diagonal)

        distanceMatrix[tril] = distanceMatrix[triu]

        #saveDistanceMatrix(distanceMatrix, "processed\\dMatrix.pkl")

    if(verbose):
        print(distanceMatrix)
        print("starting clustering")
    # prediction_data=True, for soft clustering, doesnt work with precomputed metric
    # min_cluster_size = 100, min_samples = 80,

    clusterer = AgglomerativeClustering(n_clusters=numberOfClusters, linkage='average', affinity="precomputed")
    clusterer.fit(distanceMatrix)

    if(verbose):
        print("finished clustering")
    if pruning:
        distanceMatrix = pruneMatrix(distanceMatrix, 10)

    return clusterer



def pruneMatrix(matrix, threshold):
    for i in range(0,len(matrix)):
      if np.max(matrix[i]) <= threshold and np.max(matrix[:, i]) <= threshold:
        np.delete(matrix, i, 0)  # deletes row
        np.delete(matrix, i, 1)  # deletes column
    return matrix


def recalculateDistance(attributeValuePairs, attributeValuePairsCoOccurrence):
    n = len(attributeValuePairs)
    maximumKey = max(attributeValuePairsCoOccurrence, key=attributeValuePairsCoOccurrence.get)
    maximumVal = attributeValuePairsCoOccurrence[maximumKey]
    # print(maximumVal)
    indexOffset=0
    similarityMatrix = np.zeros((n, n))
    attributeValuePairsCopy = attributeValuePairs.copy()
    for i, pair1 in enumerate(attributeValuePairs):
        # if i > 1_000:
            # break
        for j, pair2 in enumerate(attributeValuePairsCopy):
            # if not pair1 == pair2:
                score = distanceScore(pair1,pair2,attributeValuePairsCoOccurrence, maximumVal)
                similarityMatrix[i, j+indexOffset] = score
                # if score > 0.1:
                    # print(pair1+" "+pair2+" " + score.__str__())
        attributeValuePairsCopy.pop(pair1)
        indexOffset += 1
        # if (i % 2000) == 0:
            # print(i)
            # print(len(attributeValuePairsCopy))
    return similarityMatrix



def distanceScore(pair1, pair2, attributeValuePairsCoOccurrence, maximum):
    if pair1 == pair2:
        return 0
    pair3 = frozenset({pair1,pair2})
    if pair3 in attributeValuePairsCoOccurrence:
        return (maximum - attributeValuePairsCoOccurrence[pair3])**2
    else:
        return maximum**2


def saveDistanceMatrix(dict, filename):
    f = open(filename, "wb")
    pickle.dump(dict,f)
    f.close()

def loadDistanceMatrix(filename):
    file = open(filename, "rb")
    loaded = pickle.load(file)
    file.close()
    return loaded