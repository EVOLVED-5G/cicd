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
        string(name: 'RELEASE_NAME', defaultValue: 'tsn', description: 'Release name')
        choice(name: 'DEPLOYMENT', choices: ['kubernetes-athens', 'kubernetes-uma', 'kubernetes-cosmote', 'openshift'])
    }

    environment {
        GIT_BRANCH="${params.GIT_BRANCH}"
        HOSTNAME="${params.HOSTNAME}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        RELEASE_NAME = "${params.RELEASE_NAME}"
        DEPLOYMENT = "${params.DEPLOYMENT}"

    }

    stages {
        stage ('Destroy/Uninstall app in kubernetes') {
            when {
                anyOf {
                    expression { DEPLOYMENT == "kubernetes-athens" }
                    expression { DEPLOYMENT == "kubernetes-uma" }
                    expression { DEPLOYMENT == "kubernetes-cosmote" }
                }
            }
            steps {
                dir ("${env.WORKSPACE}") {
                    sh '''
                    NAMESPACE=$(helm ls --all-namespaces -f "^$RELEASE_NAME" | awk 'NR==2{print $2}')
                    echo "NAMESPACE=$NAMESPACE"
                    helm uninstall --debug --kubeconfig /home/contint/.kube/config $RELEASE_NAME -n $NAMESPACE --wait
                    kubectl --kubeconfig /home/contint/.kube/config delete ns $NAMESPACE
                    '''
                }
            }
        }
        stage ('Destroy/Uninstall app in Openshift') {
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
                    TMP_NS_TSN=evol5-tsn

                    oc login --insecure-skip-tls-verify --token=$TOKEN_NS_TSN
                    
                    helm uninstall --debug $RELEASE_NAME -n $TMP_NS_TSN --wait
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