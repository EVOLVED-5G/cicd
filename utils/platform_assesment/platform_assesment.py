import requests
import json
from time import sleep
import sys

def get_kpi_value(analitics_url ,execution_id, kpi_info):
    print('******')
    print(kpi_info)
    print('******')
    measurement=kpi_info.get('Measurement')
    kpi=kpi_info.get('KPI')
    url= analitics_url + '/statistical_analysis/uma_mydb?experimentid=' + execution_id +'&measurement=' + measurement + '&kpi=' + kpi
    print(url)
    response = requests.get(url)
    if not response.ok:
        raise('Error getting Measurement ' + kpi [0] + ' of kpi ' + kpi[1] + ' for ExecutionId ' + execution_id)

    print(json.dumps(response.json(), indent=4))
    return response.json()

descriptor = {
  "Application": None,
  "Automated": True,
  "ExclusiveExecution": False,
  "ExperimentType": "Standard",
  "Extra": {},
  "NSs": [],
  "Parameters": {},
  "Remote": None,
  "RemoteDescriptor": None,
  "ReservationTime": None,
  "Scenario": None,
  "Slice": None,
  "TestCases": [
    "EvolvedWp5"
  ],
  "UEs": [],
  "Version": "2.1.0"
}


if __name__ == '__main__':
    
    # total arguments
    n = len(sys.argv)
    
    if n != 4:
            print("expected: " + sys.argv[0] + " <elcm_url> <analitics_url> <output_filename>")
            exit(255)

    elcm_url = sys.argv[1]
    analitics_url = sys.argv[2]
    output_filename = sys.argv[3]

    execution_run_url = elcm_url + '/experiment/run'

    response = requests.post(execution_run_url, json = descriptor)

    execution_id=''
    if not response.ok:
        print(response)
        raise('Execution of experiment fails')
    else:
        execution_id_int = response.json().get('ExecutionId',None)
        if(execution_id_int != None):
            execution_id = str(execution_id_int)
        print('ExecutionId: ' + execution_id)

    if execution_id == '' or execution_id == None:
        raise('ExecutionId is not present on response, something fails')


    # execution_id = '550033'
    execution_status_url = elcm_url + '/execution/' + execution_id + '/status'

    execution_status=''
    status='Running'
    while status != 'Not Running':
        print('Pollìng execution status')
        response = requests.get(execution_status_url)
        if not response.ok:
            raise('Response error requesting status of ExecutionId ' + execution_id)
        execution_status = response.json()
        print(execution_status)
        status = execution_status.get('Status')
        if status != 'Not Running':
            sleep(10)
    print('Polling finished, final response:')
    print(execution_status)

    execution_kpis_url = elcm_url + '/execution/' + execution_id + '/kpis'
    response = requests.get(execution_kpis_url)
    if not response.ok:
        raise('Response error requesting kpi list for ExecutionId ' + execution_id)

    execution_kpis = response.json()

    results=dict()
    results['KPIs']=dict()
    for kpi_info in execution_kpis.get('KPIs'):
        result=get_kpi_value(analitics_url, execution_id, kpi_info)
        if result.get('experimentid') != {} and result.get('experimentid').get(execution_id) != {}:
            results['KPIs'].update(result.get('experimentid').get(execution_id))
            results['KPIs'][kpi_info.get('KPI')].update({ "status": True })
            results['KPIs'][kpi_info.get('KPI')].update(kpi_info)
        else:
            results['KPIs'].update({ kpi_info.get('KPI'): { "status": False } })
            print('Measurement ' + kpi_info.get('Measurement') + ' of kpi ' + kpi_info.get('KPI') + ' FAILS')

    results['Verdict'] = execution_status.get('Verdict')

    print('---Result JSON Generated---')
    print(json.dumps(results, indent=4))
    print('---------------------------')'

    with open(output_filename, 'w') as outfile:
        json.dump(results, outfile)


