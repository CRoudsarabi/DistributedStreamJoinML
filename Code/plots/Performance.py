import numpy as np
import matplotlib.pyplot as plt

import hdbscan

from sklearn.datasets import make_blobs,make_moons,make_circles


from sklearn.cluster import AgglomerativeClustering,KMeans,DBSCAN

from sklearn.mixture import GaussianMixture

import time


def plot_performance():
    number_of_samples =1000
    list_of_times_aggl = []
    intervalls =[1000,2000,3000,4000,5000,6000,7000,8000,9000,10_000]

    for x in range(10):
        blobs = make_blobs(n_samples=number_of_samples, random_state=206)
        X, y = blobs
        start = time.time()
        AgglomerativeClustering().fit_predict(X);
        end = time.time()
        print(end - start)
        list_of_times_aggl.append(end - start)
        number_of_samples += 1000

    list_of_times_kmeans = []
    number_of_samples = 1000
    for x in range(10):
        blobs = make_blobs(n_samples=number_of_samples, random_state=206)
        X, y = blobs
        start = time.time()
        KMeans(3).fit_predict(X);
        end = time.time()
        print(end - start)
        list_of_times_kmeans.append(end - start)
        number_of_samples += 1000

    list_of_times_hdbscan = []
    number_of_samples = 1000
    for x in range(10):
        blobs = make_blobs(n_samples=number_of_samples, random_state=206)
        X, y = blobs
        start = time.time()
        hdbscan.HDBSCAN().fit_predict(X);
        end = time.time()
        print(end - start)
        list_of_times_hdbscan .append(end - start)
        number_of_samples += 1000

    plt.plot(intervalls, list_of_times_aggl, label='Agglomerative Clustering')
    plt.plot(intervalls, list_of_times_kmeans, label='K-Means')
    plt.plot(intervalls, list_of_times_hdbscan, label='HDBSCAN')
    plt.xlabel("Number of Samples")
    plt.ylabel("Time")
    #plt.plot([0.1, 0.2, 0.3, 0.4], [1, 4, 9, 16], label='second plot')
    plt.legend()
    plt.show()

plot_performance()