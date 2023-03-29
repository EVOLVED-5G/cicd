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

    parameters {
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'RELEASE_NAME_CAPIF', defaultValue: 'capif', description: 'Release name Helm to CAPIF')
        string(name: 'RELEASE_NAME_NEF', defaultValue: 'nef', description: 'Release name Helm to NEF')
        string(name: 'RELEASE_NAME_NETAPP', defaultValue: 'netapp-example', description: 'Release name Helm to NetworkApp')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens", "kubernetes-uma"])  
    }

    environment {
        GIT_BRANCH="${params.GIT_BRANCH}"
        HOSTNAME="${params.HOSTNAME}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        RELEASE_NAME_CAPIF = "${params.RELEASE_NAME_CAPIF}"
        RELEASE_NAME_NEF = "${params.RELEASE_NAME_NEF}"
        RELEASE_NAME_NETAPP = "${params.RELEASE_NAME_NETAPP}"
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
        stage ('Destroy/Uninstall app in kubernetes') {
            when {
                anyOf {
                    expression { DEPLOYMENT == "kubernetes-athens" }
                    expression { DEPLOYMENT == "kubernetes-uma" }
                }
            }
            steps {
                dir ("${env.WORKSPACE}") {
                    sh '''
                    echo "#### uninstall capif ####"
                    NAMESPACE_CAPIF=$(helm ls --all-namespaces -f "^$RELEASE_NAME_CAPIF" | awk 'NR==2{print $2}')
                    echo $NAMESPACE_CAPIF
                    helm uninstall --debug --kubeconfig /home/contint/.kube/config $RELEASE_NAME_CAPIF -n $NAMESPACE_CAPIF --wait
                    kubectl --kubeconfig /home/contint/.kube/config delete ns $NAMESPACE_CAPIF
                    
                    echo "#### uninstall nef ####"
                    NAMESPACE_NEF=$(helm ls --all-namespaces -f "^$RELEASE_NAME_NEF" | awk 'NR==2{print $2}')
                    echo $NAMESPACE_NEF
                    helm uninstall --debug --kubeconfig /home/contint/.kube/config $RELEASE_NAME_NEF -n $NAMESPACE_NEF --wait
                    kubectl --kubeconfig /home/contint/.kube/config delete ns $NAMESPACE_NEF

                    echo "#### uninstall network-app ####"
                    NAMESPACE_NETAPP=$(helm ls --all-namespaces -f "^$RELEASE_NAME_NETAPP" | awk 'NR==2{print $2}')
                    echo $NAMESPACE_NETAPP
                    helm uninstall --debug --kubeconfig /home/contint/.kube/config $RELEASE_NAME_NETAPP -n $NAMESPACE_NETAPP --wait
                    kubectl --kubeconfig /home/contint/.kube/config delete ns $NAMESPACE_NETAPP
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
            steps {
                dir ("${env.WORKSPACE}") {
                    sh '''
                    helm uninstall --debug $RELEASE_NAME -n evol5-$RELEASE_NAME --wait
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