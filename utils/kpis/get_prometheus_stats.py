import subprocess
import time
import re
import sys,getopt
import json
from  requests import get


kpis_results=dict()


def get_prometheus_data(url,queries):
    for kpi_name, kpi in queries.items():
        # Store Title of kpi with properly title format
        measure_name = ' '.join(kpi_name.title().split('_'))
        if kpis_results.get(kpi['type'] + '_titles', None) == None:
            kpis_results[kpi['type'] + '_titles']=dict()
        kpis_results[kpi['type'] + '_titles'][kpi_name] = '{}({})'.format(measure_name,kpi['unit'])
        
        params = {
            "query": kpi['query']
        }
        response = get(url, params)
        if response.json()['status'] == 'success':
            results=response.json()['data']['result']

            for result in results:
                element_name = result['metric'][kpi['type']]
                # Setup name without UUID if it's a pod
                if kpi['type'] == "pod":
                    element_name = '-'.join(element_name.split('-')[:-2])
                
                # Create kpi results type dict (pod or instance)
                if kpis_results.get(kpi['type'],None) == None:
                    kpis_results[kpi['type']]=dict()
                
                # Create element dictionary
                if kpis_results[kpi['type']].get(element_name,None) == None:
                    kpis_results[kpi['type']][element_name]=dict()
                
                # Store KPI information with value obtained
                kpis_results[kpi['type']][element_name][kpi_name]=kpi

                # Store value of KPI obtained
                kpis_results[kpi['type']][element_name][kpi_name]['value'] = result['value'][1]

                
                

        print(json.dumps(response.json(), indent=4))

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

    queries= {
        "cpu" : {
            "unit": '%',
            "query":'100 * sum (rate (container_cpu_usage_seconds_total {{namespace="{namespace}"}} [5m])) by (instance) / on (instance) machine_cpu_cores'.format(namespace=namespace),
            "type": 'instance'
        },
        "memory" :  {
            "unit": "%",
            "query": '100 * sum (container_memory_working_set_bytes {{namespace="{namespace}"}}) by (instance) / on (instance) machine_memory_bytes'.format(namespace=namespace),
            "type": 'instance'
        },
        "memory_usage": {
            "unit": "%",
            "query": 'sum (container_memory_working_set_bytes {{namespace="{namespace}"}}) by (pod) / sum (kube_pod_container_resource_limits{{resource="memory", namespace="{namespace}"}}) by (pod)'.format(namespace=namespace),
            "type": 'pod'
        },
        "net_i/o": {
            "unit": "Bytes",
            "query": 'sum by (pod) (rate (container_network_receive_bytes_total {{namespace="{namespace}"}} [1m])) + sum by (pod) (rate (container_network_transmit_bytes_total {{namespace="{namespace}"}} [1m]))'.format(namespace=namespace),
            "type": 'pod'
        },
        "mem_failures": {
            "unit": "Times",
            "query": 'sum by (pod) (changes (container_memory_oom_total {{namespace="{namespace}"}} [5m]))'.format(namespace=namespace),
            "type": 'pod'
        },
        "block_i/o": {
            "unit": "Blocks",
            "query": 'sum by (pod) (rate (container_fs_writes_bytes_total {{namespace="{namespace}"}} [1m])) + sum by (pod) (rate (container_fs_reads_bytes_total {{namespace="{namespace}"}} [1m]))'.format(namespace=namespace),
            "type": 'pod'
        }
    }

    print(json.dumps(queries, indent=4))

    get_prometheus_data(url,queries)

    print(json.dumps(kpis_results, indent=4))

    # Serializing json
    json_object = json.dumps(kpis_results, indent=4)
    
    # Writing to sample.json
    with open(output_file, "w") as outfile:
        outfile.write(json_object)

if __name__ == '__main__':
    main(sys.argv[1:])

