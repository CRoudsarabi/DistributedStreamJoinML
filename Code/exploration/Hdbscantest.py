from sklearn.datasets import make_blobs
from sklearn import datasets
from sklearn.manifold import TSNE
import hdbscan
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns


#https://hdbscan.readthedocs.io/en/latest/basic_hdbscan.html


def test1():
    blobs, labels = make_blobs(n_samples=2000, n_features=10)
    #print(pd.DataFrame(blobs).head())


    clusterer = hdbscan.HDBSCAN(metric='manhattan')
    clusterer.fit(blobs)

    print(clusterer)

    print(clusterer.labels_)
    print(len(clusterer.labels_))
    print(clusterer.labels_.max())
    print(np.unique(clusterer.labels_))

    unique, counts = np.unique(clusterer.labels_, return_counts=True)
    sizes = dict(zip(unique, counts))
    print(sizes)

    newPartition =[]
    for size in sizes:
        newPartition.append(sizes[size])
    print(newPartition)

def test2():
    data = np.load('testdata/clusterable_data.npy')
    print(data.shape)

    plt.scatter(*data.T, s=50, linewidth=0, c='b', alpha=0.25)
    #plt.show()

    clusterer = hdbscan.HDBSCAN(min_cluster_size=15).fit(data)
    color_palette = sns.color_palette('deep', 8)
    cluster_colors = [color_palette[x] if x >= 0
                  else (0.5, 0.5, 0.5)
                  for x in clusterer.labels_]
    cluster_member_colors = [sns.desaturate(x, p) for x, p in
                             zip(cluster_colors, clusterer.probabilities_)]
    plt.scatter(*data.T, s=50, linewidth=0, c=cluster_member_colors, alpha=0.25)
    #plt.show()

    clusterer.condensed_tree_.plot()
    #plt.show()


def test3():
    digits = datasets.load_digits()
    data = digits.data
    projection = TSNE().fit_transform(data)
    plt.scatter(*projection.T, s=50, linewidth=0, c='b', alpha=0.25)
    #plt.show()

    clusterer = hdbscan.HDBSCAN(min_cluster_size=10, prediction_data=True).fit(data)
    color_palette = sns.color_palette('Paired', 12)
    cluster_colors = [color_palette[x] if x >= 0
                      else (0.5, 0.5, 0.5)
                      for x in clusterer.labels_]
    cluster_member_colors = [sns.desaturate(x, p) for x, p in
                             zip(cluster_colors, clusterer.probabilities_)]
    plt.scatter(*projection.T, s=50, linewidth=0, c=cluster_member_colors, alpha=0.25)
    plt.show()

    soft_clusters = hdbscan.all_points_membership_vectors(clusterer)
    color_palette = sns.color_palette('Paired', 12)
    cluster_colors = [color_palette[np.argmax(x)]
                      for x in soft_clusters]
    plt.scatter(*projection.T, s=50, linewidth=0, c=cluster_colors, alpha=0.25)
    plt.show()


test1()