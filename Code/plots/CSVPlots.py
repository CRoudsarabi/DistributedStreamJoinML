import numpy as np
import matplotlib.pyplot as plt
import pandas as pd


def plot_Csv_load():

    lb_data = pd.read_csv("load_balancing.csv")
    lb_numpy = lb_data.to_numpy()

    barWidth = 0.25
    name_1 = lb_numpy[0][0]
    name_2 = lb_numpy[1][0]
    name_3 = lb_numpy[2][0]

    ind = np.arange(3)

    plt.bar(ind,  lb_numpy[0][1:4], width = barWidth, label=name_1)
    plt.bar(ind + barWidth, lb_numpy[1][1:4],width = barWidth, label=name_2)
    plt.bar(ind + (2*barWidth), lb_numpy[2][1:4],width = barWidth, label=name_3)
    plt.xlabel("Parallelism")
    plt.ylabel("Load-Balance")
    plt.xticks([r + barWidth for r in range(3)], ['3', '5', '10'])

    plt.legend()
    plt.show()

def plot_Csv_load_rw():

    lb_data = pd.read_csv("load_balancing_rw.csv")
    lb_numpy = lb_data.to_numpy()

    barWidth = 0.2
    name_1 = lb_numpy[0][0]
    name_2 = lb_numpy[1][0]
    name_3 = lb_numpy[2][0]

    ind = np.arange(4)

    plt.bar(ind,  lb_numpy[0][1:5], width=barWidth, label=name_1)
    plt.bar(ind + barWidth, lb_numpy[1][1:5], width=barWidth, label=name_2)
    plt.bar(ind + (2*barWidth), lb_numpy[2][1:5], width=barWidth, label=name_3)
    plt.xlabel("Parallelism")
    plt.ylabel("Load-Balance")
    plt.xticks([r + barWidth for r in range(4)], ['3', '5', '10', '20'])

    plt.legend()
    plt.show()


def plot_Csv_max_load_rw():

    lb_data = pd.read_csv("max_load.csv")
    lb_numpy = lb_data.to_numpy()

    barWidth = 0.2
    name_1 = lb_numpy[0][0]
    name_2 = lb_numpy[1][0]
    name_3 = lb_numpy[2][0]

    ind = np.arange(4)

    plt.bar(ind,  lb_numpy[0][1:5], width=barWidth, label=name_1)
    plt.bar(ind + barWidth, lb_numpy[1][1:5], width=barWidth, label=name_2)
    plt.bar(ind + (2*barWidth), lb_numpy[2][1:5], width=barWidth, label=name_3)
    plt.xlabel("Parallelism")
    plt.ylabel("Maximum Load")
    plt.xticks([r + barWidth for r in range(4)], ['3', '5', '10', '20'])

    plt.legend()
    plt.show()

def plot_Csv_load_without_feedback():
    lb_data = pd.read_csv("load_balancing_without_feedback.csv")
    lb_numpy = lb_data.to_numpy()

    barWidth = 0.25
    name_1 = lb_numpy[0][0]
    name_2 = lb_numpy[1][0]
    name_3 = lb_numpy[2][0]

    ind = np.arange(3)

    plt.bar(ind, lb_numpy[0][1:4], width=barWidth, label=name_1)
    plt.bar(ind + barWidth, lb_numpy[1][1:4], width=barWidth, label=name_2)
    plt.bar(ind + (2 * barWidth), lb_numpy[2][1:4], width=barWidth, label=name_3)
    plt.xlabel("Parallelism")
    plt.ylabel("Load-Balance")
    plt.xticks([r + barWidth for r in range(3)],['3', '5', '10'])

    plt.legend()
    plt.show()

def plot_Csv_feedback():
    lb_data = pd.read_csv("load_feedback.csv")
    lb_numpy = lb_data.to_numpy()

    ind = np.arange(10)


    plt.plot(ind, lb_numpy[0])
    plt.xlabel("Iterations")
    plt.ylabel("Load-Balance")
    plt.show()

def plot_Csv_parallelism():
        lb_data = pd.read_csv("parallelism.csv")
        lb_numpy = lb_data.to_numpy()

        barWidth = 0.4

        ind = np.arange(4)


        plt.bar(ind, lb_numpy[0], width=barWidth)

        plt.xlabel("Parallelism")
        plt.ylabel("Time")
        plt.xticks([x for x in range(4)], ['3', '5', '8', '10'])

        plt.show()

def plot_Csv_replication():
    replication_data = pd.read_csv("replication.csv")
    rep_numpy = replication_data.to_numpy()

    barWidth = 0.25
    name_1 = rep_numpy[0][0]
    name_2 = rep_numpy[1][0]
    name_3 = rep_numpy[2][0]

    ind = np.arange(3)

    plt.bar(ind,  rep_numpy[0][1:4], width = barWidth, label=name_1)
    plt.bar(ind + barWidth, rep_numpy[1][1:4],width = barWidth, label=name_2)
    plt.bar(ind + (2*barWidth), rep_numpy[2][1:4],width = barWidth, label=name_3)
    plt.xlabel("Parallelism")
    plt.ylabel("Replication")
    plt.xticks([r + barWidth for r in range(3)], ['3', '5', '10'])

    plt.legend()
    plt.show()


def plot_Csv_replication_rw():
    replication_data = pd.read_csv("replication_rw.csv")
    rep_numpy = replication_data.to_numpy()

    barWidth = 0.2
    name_1 = rep_numpy[0][0]
    name_2 = rep_numpy[1][0]
    name_3 = rep_numpy[2][0]

    ind = np.arange(4)

    plt.bar(ind,  rep_numpy[0][1:5], width = barWidth, label=name_1)
    plt.bar(ind + barWidth, rep_numpy[1][1:5],width = barWidth, label=name_2)
    plt.bar(ind + (2*barWidth), rep_numpy[2][1:5],width = barWidth, label=name_3)
    plt.xlabel("Parallelism")
    plt.ylabel("Replication")
    plt.xticks([r + barWidth for r in range(4)], ['3', '5', '10', '20'])

    plt.legend()
    plt.show()

def plot_Csv_avpairs():
    replication_data = pd.read_csv("avpairs.csv")
    rep_numpy = replication_data.to_numpy()

    barWidth = 0.4

    ind = np.arange(3)

    plt.bar(ind,  rep_numpy[0], width=barWidth)
    plt.xlabel("Approach")
    plt.ylabel("Number of attribute-value pairs in %")
    plt.xticks([r for r in range(3)], ['Total', 'Agglomerative Clustering', 'HDBSCAN'])

    plt.show()

def plot_Csv_accuracy():
    replication_data = pd.read_csv("accuracy.csv")
    rep_numpy = replication_data.to_numpy()

    barWidth = 0.4

    ind = np.arange(3)

    plt.bar(ind,  rep_numpy[0], width=barWidth)
    plt.xlabel("Approach")
    plt.ylabel("Accuracy in %")
    plt.xticks([r for r in range(3)], ['Total', 'Agglomerative Clustering', 'HDBSCAN'])

    plt.show()





#plot_Csv_load_without_feedback()
#plot_Csv_feedback()
#plot_Csv_load()
#plot_Csv_load_rw()
#plot_Csv_max_load_rw()
#plot_Csv_replication()
#plot_Csv_replication_rw()
#plot_Csv_parallelism()
plot_Csv_avpairs()
plot_Csv_accuracy()
