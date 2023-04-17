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
        retry(2)
    }
    parameters {
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        string(name: 'HOSTNAME_CAPIF', defaultValue: 'capif.apps.ocp-epg.hi.inet', description: 'Hostname to CAPIF')
        string(name: 'HOSTNAME_NEF', defaultValue: 'nef.apps.ocp-epg.hi.inet', description: 'Hostname to NEF')
        string(name: 'HOSTNAME_NETAPP', defaultValue: 'fogus.apps.ocp-epg.hi.inet', description: 'Hostname to NetwrokApp')
        string(name: 'RELEASE_NAME_NETAPP', defaultValue: 'netapp-example', description: 'Release name Helm to NetworkApp')
        string(name: 'APP_REPLICAS', defaultValue: '2', description: 'Number of NetworkApp pods to run')
        string(name: 'FOLDER_NETWORK_APP', defaultValue: 'dummy-network-app', description: 'Folder where the NetworkApp is')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens", "kubernetes-uma"])  
    }

    environment {
        GIT_BRANCH="${params.GIT_CICD_BRANCH}"
        HOSTNAME_CAPIF="${params.HOSTNAME_CAPIF}"
        HOSTNAME_NEF="${params.HOSTNAME_NEF}"
        HOSTNAME_NETAPP="${params.HOSTNAME_NETAPP}"
        RELEASE_NAME_NETAPP = "${params.RELEASE_NAME_NETAPP}"
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
                            helmfile sync --debug -f ${BUILD_NUMBER}.d/02-tmp-network-app-${BUILD_NUMBER}.yaml
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
                TOKEN_NS_NETAPP = credentials("token-evol5-netapp")
            }
            steps {
                dir ("${env.WORKSPACE}") {
                    sh '''#!/bin/bash
                            CREATE_NS=false
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

                            oc login --insecure-skip-tls-verify --token=$TOKEN_NS_NETAPP
                            kubectl delete secret docker-registry regcred --ignore-not-found --namespace=$TMP_NS_NETAPP
                            kubectl create secret docker-registry regcred                                   \
                            --docker-password=$(aws ecr get-login-password)                                 \
                            --namespace=$TMP_NS_NETAPP                                                     \
                            --docker-server=709233559969.dkr.ecr.eu-central-1.amazonaws.com                 \
                            --docker-username=AWS

                            echo "#### creating temporal folder ${BUILD_NUMBER}.d/ ####"
                            mkdir ${BUILD_NUMBER}.d/

                            echo "#### setting up network-app variables ####"

                            cat ./cd/helm/helmfile.d/02-netapp.json

                            jq -n --arg RELEASE_NAME $RELEASE_NAME_NETAPP --arg CHART_NAME fogus \
                            --arg NAMESPACE $TMP_NS_NETAPP --arg FOLDER_NETWORK_APP $FOLDER_NETWORK_APP \
                            --arg HOSTNAME_CAPIF $HOSTNAME_CAPIF --arg CAPIF_HTTP_PORT $CAPIF_HTTP_PORT \
                            --arg CAPIF_HTTPS_PORT $CAPIF_HTTPS_PORT --arg HOSTNAME_NEF $HOSTNAME_NEF \
                            --arg HOSTNAME_NETAPP $HOSTNAME_NETAPP --arg DEPLOYMENT $DEPLOYMENT \
                            --arg APP_REPLICAS $APP_REPLICAS --arg CREATE_NS $CREATE_NS \
                            -f ./cd/helm/helmfile.d/02-netapp.json \
                            | yq -P > ./${BUILD_NUMBER}.d/02-tmp-network-app-${BUILD_NUMBER}.yaml

                            echo "./${BUILD_NUMBER}.d/02-tmp-network-app-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/02-tmp-network-app-${BUILD_NUMBER}.yaml

                            echo "#### applying helmfile ####"
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