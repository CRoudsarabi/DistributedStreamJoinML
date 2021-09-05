import matplotlib.pyplot as plt
import numpy as np

from collections import deque
import time
import random

from tqdm import tqdm #Progress meter

import tensorflow as tf

from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Dropout, Activation, Flatten
from tensorflow.keras.callbacks import TensorBoard
from tensorflow.keras.optimizers import Adam

import time
import random

# using tensoflow 2.3.1
#print(tf.__version__)

class PDQLAgent:
    def __init__(self, parallelism, maxLoad):

        self.parallelism = parallelism
        self.maxLoad = maxLoad

        self.model = self.createModel()
        #print(self.model.summary())
        self.targetModel = self.createModel()
        #self.targetModel.set_weights(self.model.get_weights())

        self.replayMemory = []
        #self.tensorboard = ModifiedTensorBoard(log_dir=f"logs/{MODEL_NAME}-{int(time.time())}")
        self.targetUpdateCounter = 0

    def createModel(self):
        model = Sequential()
        #input layer with current state and next partition
        model.add(Dense(32, activation="relu", input_shape=[self.parallelism+2]))
        model.add(Dense(64, activation="relu"))
        #model.add(Dropout(.2))
        model.add(Dense(64, activation="relu"))
        #output layer that assigns partition
        model.add(Dense(self.parallelism, activation="softmax"))
        model.compile(loss="mse", optimizer=Adam(lr=0.001), metrics=['accuracy'])
        return model

    def updateReplayMemory(self, transition):
        self.replayMemory.append(transition)

    def sampleReplayMemory(self, sampleSize):
        sample = []
        for i in range(0, sampleSize):
            index = random.randint(0, len(self.replayMemory)-1)
            element = self.replayMemory.pop(index)
            sample.append(element)
        return sample


    def getQ(self, state):
        return self.model.predict(np.array(state).reshape(-1, *state.shape) / self.maxLoad)[0]

    def train(self, terminalState, step):
        if len(self.replayMemory) < 500:
            return

        #print("training")
        #batch = random.sample(self.replayMemory, 64)
        batch = self.sampleReplayMemory(64)

        currentStates = np.array([transition[0] for transition in batch]) / self.maxLoad
        currentQlist = self.model.predict(currentStates)


        newCurrentStates = np.array([transition[3] for transition in batch]) / self.maxLoad
        futureQlist = self.targetModel.predict(newCurrentStates)

        X = []
        y = []

        for index, (currentState, action, reward, newCurrentState, done) in enumerate(batch):
            if not done:
                maxQ = np.max(futureQlist[index])
                newQ = reward + 0.99 * maxQ
            else:
                newQ = reward

            currentQ = currentQlist[index]
            currentQ[action] = newQ

            X.append(currentState)
            y.append(currentQ)

        self.model.fit(np.array(X) / self.maxLoad, np.array(y), batch_size=64, verbose=0, shuffle=False,)

        if terminalState:
            self.targetUpdateCounter += 1

        if self.targetUpdateCounter > 5:
            self.targetModel.set_weights(self.model.get_weights())
            self.targetUpdateCounter = 0

    def createPartitions(self, clustering, parallelism, maxLoad, clusterSizes):

        currentAssignment = PartitionAssignment(parallelism, maxLoad)

        partitions, clusters = createPartition(clustering, clusterSizes)
        remaining = sum(partitions)

        for i in range(len(partitions)):

            currentCluster = partitions[i]
            currentState = np.array([
                currentAssignment.state[0], currentAssignment.state[1], currentAssignment.state[2], currentCluster - 1,
                remaining])
            remaining = remaining - currentCluster

            partitionNumber = np.argmax(self.getQ(currentState))

            currentAssignment.assignToPartition(currentCluster, partitionNumber, clusters[i])


        return currentAssignment



def trainAgent(parallelism, maxLoad, numberOfEpisodes, clustering, clusterSizes, clusterOverlap):
    # Environment Settings
    maxPartitionSize = 4
    overloadPenalty = 2000

    agent = PDQLAgent(parallelism, maxLoad)

    # Q-Learning settings
    learningRate = 0.1
    discount = 0.95
    epsilon = 0.95
    epsilonDecay = 0.9995
    episodes = numberOfEpisodes
    resetEpsilonEvery = numberOfEpisodes - 20


    #numberOfRandomActions = 0
    #numberOfNonRandomActions = 0

    #ASCII = True for windows

    for episode in tqdm(range(1, episodes + 1), ascii=True, unit='episodes'):

        partitions, clusters = createPartition(clustering, clusterSizes)
        #partitions = np.random.randint(low=1, high=5, size=numberOfPartitions)
        remaining = sum(partitions)
        currentAssignment = PartitionAssignment(parallelism, maxLoad)

        for i in range(len(partitions)):

            done = False
            training_partition = partitions[i]
            currentState = np.array([
            currentAssignment.state[0], currentAssignment.state[1], currentAssignment.state[2], training_partition - 1,
                remaining])
            remaining = remaining - training_partition
            # lookup NN and perform action
            if np.random.random() > epsilon:
                partitionNumber = np.argmax(agent.getQ(currentState))
                #numberOfNonRandomActions += 1
            else:
                partitionNumber = np.random.randint(0, parallelism)
                #numberOfRandomActions += 1

            currentAssignment.assignToPartition(training_partition, partitionNumber, clusters[i])

            # calculate the rewards
            if currentAssignment.isOverloaded():
                reward = -overloadPenalty
                nextPartition=0
            elif i == ( len(partitions) - 1 ):
                reward = -calculateLoadBalance(currentAssignment.state)
                if clusterOverlap is not None:
                    reward += calculateOverlap(currentAssignment, clusterOverlap)
                done = True
                nextPartition = 0
                # print("done with partition assignment")
            else:
                reward = 0
                nextPartition = partitions[i+1]

            newState = np.array([currentAssignment.state[0], currentAssignment.state[1], currentAssignment.state[2],
                                nextPartition, remaining])
            agent.updateReplayMemory((currentState, partitionNumber, reward, newState, done))
            if done:
                agent.train(done, i)

            if currentAssignment.isOverloaded():
                break

        epsilon *= epsilonDecay

        if (episode % resetEpsilonEvery == 0) & (episode != 0):

            epsilon = 0

    return agent


def calculateLoadBalance(array):
    return max(array)-min(array)


def calculateLoadBalanceWithAverage(array):
    return max(array)-(sum(array)/len(array))

def calculateReplication(array):
    return 0

def greedyAssignment(partitions, parallelism):
    currentAssigment = np.zeros(parallelism, dtype=int)
    for partition in partitions:
        partitionNumber = np.argmin(currentAssigment)
        currentAssigment[partitionNumber] += partition

    return calculateLoadBalance(currentAssigment)

def createPartition(clustering, clusterSizes):
    newPartition = []
    newClusters = []
    for size in clusterSizes:
        newPartition.append(clusterSizes[size])
        newClusters.append(size)
    return newPartition, newClusters


def createRandomPartition(low,high,size):
    return np.random.randint(low=low, high=high, size=size)

def calculateOverlap(currentAssignment, clusterOverlap):
    association_costs = 0
    if clusterOverlap is None:
        return 0
    for partition in currentAssignment.stateForOverlap:
        partitionCopy = partition.copy()
        for element in partition:
            for element2 in partitionCopy:
                pair = frozenset(element, element2)
                association_costs += clusterOverlap[pair]
            del partitionCopy[element]
    return association_costs;

def estimateSizes(clustering):
    unique, counts = np.unique(clustering.labels_, return_counts=True)
    sizes = dict(zip(unique, counts))
    return sizes;

class PartitionAssignment :
    def  __init__(self, parallelism, maxLoad):
        self.maxLoad = maxLoad
        self.state = np.zeros(parallelism, dtype=int)
        self.stateForOverlap = {}

    def assignToPartition(self, size, partitionNumber, cluster):
        self.state[partitionNumber] += size
        if partitionNumber in self.stateForOverlap:
            self.stateForOverlap[partitionNumber] = self.stateForOverlap[partitionNumber] + [cluster]
        else:
            self.stateForOverlap[partitionNumber] = [cluster]

    def toString(self):
        return f"{self.state}"

    def isOverloaded(self):
        if np.max(self.state) > self.maxLoad:
            return True
        else:
            return False
