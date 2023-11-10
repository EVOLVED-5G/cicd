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

    parameters {
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        string(name: 'RELEASE_NAME', defaultValue: 'dummy-network-application', description: 'Netapp hostname')
        choice(name: 'DEPLOYMENT', choices: ['openshift', 'kubernetes-athens', 'kubernetes-uma', 'kubernetes-cosmote'])
    }

    environment {
        GIT_BRANCH="${params.GIT_BRANCH}"
        HOSTNAME="${params.HOSTNAME}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        RELEASE_NAME = "${params.RELEASE_NAME}"
        NAMESPACE_NAME = "fogus"
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
                    echo $NAMESPACE
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
                TOKEN_NS_NETAPP = credentials("token-evol5-netapp")
            }
            steps {
                dir ("${env.WORKSPACE}") {
                    sh '''#!/bin/bash
                    TMP_NS_NETAPP=evol5-netapp
                    
                    oc login --insecure-skip-tls-verify --token=$TOKEN_NS_NETAPP 

                    helm uninstall --debug $RELEASE_NAME -n $TMP_NS_NETAPP --wait
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