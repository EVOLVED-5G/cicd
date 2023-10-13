String netappName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    var= var.toLowerCase()
    return var ;
}

def getNamespace(deployment,name) {
    String var = deployment
    if("openshift".equals(var)) {
        return "evol5-capif";
    } else {
        return name;
    }
}

def getAgent(deployment) {
    String var = deployment
    if ('openshift'.equals(var)) {
        return 'evol5-openshift'
    } else if ('kubernetes-athens'.equals(var)) {
        return 'evol5-athens'
    } else if ('kubernetes-cosmote'.equals(var)) {
        return 'evol5-cosmote'
    } else {
        return 'evol5-slave'
    }
}

def getReportFilename(String netappNameLower) {
    return '019-report-deploy-' + netappNameLower
}

pipeline {
    agent {node {label getAgent("${params.DEPLOYMENT}") == "any" ? "" : getAgent("${params.DEPLOYMENT}")}}
    options {
        timeout(time: 10, unit: 'MINUTES')
        retry(1)
    }

    parameters {
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        string(name: 'BUILD_ID', defaultValue: '', description: 'value to identify each execution')
        choice(name: 'STAGE', choices: ['verification', 'validation', 'certification'])
        string(name: 'HOSTNAME_CAPIF', defaultValue: 'capif.apps.ocp-epg.hi.inet', description: 'Hostname to CAPIF')
        string(name: 'VERSION_CAPIF', defaultValue: '3.0', description: 'Version of CAPIF')
        string(name: 'RELEASE_NAME_CAPIF', defaultValue: 'capif', description: 'Release name Helm to CAPIF')
        string(name: 'HOSTNAME_NEF', defaultValue: 'nef.apps.ocp-epg.hi.inet', description: 'Hostname to NEF')
        string(name: 'RELEASE_NAME_NEF', defaultValue: 'nef', description: 'Release name Helm to NEF')
        string(name: 'HOSTNAME_TSN', defaultValue: 'tsn.apps.ocp-epg.hi.inet', description: 'Hostname to TSN')
        string(name: 'RELEASE_NAME_TSN', defaultValue: 'tsn', description: 'Release name Helm to TSN Frontend')        
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-network-application', description: 'URL of the Github Repository')
        string(name: 'HOSTNAME_NETAPP', defaultValue: 'networkapp.apps.ocp-epg.hi.inet', description: 'Hostname to NetwrokApp')
        string(name: 'RELEASE_NAME_NETAPP', defaultValue: 'netapp-example', description: 'Release name Helm to NetworkApp')
        string(name: 'APP_REPLICAS', defaultValue: '1', description: 'Number of NetworkApp pods to run')
        choice(name: 'DEPLOYMENT', choices: ['kubernetes-athens', 'kubernetes-uma', 'kubernetes-cosmote', 'openshift'])
        booleanParam(name: 'REPORTING', defaultValue: false, description: 'Save report into artifactory')
    }

    environment {
        GIT_BRANCH="${params.GIT_BRANCH}"
        HOSTNAME_CAPIF="${params.HOSTNAME_CAPIF}"
        RELEASE_NAME_CAPIF = "${params.RELEASE_NAME_CAPIF}"
        HOSTNAME_NEF="${params.HOSTNAME_NEF}"
        RELEASE_NAME_NEF = "${params.RELEASE_NAME_NEF}"
        HOSTNAME_TSN="${params.HOSTNAME_TSN}"
        RELEASE_NAME_TSN = "${params.RELEASE_NAME_TSN}"
        GIT_NETAPP_URL = "${params.GIT_NETAPP_URL}"
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        HOSTNAME_NETAPP="${params.HOSTNAME_NETAPP}"
        RELEASE_NAME_NETAPP = "${params.RELEASE_NAME_NETAPP}"
        VERSION="${params.VERSION}"
        DEPLOYMENT = "${params.DEPLOYMENT}"
        REPORT_FILENAME = getReportFilename(NETAPP_NAME_LOWER)
        ARTIFACTORY_URL = "http://artifactory.hi.inet/artifactory/misc-evolved5g/${params.STAGE}"
        ARTIFACTORY_CRED = credentials('artifactory_credentials')
    }

    stages {
        stage ('Log into AWS ECR') {
            when {
                anyOf {
                    expression { DEPLOYMENT == "kubernetes-athens"}
                    expression { DEPLOYMENT == "kubernetes-uma" }
                }
            }
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'evolved5g-pull', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    sh '''
                    kubectl delete secret docker-registry regcred --ignore-not-found --namespace=capif-${BUILD_NUMBER}
                    kubectl create namespace capif-${BUILD_NUMBER}
                    kubectl create secret docker-registry regcred                                   \
                    --docker-password=$(aws ecr get-login-password)                                 \
                    --namespace=capif-${BUILD_NUMBER}                                                     \
                    --docker-server=709233559969.dkr.ecr.eu-central-1.amazonaws.com                 \
                    --docker-username=AWS
                    kubectl delete secret docker-registry regcred --ignore-not-found --namespace=nef-${BUILD_NUMBER}
                    kubectl create namespace nef-${BUILD_NUMBER}
                    kubectl create secret docker-registry regcred                                   \
                    --docker-password=$(aws ecr get-login-password)                                 \
                    --namespace=nef-${BUILD_NUMBER}                                                     \
                    --docker-server=709233559969.dkr.ecr.eu-central-1.amazonaws.com                 \
                    --docker-username=AWS
                    kubectl delete secret docker-registry regcred --ignore-not-found --namespace=tsn-${BUILD_NUMBER}
                    kubectl create namespace tsn-${BUILD_NUMBER}
                    kubectl create secret docker-registry regcred                                   \
                    --docker-password=$(aws ecr get-login-password)                                 \
                    --namespace=tsn-${BUILD_NUMBER}                                                     \
                    --docker-server=709233559969.dkr.ecr.eu-central-1.amazonaws.com                 \
                    --docker-username=AWS
                    kubectl delete secret docker-registry regcred --ignore-not-found --namespace=networt-app-${BUILD_NUMBER}
                    kubectl create namespace network-app-${BUILD_NUMBER}
                    kubectl create secret docker-registry regcred                                   \
                    --docker-password=$(aws ecr get-login-password)                                 \
                    --namespace=network-app-${BUILD_NUMBER}                                                     \
                    --docker-server=709233559969.dkr.ecr.eu-central-1.amazonaws.com                 \
                    --docker-username=AWS
                    '''
                }    
            }    
        }
        stage ('Upgrade app in kubernetes') {
            when {
                anyOf {
                    expression { DEPLOYMENT == "kubernetes-athens" }
                    expression { DEPLOYMENT == "kubernetes-uma" }
                }
            }
            steps {
                dir ("${env.WORKSPACE}") {
                    sh '''#!/bin/bash
                            echo "#### creating temporal folder ${BUILD_NUMBER}.d/ ####"
                            echo "WORKSPACE: $WORKSPACE"
                            mkdir ${BUILD_NUMBER}.d/
                            CREATE_NS=true
                            
                            if [[ $DEPLOYMENT == "kubernetes-athens" ]]; then 
                                CAPIF_HTTP_PORT=30048 
                                CAPIF_HTTPS_PORT=30548 
                            else
                                CAPIF_HTTP_PORT=80
                                CAPIF_HTTPS_PORT=443 
                            fi
                            
                            echo "CAPIF_HTTP_PORT: $CAPIF_HTTP_PORT"
                            echo "CAPIF_HTTPS_PORT: $CAPIF_HTTPS_PORT"

                            echo "#### setting up capif variables ####"
                            
                            LATEST_VERSION=$(grep appVersion: ./cd/helm/capif/Chart.yaml)
                            sed -i -e "s/$LATEST_VERSION/appVersion: '$VERSION_CAPIF'/g" ./cd/helm/capif/Chart.yaml
                            echo "VERSION_CAPIF: $VERSION_CAPIF"

                            jq -n --arg RELEASE_NAME $RELEASE_NAME_CAPIF --arg CHART_NAME capif \
                            --arg NAMESPACE capif-$BUILD_NUMBER --arg HOSTNAME_CAPIF $HOSTNAME_CAPIF \
                            --arg DEPLOYMENT $DEPLOYMENT --arg CREATE_NS $CREATE_NS \
                            -f $WORKSPACE/cd/helm/helmfile.d/00-capif.json \
                            | yq -P > ./${BUILD_NUMBER}.d/00-tmp-capif-${BUILD_NUMBER}.yaml

                            echo "./${BUILD_NUMBER}.d/00-tmp-capif-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/00-tmp-capif-${BUILD_NUMBER}.yaml
                            
                            echo "#### setting up nef variables ####"

                            jq -n --arg RELEASE_NAME $RELEASE_NAME_NEF --arg CHART_NAME nef \
                            --arg NAMESPACE nef-$BUILD_NUMBER --arg HOSTNAME_NEF $HOSTNAME_NEF \
                            --arg HOSTNAME_CAPIF $HOSTNAME_CAPIF --arg CAPIF_HTTP_PORT $CAPIF_HTTP_PORT \
                            --arg CAPIF_HTTPS_PORT $CAPIF_HTTPS_PORT --arg DEPLOYMENT $DEPLOYMENT \
                            --arg CREATE_NS $CREATE_NS --arg DOMAIN_NAME $HOSTNAME_NEF:$CAPIF_HTTPS_PORT \
                            -f $WORKSPACE/cd/helm/helmfile.d/01-nef.json \
                            | yq -P > ./${BUILD_NUMBER}.d/01-tmp-nef-${BUILD_NUMBER}.yaml

                            echo "./${BUILD_NUMBER}.d/01-tmp-nef-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/01-tmp-nef-${BUILD_NUMBER}.yaml

                            echo "#### setting up tsn variables ####"

                            jq -n --arg RELEASE_NAME $RELEASE_NAME_TSN --arg CHART_NAME tsn-frontend \
                            --arg NAMESPACE tsn-$BUILD_NUMBER --arg HOSTNAME_TSN $HOSTNAME_TSN \
                            --arg HOSTNAME_CAPIF $HOSTNAME_CAPIF --arg CAPIF_HTTP_PORT $CAPIF_HTTP_PORT \
                            --arg CAPIF_HTTPS_PORT $CAPIF_HTTPS_PORT --arg DEPLOYMENT $DEPLOYMENT \
                            --arg CREATE_NS $CREATE_NS --arg DOMAIN_NAME $HOSTNAME_TSN:$CAPIF_HTTPS_PORT \
                            -f $WORKSPACE/cd/helm/helmfile.d/02-tsn.json \
                            | yq -P > ./${BUILD_NUMBER}.d/02-tmp-tsn-${BUILD_NUMBER}.yaml
                            
                            echo "./${BUILD_NUMBER}.d/02-tmp-tsn-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/02-tmp-tsn-${BUILD_NUMBER}.yaml

                            echo "#### setting up network-app variables ####"

                            jq -n --arg RELEASE_NAME $RELEASE_NAME_NETAPP --arg CHART_NAME $NETAPP_NAME_LOWER \
                            --arg NAMESPACE network-app-$BUILD_NUMBER --arg FOLDER_NETWORK_APP $NETAPP_NAME_LOWER \
                            --arg HOSTNAME_CAPIF $HOSTNAME_CAPIF --arg CAPIF_HTTP_PORT $CAPIF_HTTP_PORT \
                            --arg CAPIF_HTTPS_PORT $CAPIF_HTTPS_PORT --arg HOSTNAME_NEF $HOSTNAME_NEF \
                            --arg HOSTNAME_NETAPP $HOSTNAME_NETAPP --arg DEPLOYMENT $DEPLOYMENT \
                            --arg STAGE $STAGE --arg APP_REPLICAS $APP_REPLICAS \
                            --arg CREATE_NS $CREATE_NS --arg HOSTNAME_TSN $HOSTNAME_TSN \
                            -f $WORKSPACE/cd/helm/helmfile.d/03-netapp.json \
                            | yq -P > ./${BUILD_NUMBER}.d/03-tmp-network-app-${BUILD_NUMBER}.yaml

                            echo "./${BUILD_NUMBER}.d/03-tmp-network-app-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/03-tmp-network-app-${BUILD_NUMBER}.yaml
                            
                            echo "#### applying helmfile ####"
                            helmfile sync --debug -f ${BUILD_NUMBER}.d/00-tmp-capif-${BUILD_NUMBER}.yaml
                            sleep 30
                            helmfile sync --debug -f ${BUILD_NUMBER}.d/01-tmp-nef-${BUILD_NUMBER}.yaml
                            sleep 30
                            helmfile sync --debug -f ${BUILD_NUMBER}.d/02-tmp-tsn-${BUILD_NUMBER}.yaml
                            sleep 30
                            helmfile sync --debug -f ${BUILD_NUMBER}.d/03-tmp-network-app-${BUILD_NUMBER}.yaml

                            echo "#### getting PKI ####"
                            
                            echo "# dependencies #"
                            sudo apt-get install dateutils -y
                            sudo install /usr/bin/dateutils.ddiff /usr/local/bin/datediff
                            
                            DEBUG="true"
                            NS=network-app-$BUILD_NUMBER
                            TMP_PKI="/tmp/tmp.pki"
                            (
                            echo -e "Pod\tnodeName\tstartTime\tstartedAt"
                            kubectl -n "$NS" get pods -o=jsonpath='{range .items[*]}{.metadata.name}{"\\t"}{.spec.nodeName}{"\\t"}{.status.startTime}{"\\t"}{.status.containerStatuses[0].state.running.startedAt}{"\\n"}{end}'
                            ) | column -t
                            (
                            echo -e "Pod\tnodeName\tstartTime\tstartedAt"
                            kubectl -n "$NS" get pods -o=jsonpath='{range .items[*]}{.metadata.name}{"\\t"}{.spec.nodeName}{"\\t"}{.status.startTime}{"\\t"}{.status.containerStatuses[0].state.running.startedAt}{"\\n"}{end}'
                            ) | column -t > $TMP_PKI

                            if [ $DEBUG == "true" ]; then
                                echo "### startTime ###" 
                                cat $TMP_PKI | awk '{if (NR!=1) {print $3}}'

                                echo "### startAt ###"
                                cat $TMP_PKI | awk '{if (NR!=1) {print $4}}'
                            fi

                            ITEMS=$(cat $TMP_PKI | awk '{if (NR!=1) {print $3}}' | wc -l)
                            LEN=$(($ITEMS +1))

                            if [ $DEBUG == "true" ]; then
                                echo "LENGTH: $LEN"
                            fi

                            x=2

                            while [ $x -le $LEN ]; do
                                if [ $DEBUG == "true" ]; then
                                    echo "### startTime individual ###"
                                fi

                                CMD_STARTIME="cat $TMP_PKI | awk 'NR==$x{if (NR!=1) {print \\$3}}'"
                                DATE_FORMAT_0=$(eval $CMD_STARTIME)

                                if [ $DEBUG == "true" ]; then
                                    echo "$CMD_STARTIME"
                                    echo "$DATE_FORMAT_0"
                                    echo "### startAt individual ###"
                                fi

                                CMD_STARTAT="cat $TMP_PKI | awk 'NR==$x{if (NR!=1) {print \\$4}}'"
                                DATE_FORMAT_1=$(eval $CMD_STARTAT)

                                if [ $DEBUG == "true" ]; then
                                    echo "$CMD_STARTAT"
                                    echo "$DATE_FORMAT_1"
                                    echo "---"
                                    echo "$DATE_FORMAT_1 - $DATE_FORMAT_0 is:"
                                fi

                                KPI=$(datediff $DATE_FORMAT_0 $DATE_FORMAT_1 | awk -F 's' '{print $1}')
                                if [ $DEBUG == "true" ]; then
                                    echo "$KPI"
                                fi

                                if [[ $N_KPI -lt $KPI ]]; then
                                    N_KPI=$KPI
                                fi

                                x=$(($x + 1))
                            done
                                echo "{ \\"deploy_kpi\\" : \\"$N_KPI\\"}"
                                echo "{ \\"deploy_kpi\\" : \\"$N_KPI\\"}" | jq > $REPORT_FILENAME.json
                    '''
                }
            }
        }
        stage ('Upgrade app in Openshift') {
            when {
                allOf {
                    expression { DEPLOYMENT == "openshift"}
                }
            }
            environment {
                TOKEN_NS_CAPIF = credentials("token-os-capif")
                TOKEN_NS_NEF = credentials("openshiftv4-nef")
                TOKEN_NS_TSN = credentials("openshift-evol5-tsn-token")
                TOKEN_NS_NETAPP = credentials("token-evol5-netapp")
            }
            steps {
                dir ("${env.WORKSPACE}") {

                    sh '''#!/bin/bash
                            CREATE_NS=false
                            TMP_NS_CAPIF=evol5-capif
                            TMP_NS_NEF=evol5-nef
                            TMP_NS_TSN=evol5-tsn
                            TMP_NS_NETAPP=evol5-netapp

                            if [[ $DEPLOYMENT == "kubernetes-athens" ]]; then 
                                CAPIF_HTTP_PORT=30048 
                                CAPIF_HTTPS_PORT=30548 
                            else
                                CAPIF_HTTP_PORT=80
                                CAPIF_HTTPS_PORT=443 
                            fi
                            
                            echo "CAPIF_HTTP_PORT: $CAPIF_HTTP_PORT"
                            echo "CAPIF_HTTPS_PORT: $CAPIF_HTTPS_PORT"

                            echo "#### login in AWS ECR ####"

                            oc login --insecure-skip-tls-verify --token=$TOKEN_NS_CAPIF 
                            
                            kubectl delete secret docker-registry regcred --ignore-not-found --namespace=$TMP_NS_CAPIF
                            kubectl create secret docker-registry regcred                                   \
                            --docker-password=$(aws ecr get-login-password)                                 \
                            --namespace=$TMP_NS_CAPIF                                                   \
                            --docker-server=709233559969.dkr.ecr.eu-central-1.amazonaws.com                 \
                            --docker-username=AWS

                            oc login --insecure-skip-tls-verify --token=$TOKEN_NS_NEF
                            kubectl delete secret docker-registry regcred --ignore-not-found --namespace=$TMP_NS_NEF
                            kubectl create secret docker-registry regcred                                   \
                            --docker-password=$(aws ecr get-login-password)                                 \
                            --namespace=$TMP_NS_NEF                                                     \
                            --docker-server=709233559969.dkr.ecr.eu-central-1.amazonaws.com                 \
                            --docker-username=AWS

                            oc login --insecure-skip-tls-verify --token=$TOKEN_NS_TSN
                            kubectl delete secret docker-registry regcred --ignore-not-found --namespace=$TMP_NS_TSN
                            kubectl create secret docker-registry regcred                                   \
                            --docker-password=$(aws ecr get-login-password)                                 \
                            --namespace=$TMP_NS_TSN                                                     \
                            --docker-server=709233559969.dkr.ecr.eu-central-1.amazonaws.com                 \
                            --docker-username=AWS

                            oc login --insecure-skip-tls-verify --token=$TOKEN_NS_NETAPP
                            kubectl delete secret docker-registry regcred --ignore-not-found --namespace=$TMP_NS_NETAPP
                            kubectl create secret docker-registry regcred                                   \
                            --docker-password=$(aws ecr get-login-password)                                 \
                            --namespace=$TMP_NS_NETAPP                                                     \
                            --docker-server=709233559969.dkr.ecr.eu-central-1.amazonaws.com                 \
                            --docker-username=AWS
                            
                            echo "#### creating temporal folder ${BUILD_NUMBER}.d/ ####"
                            mkdir ${BUILD_NUMBER}.d/

                            echo "#### setting up capif variables ####"
                            
                            LATEST_VERSION=$(grep appVersion: ./cd/helm/capif/Chart.yaml)

                            sed -i -e "s/$LATEST_VERSION/appVersion: '$VERSION_CAPIF'/g" ./cd/helm/capif/Chart.yaml
                            
                            jq -n --arg RELEASE_NAME $RELEASE_NAME_CAPIF --arg CHART_NAME capif \
                            --arg NAMESPACE $TMP_NS_CAPIF --arg HOSTNAME_CAPIF $HOSTNAME_CAPIF \
                            --arg DEPLOYMENT $DEPLOYMENT --arg CREATE_NS $CREATE_NS \
                            -f $WORKSPACE/cd/helm/helmfile.d/00-capif.json \
                            | yq -P > ./${BUILD_NUMBER}.d/00-tmp-capif-${BUILD_NUMBER}.yaml

                            echo "./${BUILD_NUMBER}.d/00-tmp-capif-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/00-tmp-capif-${BUILD_NUMBER}.yaml
                            
                            echo "#### setting up nef variables ####"

                            jq -n --arg RELEASE_NAME $RELEASE_NAME_NEF --arg CHART_NAME nef \
                            --arg NAMESPACE $TMP_NS_NEF --arg HOSTNAME_NEF $HOSTNAME_NEF \
                            --arg HOSTNAME_CAPIF $HOSTNAME_CAPIF --arg CAPIF_HTTP_PORT $CAPIF_HTTP_PORT \
                            --arg CAPIF_HTTPS_PORT $CAPIF_HTTPS_PORT --arg CREATE_NS $CREATE_NS \
                            --arg DOMAIN_NAME $HOSTNAME_NEF:$CAPIF_HTTPS_PORT \
                            --arg DEPLOYMENT $DEPLOYMENT -f $WORKSPACE/cd/helm/helmfile.d/01-nef.json \
                            | yq -P > ./${BUILD_NUMBER}.d/01-tmp-nef-${BUILD_NUMBER}.yaml

                            echo "./${BUILD_NUMBER}.d/01-tmp-nef-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/01-tmp-nef-${BUILD_NUMBER}.yaml

                            echo "#### setting up tsn variables ####"
                            
                            jq -n --arg RELEASE_NAME $RELEASE_NAME_TSN --arg CHART_NAME tsn-frontend \
                            --arg NAMESPACE $TMP_NS_TSN --arg HOSTNAME_TSN $HOSTNAME_TSN \
                            --arg HOSTNAME_CAPIF $HOSTNAME_CAPIF --arg CAPIF_HTTP_PORT $CAPIF_HTTP_PORT \
                            --arg CAPIF_HTTPS_PORT $CAPIF_HTTPS_PORT --arg CREATE_NS $CREATE_NS \
                            --arg DOMAIN_NAME $HOSTNAME_TSN:$CAPIF_HTTPS_PORT \
                            --arg DEPLOYMENT $DEPLOYMENT -f $WORKSPACE/cd/helm/helmfile.d/02-tsn.json \
                            | yq -P > ./${BUILD_NUMBER}.d/02-tmp-tsn-${BUILD_NUMBER}.yaml
                            
                            echo "./${BUILD_NUMBER}.d/02-tmp-tsn-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/02-tmp-tsn-${BUILD_NUMBER}.yaml

                            echo "#### setting up network-app variables ####"

                            jq -n --arg RELEASE_NAME $RELEASE_NAME_NETAPP --arg CHART_NAME $NETAPP_NAME_LOWER \
                            --arg NAMESPACE $TMP_NS_NETAPP --arg FOLDER_NETWORK_APP $NETAPP_NAME_LOWER \
                            --arg HOSTNAME_CAPIF $HOSTNAME_CAPIF --arg CAPIF_HTTP_PORT $CAPIF_HTTP_PORT \
                            --arg CAPIF_HTTPS_PORT $CAPIF_HTTPS_PORT --arg HOSTNAME_NEF $HOSTNAME_NEF \
                            --arg HOSTNAME_NETAPP $HOSTNAME_NETAPP --arg DEPLOYMENT $DEPLOYMENT \
                            --arg APP_REPLICAS $APP_REPLICAS --arg CREATE_NS $CREATE_NS \
                            --arg HOSTNAME_TSN $HOSTNAME_TSN \
                            -f $WORKSPACE/cd/helm/helmfile.d/03-netapp.json \
                            | yq -P > ./${BUILD_NUMBER}.d/03-tmp-network-app-${BUILD_NUMBER}.yaml

                            echo "./${BUILD_NUMBER}.d/03-tmp-network-app-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/03-tmp-network-app-${BUILD_NUMBER}.yaml

                            echo "#### applying helmfile ####"
                            
                            oc login --insecure-skip-tls-verify --token=$TOKEN_NS_CAPIF
                            helmfile sync --debug -f ./${BUILD_NUMBER}.d/00-tmp-capif-${BUILD_NUMBER}.yaml

                            oc login --insecure-skip-tls-verify --token=$TOKEN_NS_NEF
                            helmfile sync --debug -f ./${BUILD_NUMBER}.d/01-tmp-nef-${BUILD_NUMBER}.yaml

                            oc login --insecure-skip-tls-verify --token=$TOKEN_NS_TSN
                            helmfile sync --debug -f ./${BUILD_NUMBER}.d/02-tmp-tsn-${BUILD_NUMBER}.yaml

                            oc login --insecure-skip-tls-verify --token=$TOKEN_NS_NETAPP
                            helmfile sync --debug -f ./${BUILD_NUMBER}.d/03-tmp-network-app-${BUILD_NUMBER}.yaml

                            echo "#### getting PKI ####"
                            
                            echo "# dependencies #"
                            sudo apt-get install dateutils -y
                            sudo install /usr/bin/dateutils.ddiff /usr/local/bin/datediff
                            
                            DEBUG="true"
                            NS=network-app-$BUILD_NUMBER
                            TMP_PKI="/tmp/tmp.pki"
                            (
                            echo -e "Pod\tnodeName\tstartTime\tstartedAt"
                            kubectl -n "$NS" get pods -o=jsonpath='{range .items[*]}{.metadata.name}{"\\t"}{.spec.nodeName}{"\\t"}{.status.startTime}{"\\t"}{.status.containerStatuses[0].state.running.startedAt}{"\\n"}{end}'
                            ) | column -t
                            (
                            echo -e "Pod\tnodeName\tstartTime\tstartedAt"
                            kubectl -n "$NS" get pods -o=jsonpath='{range .items[*]}{.metadata.name}{"\\t"}{.spec.nodeName}{"\\t"}{.status.startTime}{"\\t"}{.status.containerStatuses[0].state.running.startedAt}{"\\n"}{end}'
                            ) | column -t > $TMP_PKI

                            if [ $DEBUG == "true" ]; then
                                echo "### startTime ###" 
                                cat $TMP_PKI | awk '{if (NR!=1) {print $3}}'

                                echo "### startAt ###"
                                cat $TMP_PKI | awk '{if (NR!=1) {print $4}}'
                            fi

                            ITEMS=$(cat $TMP_PKI | awk '{if (NR!=1) {print $3}}' | wc -l)
                            LEN=$(($ITEMS +1))

                            if [ $DEBUG == "true" ]; then
                                echo "LENGTH: $LEN"
                            fi

                            x=2

                            while [ $x -le $LEN ]; do
                                if [ $DEBUG == "true" ]; then
                                    echo "### startTime individual ###"
                                fi

                                CMD_STARTIME="cat $TMP_PKI | awk 'NR==$x{if (NR!=1) {print \\$3}}'"
                                DATE_FORMAT_0=$(eval $CMD_STARTIME)

                                if [ $DEBUG == "true" ]; then
                                    echo "$CMD_STARTIME"
                                    echo "$DATE_FORMAT_0"
                                    echo "### startAt individual ###"
                                fi

                                CMD_STARTAT="cat $TMP_PKI | awk 'NR==$x{if (NR!=1) {print \\$4}}'"
                                DATE_FORMAT_1=$(eval $CMD_STARTAT)

                                if [ $DEBUG == "true" ]; then
                                    echo "$CMD_STARTAT"
                                    echo "$DATE_FORMAT_1"
                                    echo "---"
                                    echo "$DATE_FORMAT_1 - $DATE_FORMAT_0 is:"
                                fi

                                KPI=$(datediff $DATE_FORMAT_0 $DATE_FORMAT_1 | awk -F 's' '{print $1}')
                                if [ $DEBUG == "true" ]; then
                                    echo "$KPI"
                                fi

                                if [[ $N_KPI -lt $KPI ]]; then
                                    N_KPI=$KPI
                                fi

                                x=$(($x + 1))
                            done
                                echo "{ \\"deploy_kpi\\" : \\"$N_KPI\\"}"
                                echo "{ \\"deploy_kpi\\" : \\"$N_KPI\\"}" | jq > $REPORT_FILENAME.json
                    '''
                }
            }
        }
    }                
    post {
        always {
            retry(2){
                script{
                    if ("${params.REPORTING}".toBoolean() == true) {
                    sh '''#!/bin/bash
                    if [ -f "${REPORT_FILENAME}.json" ]; then
                            echo "The file $REPORT_FILENAME.json exists."
                        url="$ARTIFACTORY_URL/$NETAPP_NAME/$BUILD_ID/$REPORT_FILENAME.json"

                        curl -v -f -i -X PUT -u $ARTIFACTORY_CRED \
                                        --data-binary @$REPORT_FILENAME.json \
                                        "$url"
                    else
                            echo "No report file generated"
                    fi
                    '''
                    }

                }
            }
        }
        cleanup{
            /* clean up our workspace */
            deleteDir()
            /* clean up tmp directory */
            dir("${env.workspace}@tmp") {
                deleteDir()
            }
            /* clean up script directory */
            dir("${env.workspace}@script") {
                deleteDir()
            }
        }       
    }
}