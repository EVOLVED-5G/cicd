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
    if("openshift".equals(var)) {
        return "evol5-openshift";
    }else if("kubernetes-athens".equals(var)){
        return "evol5-athens"
    }else {
        return "evol5-slave";
    }
}

pipeline {
    agent {node {label getAgent("${params.DEPLOYMENT}") == "any" ? "" : getAgent("${params.DEPLOYMENT}")}}
    options {
        timeout(time: 10, unit: 'MINUTES')
        retry(1)
    }

    parameters {
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'HOSTNAME_CAPIF', defaultValue: 'capif.apps.ocp-epg.hi.inet', description: 'Hostname to CAPIF')
        string(name: 'VERSION_CAPIF', defaultValue: '3.0', description: 'Version of CAPIF')
        string(name: 'RELEASE_NAME_CAPIF', defaultValue: 'capif', description: 'Release name Helm to CAPIF')
        string(name: 'HOSTNAME_NEF', defaultValue: 'nef.apps.ocp-epg.hi.inet', description: 'Hostname to NEF')
        string(name: 'RELEASE_NAME_NEF', defaultValue: 'nef', description: 'Release name Helm to NEF')
        string(name: 'HOSTNAME_TSN', defaultValue: 'tsn.apps.ocp-epg.hi.inet', description: 'Hostname to TSN')
        string(name: 'RELEASE_NAME_TSN', defaultValue: 'tsn', description: 'Release name Helm to TSN Frontend')        
        string(name: 'HOSTNAME_NETAPP', defaultValue: 'fogus.apps.ocp-epg.hi.inet', description: 'Hostname to NetwrokApp')
        string(name: 'RELEASE_NAME_NETAPP', defaultValue: 'netapp-example', description: 'Release name Helm to NetworkApp')
        string(name: 'APP_REPLICAS', defaultValue: '2', description: 'Number of NetworkApp pods to run')
        string(name: 'FOLDER_NETWORK_APP', defaultValue: 'dummy-network-app', description: 'Folder where the NetworkApp is')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens", "kubernetes-uma"])  
    }

    environment {
        GIT_BRANCH="${params.GIT_BRANCH}"
        HOSTNAME_CAPIF="${params.HOSTNAME_CAPIF}"
        RELEASE_NAME_CAPIF = "${params.RELEASE_NAME_CAPIF}"
        HOSTNAME_NEF="${params.HOSTNAME_NEF}"
        RELEASE_NAME_NEF = "${params.RELEASE_NAME_NEF}"
        HOSTNAME_TSN="${params.HOSTNAME_TSN}"
        RELEASE_NAME_TSN = "${params.RELEASE_NAME_TSN}"
        HOSTNAME_NETAPP="${params.HOSTNAME_NETAPP}"
        RELEASE_NAME_NETAPP = "${params.RELEASE_NAME_NETAPP}"
        VERSION="${params.VERSION}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        DEPLOYMENT = "${params.DEPLOYMENT}"
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
                            --arg CREATE_NS $CREATE_NS -f $WORKSPACE/cd/helm/helmfile.d/01-nef.json \
                            | yq -P > ./${BUILD_NUMBER}.d/01-tmp-nef-${BUILD_NUMBER}.yaml

                            echo "./${BUILD_NUMBER}.d/01-tmp-nef-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/01-tmp-nef-${BUILD_NUMBER}.yaml

                            echo "#### setting up tsn variables ####"

                            jq -n --arg RELEASE_NAME $RELEASE_NAME_TSN --arg CHART_NAME tsn-frontend \
                            --arg NAMESPACE tsn-$BUILD_NUMBER --arg HOSTNAME_TSN $HOSTNAME_TSN \
                            --arg HOSTNAME_CAPIF $HOSTNAME_CAPIF --arg CAPIF_HTTP_PORT $CAPIF_HTTP_PORT \
                            --arg CAPIF_HTTPS_PORT $CAPIF_HTTPS_PORT --arg DEPLOYMENT $DEPLOYMENT \
                            --arg CREATE_NS $CREATE_NS -f $WORKSPACE/cd/helm/helmfile.d/03-tsn.json \
                            | yq -P > ./${BUILD_NUMBER}.d/03-tmp-tsn-${BUILD_NUMBER}.yaml

                            echo "./${BUILD_NUMBER}.d/03-tmp-tsn-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/03-tmp-tsn-${BUILD_NUMBER}.yaml

                            echo "#### setting up network-app variables ####"

                            jq -n --arg RELEASE_NAME $RELEASE_NAME_NETAPP --arg CHART_NAME fogus \
                            --arg NAMESPACE network-app-$BUILD_NUMBER --arg FOLDER_NETWORK_APP $FOLDER_NETWORK_APP \
                            --arg HOSTNAME_CAPIF $HOSTNAME_CAPIF --arg CAPIF_HTTP_PORT $CAPIF_HTTP_PORT \
                            --arg CAPIF_HTTPS_PORT $CAPIF_HTTPS_PORT --arg HOSTNAME_NEF $HOSTNAME_NEF \
                            --arg HOSTNAME_NETAPP $HOSTNAME_NETAPP --arg DEPLOYMENT $DEPLOYMENT \
                            --arg APP_REPLICAS $APP_REPLICAS --arg CREATE_NS $CREATE_NS \
                            -f $WORKSPACE/cd/helm/helmfile.d/02-netapp.json \
                            | yq -P > ./${BUILD_NUMBER}.d/02-tmp-network-app-${BUILD_NUMBER}.yaml

                            echo "./${BUILD_NUMBER}.d/02-tmp-network-app-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/02-tmp-network-app-${BUILD_NUMBER}.yaml
                            
                            echo "#### applying helmfile ####"
                            helmfile sync --debug -f ${BUILD_NUMBER}.d/
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
                            --arg DEPLOYMENT $DEPLOYMENT -f $WORKSPACE/cd/helm/helmfile.d/01-nef.json \
                            | yq -P > ./${BUILD_NUMBER}.d/01-tmp-nef-${BUILD_NUMBER}.yaml

                            echo "./${BUILD_NUMBER}.d/01-tmp-nef-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/01-tmp-nef-${BUILD_NUMBER}.yaml

                            echo "#### setting up tsn variables ####"

                            jq -n --arg RELEASE_NAME $RELEASE_NAME_TSN --arg CHART_NAME tsn-frontend \
                            --arg NAMESPACE $TMP_NS_TSN --arg HOSTNAME_TSN $HOSTNAME_TSN \
                            --arg HOSTNAME_CAPIF $HOSTNAME_CAPIF --arg CAPIF_HTTP_PORT $CAPIF_HTTP_PORT \
                            --arg CAPIF_HTTPS_PORT $CAPIF_HTTPS_PORT --arg CREATE_NS $CREATE_NS \
                            --arg DEPLOYMENT $DEPLOYMENT -f $WORKSPACE/cd/helm/helmfile.d/03-tsn.json \
                            | yq -P > ./${BUILD_NUMBER}.d/03-tmp-tsn-${BUILD_NUMBER}.yaml

                            echo "./${BUILD_NUMBER}.d/03-tmp-tsn-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/03-tmp-tsn-${BUILD_NUMBER}.yaml

                            echo "#### setting up network-app variables ####"

                            jq -n --arg RELEASE_NAME $RELEASE_NAME_NETAPP --arg CHART_NAME fogus \
                            --arg NAMESPACE $TMP_NS_NETAPP --arg FOLDER_NETWORK_APP $FOLDER_NETWORK_APP \
                            --arg HOSTNAME_CAPIF $HOSTNAME_CAPIF --arg CAPIF_HTTP_PORT $CAPIF_HTTP_PORT \
                            --arg CAPIF_HTTPS_PORT $CAPIF_HTTPS_PORT --arg HOSTNAME_NEF $HOSTNAME_NEF \
                            --arg HOSTNAME_NETAPP $HOSTNAME_NETAPP --arg DEPLOYMENT $DEPLOYMENT \
                            --arg APP_REPLICAS $APP_REPLICAS --arg CREATE_NS $CREATE_NS \
                            -f $WORKSPACE/cd/helm/helmfile.d/02-netapp.json \
                            | yq -P > ./${BUILD_NUMBER}.d/02-tmp-network-app-${BUILD_NUMBER}.yaml

                            echo "./${BUILD_NUMBER}.d/02-tmp-network-app-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/02-tmp-network-app-${BUILD_NUMBER}.yaml

                            echo "#### applying helmfile ####"
                            
                            oc login --insecure-skip-tls-verify --token=$TOKEN_NS_CAPIF
                            helmfile sync --debug -f ./${BUILD_NUMBER}.d/00-tmp-capif-${BUILD_NUMBER}.yaml

                            oc login --insecure-skip-tls-verify --token=$TOKEN_NS_NEF
                            helmfile sync --debug -f ./${BUILD_NUMBER}.d/01-tmp-nef-${BUILD_NUMBER}.yaml

                            oc login --insecure-skip-tls-verify --token=$TOKEN_NS_TSN
                            helmfile sync --debug -f ./${BUILD_NUMBER}.d/03-tmp-tsn-${BUILD_NUMBER}.yaml

                            oc login --insecure-skip-tls-verify --token=$TOKEN_NS_NETAPP
                            helmfile sync --debug -f ./${BUILD_NUMBER}.d/02-tmp-network-app-${BUILD_NUMBER}.yaml
                    '''
                }
            }
        }
    }                
    post {
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