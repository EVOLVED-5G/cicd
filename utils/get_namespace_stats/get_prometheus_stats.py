import subprocess
import time
import re
import pandas as pd
import sys,getopt
import json
from  requests import get



top_information=dict()
top_processed_information = dict()
top_processed_information['PODS'] = dict()
top_processed_information['TITLES'] = dict()

PROMETHEUS_URL

def get_prometheus_data(url, params):
    params=dict()
    params['query'] = '100*sum(rate(container_cpu_usage_seconds_total{namespace="andres"}[5m]))by(instance)/on(instance)machine_cpu_cores'
    get(url, params)


    # curl -g 'http://prometheus.mon.int:30048/api/v1/query?query=100*sum(rate(container_cpu_usage_seconds_total{namespace="andres"}[5m]))by(instance)/on(instance)machine_cpu_cores'

def add_metrics(titles,results):
    titles=titles.replace('cores','%')
    titles=titles.replace('bytes','megabytes')
    titles_array = titles.split(':')
    titles_array.pop(0)

    info_array = results.split(':')
    name=info_array[0]
    info_array.pop(0)
    
    if top_information.get(name,None) == None:
        top_information[name]=dict()
        for title in titles_array:
            top_information[name][title]=list()

    titles_array_len = len(titles_array)
    info_array_len = len(info_array)

    if info_array_len != titles_array_len:
        raise Exception('info array has not the same length than titles')

    index = 0
    for data in info_array:
        data_clean=re.search("^([0-9]+)([a-zA-Z]*$)",data)
        data_value = float(data_clean[1])
        if data_clean[2] == 'm':
            data_value = data_value / 10
        top_information[name][titles_array[index]].append(data_value)
        top_processed_information['TITLES'][titles_array[index]]=data_clean[2]
        index = index + 1

def getData(namespace):
    command="kubectl top pods -n {} ".format(namespace) + '| awk \'{ print $1":"$2":"$3}\''
    p = subprocess.run(command,stdout=subprocess.PIPE, shell=True,universal_newlines=True)
    return p.stdout.split('\n')

def main(argv):
    namespace = None
    output_file = None
    url = None

    try:
        opts, args = getopt.getopt(argv,"hn:u:o:")
    except getopt.GetoptError:
        print ('get_prometheus_stats.py -n <namespace> -u <prometheus_url> -o <output_file>')
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print ('get_prometheus_stats.py -n <namespace> -u <prometheus_url> -o <output_file>')
            sys.exit()
        elif opt in ("-n"):
            namespace = arg
        elif opt in ("-u"):
            url = arg
        elif opt in ("-o"):
            output_file = arg
    if namespace == None or output_file == None or url == None:
        print ('get_prometheus_stats.py -n <namespace> -u <prometheus_url> -o <output_file>')
        exit(1)
    
    
    # for index in range(0,int(loops)):
        # results = getData(namespace)
    get_prometheus_data(url,params)
        # title_array = results[0]
        # results.pop(0)

        # for result in results:
        #     if result:
        #         add_metrics(title_array,result)
        
        # print(json.dumps(top_information, indent=4))

    #     time.sleep(float(delay))

    print(top_information)

    # for pod_name, info in top_information.items():
    #     top_processed_information['PODS'][pod_name] = dict()
    #     df = pd.DataFrame(info)
    #     mean_value=df.mean(axis=0,skipna = True)
    #     max_value=df.max(axis=0,skipna = True)
    #     min_value=df.min(axis=0,skipna = True)
    #     std_value=df.std(axis=0,skipna = True)
    #     median_value=df.median(axis=0,skipna = True)
    #     for title, values in info.items():
    #         top_processed_information['PODS'][pod_name][title] = dict()
    #         top_processed_information['PODS'][pod_name][title]['Mean'] = str(mean_value[title])
    #         top_processed_information['PODS'][pod_name][title]['Max'] = str(max_value[title])
    #         top_processed_information['PODS'][pod_name][title]['Min'] = str(min_value[title])
    #         top_processed_information['PODS'][pod_name][title]['Standar Deviation'] = str(std_value[title])
    #         top_processed_information['PODS'][pod_name][title]['Median'] = str(median_value[title])

    # print(top_processed_information)
    # # Serializing json
    # json_object = json.dumps(top_processed_information, indent=4)
    
    # # Writing to sample.json
    # with open(output_file, "w") as outfile:
    #     outfile.write(json_object)


if __name__ == '__main__':
    main(sys.argv[1:])

