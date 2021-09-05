import matplotlib.pyplot as plt
import numpy as np

from collections import deque
import time
import random

from tqdm import tqdm #Progress meter

from exploration.PartitioningQLearning import PartitionAssignment
from exploration.PartitioningQLearning import greedyAssignment
from exploration.PartitioningQLearning import calculateLoadBalance

import tensorflow as tf

from keras.models import Sequential
from keras.layers import Dense, Dropout, Activation, Flatten
from keras.callbacks import TensorBoard
from keras.optimizers import Adam

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
        for i in range(0,sampleSize):
            index = random.randint(0, len(self.replayMemory)-1)
            element = self.replayMemory.pop(index)
            sample.append(element)
        return sample


    def getQ(self, state):
        return self.model.predict(np.array(state).reshape(-1, *state.shape) / self.maxLoad)[0]

    def train(self, terminalState, step):
        if len(self.replayMemory) < 5_000:
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

def main():
    # Environment Settings
    parallelism = 3
    maxLoad = 10
    maxPartitionSize = 4
    overloadPenalty = 200

    agent = PDQLAgent(parallelism,maxLoad)

    # Q-Learning settings
    learningRate = 0.1
    discount = 0.95
    epsilon = 0.95
    epsilonDecay = 0.9995
    episodes = 3_000
    plotEvery = 100
    resetEpsilonEvery = 2_000

    average = 0
    averageBenchmark = 0
    avgList = []
    avgListBenchmark = []

    numberOfRandomActions = 0
    numberOfNonRandomActions = 0

    #ASCII = True for windows

    for episode in tqdm(range(1, episodes + 1), ascii=True, unit='episodes'):

        partitions = np.random.randint(low=1, high=5, size=7)
        remaining = sum(partitions)
        currentAssignment = PartitionAssignment(parallelism, maxLoad)

        for i in range(partitions.size):

            done = False
            partition = partitions[i]
            currentState = np.array([
            currentAssignment.state[0], currentAssignment.state[1], currentAssignment.state[2], partition - 1, remaining])
            remaining = remaining - partition
            # lookup NN and perform action
            if np.random.random() > epsilon:
                partitionNumber = np.argmax(agent.getQ(currentState))
                numberOfNonRandomActions += 1
            else:
                partitionNumber = np.random.randint(0, parallelism)
                numberOfRandomActions += 1

            currentAssignment.assignToPartition(partition, partitionNumber)

            # calculate the rewards
            if currentAssignment.isOverloaded():
                reward = -overloadPenalty
                nextPartition=0
            elif i == partitions.size - 1:
                reward = -calculateLoadBalance(currentAssignment.state)
                done = True
                nextPartition = 0
                # print("done with partition assignment")
            else:
                reward = 0
                nextPartition = partitions[i+1]


            newState =np.array([currentAssignment.state[0], currentAssignment.state[1], currentAssignment.state[2], nextPartition, remaining])
            agent.updateReplayMemory((currentState,partitionNumber,reward,newState,done))
            if done:
                agent.train(done, i)

            if currentAssignment.isOverloaded():
                break

        epsilon *= epsilonDecay
        average += calculateLoadBalance(currentAssignment.state)
        averageBenchmark += greedyAssignment(partitions, parallelism)
        if (episode % plotEvery == 0) & (episode != 0):
            print(currentAssignment.toString(),partitions)
            print(average / plotEvery)
            #print(numberOfNonRandomActions)
            # print(averageBenchmark / plotEvery)
            avgList.append(average / plotEvery)
            avgListBenchmark.append(averageBenchmark / plotEvery)
            average = 0
            averageBenchmark = 0
        if (episode % resetEpsilonEvery == 0) & (episode != 0):
            print("reset Epsilon")
            epsilon = 0

    plt.plot(avgList)
    plt.plot(avgListBenchmark)
    plt.ylabel('average')
    plt.xlabel('number of episodes')
    plt.show()
