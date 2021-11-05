String returnBody(){
    return("""
{
  "Application": null,
  "Automated": true,
  "ExclusiveExecution": false,
  "ExperimentType": "Standard",
  "Extra": {},
  "NSs": [],
  "Parameters": {},
  "Remote": null,
  "RemoteDescriptor": null,
  "ReservationTime": null,
  "Scenario": null,
  "Slice": null,
  "TestCases": [
    "Simple Test Case"
  ],
  "UEs": [],
  "Version": "2.1.0"
}
"""
)
    }
pipeline {
    agent { node {label 'evol5-slave2'}  }

    // parameters {
    //     // ADD more parameters
    // }

    environment {
        MSG = returnBody()
    }

    stages {
        
        // For the moment, the token authorization is HARDCODED
        // stage('Execute the experiment') {
        //     steps {

        //     user: evolved5gpass
        //     pass: evolved5g

        //     command: ACCESS_TOKEN=`curl -X GET "https://10.11.23.220:8082/auth/get_token" -H "accept: application/json" -H "authorization: Basic ZXZvbHZlZDVncGFzczpldm9sdmVkNWc=" --insecure| jq -r .result `

        //     }   
        // }
        stage('Check the connectivity with UMA ECLM') {
            steps {
                dir ("${env.WORKSPACE}") {

                sh '''
                ping -c 3 10.11.23.220
                '''

                }   
            }
        }

        stage('Execute the Experiment in the platform') {
            steps {
                dir ("${env.WORKSPACE}") {

                        sh '''
                        ExecutionId=`curl -X POST "https://10.11.23.220:8082/elcm/api/v0/run" -H "accept: application/json" -H "authorization: Basic ZXZvbHZlZDVncGFzczpldm9sdmVkNWc=" -H "Content-Type: application/json" --data "${MSG} " --insecure| jq -r .ExecutionId `
                        curl -X GET "https://10.11.23.220:8082/execution${ExecutionId}/json" -H "accept: application/json" -H "authorization: Basic ZXZvbHZlZDVncGFzczpldm9sdmVkNWc=" --insecure
                        curl "http://10.11.23.220:5003/statistical_analysis/uma_mydb?experimentid=15012&measurement=server_5g&kpi=Jitter%20(ms)"
                        '''
                    }
            }
        }

    }
}