import json

def main():
    with open('nobench\\nobench_data.json') as loadedJsonFile:

        sampleBench = json.load(loadedJsonFile)

    for object in sampleBench :
        print(object['num'])
        print(object['nested_arr'])
        if 'sparse_000' in object:
            print(object['sparse_000'])
            print(object['sparse_009'])
        else:
            print('sparse_000 not part of this object')





