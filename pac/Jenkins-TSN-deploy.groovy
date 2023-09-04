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

pipeline {
    agent {node {label getAgent("${params.DEPLOYMENT}") == "any" ? "" : getAgent("${params.DEPLOYMENT}")}}
    options {
        timeout(time: 10, unit: 'MINUTES')
        retry(1)
    }

    parameters {
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        string(name: 'HOSTNAME_CAPIF', defaultValue: 'capif.apps.ocp-epg.hi.inet', description: 'Hostname to CAPIF')
        string(name: 'HOSTNAME_TSN', defaultValue: 'tsn.apps.ocp-epg.hi.inet', description: 'Hostname to TSN')
        string(name: 'RELEASE_NAME_TSN', defaultValue: 'tsn', description: 'Release name to TSN')
        choice(name: 'DEPLOYMENT', choices: ['kubernetes-athens', 'kubernetes-uma', 'kubernetes-cosmote', 'openshift'])
    }

    environment {
        GIT_BRANCH="${params.GIT_BRANCH}"
        HOSTNAME_CAPIF="${params.HOSTNAME_CAPIF}"
        HOSTNAME_TSN="${params.HOSTNAME_TSN}"
        RELEASE_NAME_TSN = "${params.RELEASE_NAME_TSN}"
        DEPLOYMENT = "${params.DEPLOYMENT}"

    }

    stages {        
        stage ('Log into AWS ECR') {
            when {
                anyOf {
                    expression { DEPLOYMENT == "kubernetes-athens" }
                    expression { DEPLOYMENT == "kubernetes-uma" }
                }
            }
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'evolved5g-pull', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    sh '''
                    kubectl delete secret docker-registry regcred --ignore-not-found --namespace=tsn-${BUILD_NUMBER}
                    kubectl create namespace tsn-${BUILD_NUMBER}
                    kubectl create secret docker-registry regcred                                   \
                    --docker-password=$(aws ecr get-login-password)                                 \
                    --namespace=tsn-${BUILD_NUMBER}                                                     \
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

                            echo "#### setting up tsn variables ####"

                            jq -n --arg RELEASE_NAME $RELEASE_NAME_TSN --arg CHART_NAME tsn-frontend \
                            --arg NAMESPACE tsn-$BUILD_NUMBER --arg HOSTNAME_TSN $HOSTNAME_TSN \
                            --arg HOSTNAME_CAPIF $HOSTNAME_CAPIF --arg CAPIF_HTTP_PORT $CAPIF_HTTP_PORT \
                            --arg CAPIF_HTTPS_PORT $CAPIF_HTTPS_PORT --arg DEPLOYMENT $DEPLOYMENT \
                            --arg CREATE_NS $CREATE_NS -f $WORKSPACE/cd/helm/helmfile.d/03-tsn.json \
                            | yq -P > ./${BUILD_NUMBER}.d/03-tmp-tsn-${BUILD_NUMBER}.yaml

                            echo "./${BUILD_NUMBER}.d/03-tmp-tsn-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/03-tmp-tsn-${BUILD_NUMBER}.yaml

                            echo "#### applying helmfile ####"
                            helmfile sync --debug -f ${BUILD_NUMBER}.d/03-tmp-tsn-${BUILD_NUMBER}.yaml

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
                TOKEN_NS_TSN = credentials("openshift-evol5-tsn-token")
            }
            steps {
                dir ("${env.WORKSPACE}") {
                    sh '''#!/bin/bash
                            CREATE_NS=false
                            TMP_NS_TSN=evol5-tsn

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

                            oc login --insecure-skip-tls-verify --token=$TOKEN_NS_TSN
                            kubectl delete secret docker-registry regcred --ignore-not-found --namespace=$TMP_NS_TSN
                            kubectl create secret docker-registry regcred                                   \
                            --docker-password=$(aws ecr get-login-password)                                 \
                            --namespace=$TMP_NS_TSN                                                     \
                            --docker-server=709233559969.dkr.ecr.eu-central-1.amazonaws.com                 \
                            --docker-username=AWS

                            echo "#### creating temporal folder ${BUILD_NUMBER}.d/ ####"
                            mkdir ${BUILD_NUMBER}.d/

                            echo "#### setting up tsn-frontend variables ####"

                            jq -n --arg RELEASE_NAME $RELEASE_NAME_TSN --arg CHART_NAME tsn-frontend \
                            --arg NAMESPACE $TMP_NS_TSN --arg HOSTNAME_TSN $HOSTNAME_TSN \
                            --arg HOSTNAME_CAPIF $HOSTNAME_CAPIF --arg CAPIF_HTTP_PORT $CAPIF_HTTP_PORT \
                            --arg CAPIF_HTTPS_PORT $CAPIF_HTTPS_PORT --arg CREATE_NS $CREATE_NS \
                            --arg DEPLOYMENT $DEPLOYMENT -f $WORKSPACE/cd/helm/helmfile.d/03-tsn.json \
                            | yq -P > ./${BUILD_NUMBER}.d/03-tmp-tsn-${BUILD_NUMBER}.yaml

                            echo "./${BUILD_NUMBER}.d/03-tmp-tsn-${BUILD_NUMBER}.yaml"
                            cat ./${BUILD_NUMBER}.d/03-tmp-tsn-${BUILD_NUMBER}.yaml

                            echo "#### applying helmfile ####"

                            oc login --insecure-skip-tls-verify --token=$TOKEN_NS_TSN
                            helmfile sync --debug -f ./${BUILD_NUMBER}.d/03-tmp-tsn-${BUILD_NUMBER}.yaml

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