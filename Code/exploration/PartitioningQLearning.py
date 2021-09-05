import numpy as np
import matplotlib.pyplot as plt





def main():
    # Environment Settings
    parallelism = 3
    maxLoad = 10
    maxPartitionSize = 4
    overloadPenalty = 100

    # Q-Learning settings
    learningRate = 0.1
    discount = 0.95
    epsilon = 0.95
    epsilonDecay = 0.999998
    episodes = 3000000
    plotEvery = 10000
    resetEpsilonEvery = 2900000

    # we can load an already trained Agent here otherwise we initialize with random values
    startQTable = None

    if startQTable is None:
        #currently hardcoded for parallelism 3
       qTable = np.random.uniform(low=-30, high=-20, size=([maxLoad +1, maxLoad +1, maxLoad+1 , maxPartitionSize, parallelism]))
    else:
        qTable = startQTable
    average = 0
    averageBenchmark = 0
    avgList =[]
    avgListBenchmark = []
    for episode in range(episodes):
        partitions =np.random.randint(low=1, high=5, size=(7))
        currentAssignement = PartitionAssignment(parallelism, maxLoad)
        for i in range(partitions.size):

            partition = partitions[i]
            currentState = (currentAssignement.state[0], currentAssignement.state[1], currentAssignement.state[2], partition-1)
            #lookup Q-Table and perfom action
            if np.random.random() > epsilon:
                partitionNumber = np.argmax(qTable[currentState])
            else:
                partitionNumber = np.random.randint(0, parallelism)

            currentQ = qTable[currentState][partitionNumber]

            currentAssignement.assignToPartition(partition, partitionNumber)

            #calculate the rewards
            if currentAssignement.isOverloaded():
                reward = -overloadPenalty
                newQ = reward
            elif i == partitions.size-1:
                reward = -calculateLoadBalance(currentAssignement.state)
                newQ = reward
                #print("done with partition assignment")
            else:
                reward = 0
                bestQ = np.max(qTable[currentAssignement.state[0], currentAssignement.state[1], currentAssignement.state[2]])
                newQ = (1 - learningRate) * currentQ + learningRate * (reward + discount * bestQ)

            qTable[currentState][partitionNumber] = newQ

            if currentAssignement.isOverloaded():
                break

        epsilon *= epsilonDecay
        average += calculateLoadBalance(currentAssignement.state)
        averageBenchmark += greedyAssignment(partitions,parallelism)
        if (episode % plotEvery == 0) & (episode != 0) :
            #print(currentAssignement.toString(),partitions)
            print(average/plotEvery)
            #print(averageBenchmark / plotEvery)
            avgList.append(average/plotEvery)
            avgListBenchmark.append(averageBenchmark / plotEvery)
            average = 0
            averageBenchmark = 0
        if (episode % resetEpsilonEvery == 0) & (episode != 0) :
            print("reset Epsilon")
            epsilon = 0

    plt.plot(avgList)
    plt.plot(avgListBenchmark)
    plt.ylabel('average')
    plt.xlabel('number of episodes')
    plt.show()


# np.save('qTable', qTable)



def calculateLoadBalance(array):
    return max(array)-min(array)


def calculateLoadBalanceWithAverage(array):
    return max(array)-(sum(array)/len(array))

#TODO
def calculateReplication(array):
    return 0

def greedyAssignment(partitions, parallelism):
    currentAssigment = np.zeros(parallelism, dtype=int)
    for partition in partitions:
        partitionNumber=np.argmin(currentAssigment)
        currentAssigment[partitionNumber] += partition

    return calculateLoadBalance(currentAssigment)

class PartitionAssignment :
    def  __init__(self, parallelism, maxLoad):
        self.maxLoad = maxLoad
        self.state = np.zeros(parallelism, dtype=int)

    def assignToPartition(self, size, partitionNumber):
        self.state[partitionNumber] += size

    def toString(self):
        return f"{self.state}"

    def isOverloaded(self):
        if np.max(self.state) > self.maxLoad:
            return True
        else:
            return False






