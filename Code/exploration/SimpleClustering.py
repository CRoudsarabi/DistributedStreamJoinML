
#from tqdm import tqdm #Progress meter
#import pandas as pd
import matplotlib.pyplot as plt
import hdbscan
from sklearn.cluster import AgglomerativeClustering
from sklearn.cluster import AffinityPropagation
#from sklearn.cluster import DBSCAN
import numpy as np



def main(attributeValuePairs, attributeValuePairsCoOccurrence):
    n =len(attributeValuePairs)
    print(n)
    print(len(attributeValuePairsCoOccurrence))
    pruning = True

    clustering = agglClustering(attributeValuePairs, attributeValuePairsCoOccurrence, pruning)
    print(clustering)
    print(max(clustering))
    unique, counts = np.unique(clustering, return_counts=True)
    cluster_sizes = dict(zip(unique, counts))
    print(cluster_sizes)

    #[(clusterIndex, clusterSize) for k, v in dict.items()]

    clusterIndex = []
    clusterSize = []
    for key, value in cluster_sizes.items():
        clusterIndex.append(key)
        clusterSize.append(value)

    y_pos = np.arange(len(clusterIndex))
    plt.bar(y_pos, clusterSize, align='center', alpha=0.5)
    plt.ylabel('Size')
    plt.title('Cluster Sizes: Agglomerative Clustering, 1000 Json objects')
    plt.show()

def agglClustering(attributeValuePairs, attributeValuePairsCoOccurrence, pruning):
    distanceMatrix = recalculateDistance(attributeValuePairs, attributeValuePairsCoOccurrence)
    tril = np.tril_indices_from(distanceMatrix, -1)  # take lower & upper triangle's indices
    triu = np.triu_indices_from(distanceMatrix, 1)  # (without diagonal)
    distanceMatrix[tril] = distanceMatrix[triu]

    if pruning:
        distanceMatrix = pruneMatrix(distanceMatrix, 10)

    AggClusterDistObj=AgglomerativeClustering(n_clusters=7,linkage='average',affinity="precomputed")
    clustering = AggClusterDistObj.fit_predict(distanceMatrix)
    return clustering

def affPropagation(attributeValuePairs, attributeValuePairsCoOccurrence, pruning):
    similarityMatrix = recalculate(attributeValuePairs, attributeValuePairsCoOccurrence)
    tril = np.tril_indices_from(similarityMatrix, -1)  # take lower & upper triangle's indices
    triu = np.triu_indices_from(similarityMatrix, 1)  # (without diagonal)
    similarityMatrix[tril] = similarityMatrix[triu]

    if pruning:
        similarityMatrix = pruneMatrix(similarityMatrix, 10)

    affClusterDistObj=AffinityPropagation(n_clusters=7,linkage='average',affinity="precomputed")
    clustering = affClusterDistObj.fit_predict(similarityMatrix)
    return clustering

def pruneMatrix(matrix, threshold):
    for i in range(0,len(matrix)):
      if np.max(matrix[i]) <= threshold and np.max(matrix[:, i]) <= threshold:
         np.delete(matrix, i, 0) #deletes row
         np.delete(matrix, i, 1) #deletes column
    return matrix

def recalculate(attributeValuePairs, attributeValuePairsCoOccurrence):
    n = len(attributeValuePairs)
    similarityMatrix = np.zeros((n, n))
    attributeValuePairsCopy = attributeValuePairs.copy()
    for i, pair1 in enumerate(attributeValuePairs):
        #if i > 1_000:
            #break
        for j, pair2 in enumerate(attributeValuePairsCopy):
            if not pair1 == pair2:
                score = similarityScore(pair1,pair2,attributeValuePairs,attributeValuePairsCoOccurrence)
                similarityMatrix[i, j] = score
                #if score > 0.1:
                    #print(pair1+" "+pair2+" " + score.__str__())
        attributeValuePairsCopy.pop(pair1)
        if (i % 2000) == 0:
            print(i)
            #print(len(attributeValuePairsCopy))
    return similarityMatrix

def recalculateDistance(attributeValuePairs, attributeValuePairsCoOccurrence):
    n = len(attributeValuePairs)
    similarityMatrix = np.zeros((n, n))
    attributeValuePairsCopy = attributeValuePairs.copy()
    for i, pair1 in enumerate(attributeValuePairs):
        #if i > 1_000:
            #break
        for j, pair2 in enumerate(attributeValuePairsCopy):
            if not pair1 == pair2:
                score = distanceScore(pair1,pair2,attributeValuePairsCoOccurrence)
                similarityMatrix[i, j] = score
                #if score > 0.1:
                    #print(pair1+" "+pair2+" " + score.__str__())
        attributeValuePairsCopy.pop(pair1)
        if (i % 2000) == 0:
            print(i)
            #print(len(attributeValuePairsCopy))
    return similarityMatrix

def similarityScore(pair1, pair2,attributeValuePairs, attributeValuePairsCoOccurrence):
    if pair1 == pair2 :
        return 1
    pair3 = frozenset({pair1,pair2})
    if pair3 in attributeValuePairsCoOccurrence:
        return (attributeValuePairsCoOccurrence[pair3] / (attributeValuePairs[pair1]+attributeValuePairs[pair2]))
    else:
        return 0

def distanceScore(pair1, pair2, attributeValuePairsCoOccurrence):
    if pair1 == pair2 :
        return 1
    pair3 = frozenset({pair1,pair2})
    if pair3 in attributeValuePairsCoOccurrence:
        return (attributeValuePairsCoOccurrence[pair3])
    else:
        return 0