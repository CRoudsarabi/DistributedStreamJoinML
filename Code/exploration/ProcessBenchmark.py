import json

def processBenchmark(pruneBoolean, filename = 'nobench\\nobench_data.json' ):
    with open(filename) as loadedJsonFile:

        sampleBench = json.load(loadedJsonFile)

    attributeValuePairsCoOccurrence = {}
    attributeValuePairs = {}
    print(len(sampleBench))
    for i,object in enumerate(sampleBench):
        if i > 1000:
            break
        #print(i)
        #numberOfAttributes = len(object)

        objectCopy = object.copy()
        for attributeX in object:
            attributeValuePair = attributeX+json.dumps(object[attributeX])
            if attributeValuePair in attributeValuePairs:
                attributeValuePairs[attributeValuePair] += 1
            else:
                attributeValuePairs[attributeValuePair] = 1
            for attributeY in objectCopy:
                if not attributeX == attributeY:
                    coOccurence = frozenset({attributeX+json.dumps(object[attributeX]),attributeY+json.dumps(object[attributeY])})
                    if coOccurence in attributeValuePairsCoOccurrence:
                        attributeValuePairsCoOccurrence[coOccurence] += 1
                    else:
                        attributeValuePairsCoOccurrence[coOccurence] = 1
            objectCopy.pop(attributeX)


    return (attributeValuePairs, attributeValuePairsCoOccurrence)

def testPreprocessing():
    attributeValuePairs, attributeValuePairsCoOccurrence = processBenchmark()
    attributeValuePairsPreprocessed, attributeValuePairsCoOccurrencePreprocessed = preprocessing(attributeValuePairs, attributeValuePairsCoOccurrence, 5)

def preprocessing(attributeValuePairs, attributeValuePairsCoOccurrence, parallelism):
    attributeValuePairsModified = {}
    attributeValuePairsCoOccurrenceModified = {}
    for i,pair in enumerate(attributeValuePairs):
        attributeValuePairsModified[pair] = attributeValuePairs[pair]

        #if json.dumps(object[attributeX]) == "True" or json.dumps(object[attributeX]) == "False":
            #objectCopy.pop(attributeX)

    return (attributeValuePairsModified, attributeValuePairsCoOccurrenceModified)