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
        string(name: 'HOSTNAME_NEF', defaultValue: 'nef.apps.ocp-epg.hi.inet', description: 'Hostname to NEF')
        string(name: 'HOSTNAME_NETAPP', defaultValue: 'fogus.apps.ocp-epg.hi.inet', description: 'Hostname to NetwrokApp')
        string(name: 'APP_REPLICAS', defaultValue: '2', description: 'Number of NetworkApp pods to run')
        string(name: 'FOLDER_NETWORK_APP', defaultValue: 'dummy-network-app', description: 'Folder where the NetworkApp is')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens", "kubernetes-uma"])  
    }

    environment {
        GIT_BRANCH="${params.GIT_BRANCH}"
        HOSTNAME_CAPIF="${params.HOSTNAME_CAPIF}"
        HOSTNAME_NEF="${params.HOSTNAME_NEF}"
        HOSTNAME_NETAPP="${params.HOSTNAME_NETAPP}"
        VERSION="${params.VERSION}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        RELEASE_NAME = "${params.RELEASE_NAME}"
        DEPLOYMENT = "${params.DEPLOYMENT}"
    }

    stages {
        stage ("Login in openshift"){
            when {
                    allOf {
                        expression { DEPLOYMENT == "openshift"}
                    }
                }
            steps {
                withCredentials([string(credentialsId: 'openshiftv4', variable: 'TOKEN')]) {
                    sh '''
                        oc login --insecure-skip-tls-verify --token=$TOKEN 
                    '''
                }
            }
        }
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
                            echo "creating temporal folder ${BUILD_NUMBER}.d/"
                            mkdir ${BUILD_NUMBER}.d/
                            echo "setting up capif variables"
                            LATEST_VERSION=$(grep appVersion: ./cd/helm/capif/Chart.yaml)
                            
                            sed -i -e "s/$LATEST_VERSION/appVersion: '$VERSION_CAPIF'/g" ./cd/helm/capif/Chart.yaml
                            
                            yq -r -j ./cd/helm/helmfile.d/00-capif.yaml \
                            | jq '.releases[].name="capif-${BUILD_NUMBER}"' \
                            | jq '.releases[].namespace="capif-${BUILD_NUMBER}"' \
                            | jq '.releases[].values[1].nginx.nginx.env.capifHostname="$HOSTNAME_CAPIF"' \
                            | yq -P > ./${BUILD_NUMBER}.d/00-tmp-capif-${BUILD_NUMBER}.yaml
                            
                            echo "./${BUILD_NUMBER}.d/00-tmp-capif-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/00-tmp-capif-${BUILD_NUMBER}.yaml
                            
                            echo "setting up nef variables"
                            yq -r -j ./cd/helm/helmfile.d/01-nef.yaml \
                            | jq '.releases[].name="nef-${BUILD_NUMBER}"' \
                            | jq '.releases[].namespace="nef-${BUILD_NUMBER}"' \
                            | jq '.releases[].values[1].backend.ingress.host="$HOSTNAME_NEF"' \
                            | yq -P > ./${BUILD_NUMBER}.d/01-tmp-nef-${BUILD_NUMBER}.yaml
                            
                            echo "./${BUILD_NUMBER}.d/01-tmp-nef-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/01-tmp-nef-${BUILD_NUMBER}.yaml
                            
                            echo "setting up network-app variables"
                            yq -r -j ./cd/helm/helmfile.d/02-netapp.yaml \
                            | jq '.releases[].name="$FOLDER_NETWORK_APP-${BUILD_NUMBER}"' \
                            | jq '.releases[].namespace="network-app-${BUILD_NUMBER}"' \
                            | jq '.releases[].chart="../$FOLDER_NETWORK_APP/"' \
                            | jq '.releases[].values[1].env.fogusHostname="$HOSTNAME_NETAPP"' \
                            | jq '.releases[].values[1].env.environment="$DEPLOYMENT"' \
                            | jq '.releases[].values[2].fe.replicas="$APP_REPLICAS"' \
                            | yq -P > ./${BUILD_NUMBER}.d/02-tmp-network-app-${BUILD_NUMBER}.yaml
                            
                            echo "./${BUILD_NUMBER}.d/02-tmp-network-app-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/02-tmp-network-app-${BUILD_NUMBER}.yaml
                            
                            echo "applying helmfile"
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
            steps {
                dir ("${env.WORKSPACE}") {
                    sh '''
                    helm upgrade --install --debug -n evol5-$RELEASE_NAME \
                    --wait $RELEASE_NAME ./cd/helm/capif/ --set capif_hostname=$HOSTNAME \
                    --set env=$DEPLOYMENT --set version=$VERSION \
                    --atomic
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