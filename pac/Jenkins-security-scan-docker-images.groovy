String netappName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    return var ;
}

pipeline {
    agent { node {label 'evol5-openshift'}  }

    parameters {
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
    }

    environment {
        GIT_NETAPP_URL="${params.GIT_NETAPP_URL}"
        GIT_CICD_BRANCH="${params.GIT_CICD_BRANCH}"
        GIT_NETAPP_BRANCH="${params.GIT_NETAPP_BRANCH}"
        PASSWORD_ARTIFACTORY= credentials("artifactory_credentials")
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}").toLowerCase()
    }

    stages {
        stage('Get Repo and clone'){
            steps {
                dir ("${env.WORKSPACE}/") {
                    sh '''
                    git clone -single-branch --branch $GIT_NETAPP_BRANCH $GIT_NETAPP_URL .
                    git pull .
                    shopt -s extglob
                    rm -rf !(.git/)
                    git clone --single-branch --branch $GIT_NETAPP_BRANCH $GIT_NETAPP_URL .
                    git add .
                    git commit -m "Adding repo to Telefonica Project"
                    git push -u origin evolved5g
                    '''
                }
           }
        }
        stage('Launch Github Actions command') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'evolved5g-push', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {               
                    script {    
                        def images = "curl 'http://artifactory.hi.inet/ui/api/v1/ui/nativeBrowser/docker/evolved-5g/' \
                                            -u 'contint:${PASSWORD_ARTIFACTORY}' \
                                            -H 'Accept: application/json, text/plain, */*' \
                                            -k | jq '.children[].name' | grep 'fogus.*'"
                        def image = sh(returnStdout: true, script: images).trim()


                        image.tokenize().each { x ->
                            sh """  curl  -H "Content-Type: application/json"   -X POST "http://epg-trivy.hi.inet:5000/scan-image?token=fb1d3b71-2c1e-49cb-b04b-54534534ef0a&image=${x}&update_wiki=true&repository=Telefonica/${NETAPP_NAME}&branch=${GIT_NETAPP_BRANCH}&output_format=md" """
                            sh """  curl  -H "Content-Type: application/json"   -X POST "http://epg-trivy.hi.inet:5000/scan-image?token=fb1d3b71-2c1e-49cb-b04b-54534534ef0a&image=${x}&update_wiki=true&repository=Telefonica/${NETAPP_NAME}&branch=${GIT_NETAPP_BRANCH}&output_format=md" > report-tr-img-${NETAPP_NAME}.json """
                        }
                    }
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

