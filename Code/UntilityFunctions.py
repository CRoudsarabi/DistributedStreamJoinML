#turns numpy int into regular int to sent it to java
def decode(map):
    decodedMap = {}
    for key in map:
        decodedKey = key.item()
        decodedList = []
        for element in map[key]:
            decodedList.append(element.item())
        decodedMap[decodedKey] = decodedList
    return decodedMap


def convertToStringMap(attributeValuePairsCoOccurrence):
    cooccurences = {}
    for set in attributeValuePairsCoOccurrence:
        cooccurences['&'.join(list(set))] = attributeValuePairsCoOccurrence[set]
    return cooccurences