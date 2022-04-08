import java.io.*;
String netappName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    return var ;
}


// Function that returns the name of the Netapp container
String trimImage(String filepath) {
    File file = new File(fileName);
    Scanner input = new Scanner(file);
    List<String> list = new ArrayList<String>();

    while (input.hasNextLine()) {
        list.add(input.nextLine());
    }

}

def fileE(String path ) {
    var = """${sh(
                    returnStdout: true,
                    script: "[[ -f ${path} ]] && echo 'True'"
                    )}"""
    return var
}

pipeline {
    agent { node {label 'evol5-openshift'}  }

    parameters {
        string(name: 'VERSION', defaultValue: '1.0', description: '')
        string(name: 'GIT_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
    }

    environment {
        GIT_URL="${params.GIT_URL}"
        GIT_BRANCH="${params.GIT_BRANCH}"
        VERSION="${params.VERSION}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        NETAPP_FOLDER= netappName("${params.GIT_URL}")
        NETAPP_NAME = NETAPP_FOLDER.toLowerCase()
        DOCKER_VAR = 'false'
    }

    stages {
        stage('Get the code!') {
            steps {
                dir ("${env.WORKSPACE}/${NETAPP_NAME}/") {
                    sh '''
                    rm -rf ${NETAPP_NAME} 
                    mkdir ${NETAPP_NAME} 
                    cd ${NETAPP_NAME} 
                    git clone --single-branch --branch evolved5g $GIT_URL .
                    '''
                }
           }
        }
        stage('Docker-compose ?') {
            steps {
                script{
                    env.DOCKER_VAR = fileExists "${env.WORKSPACE}/${NETAPP_NAME}/docker-compose.yml"
                }
                echo "DOCKER VAR is ${DOCKER_VAR}"
                
            }
        }

        stage('Build') {
            when {
                allOf {
                    expression { DOCKER_VAR == "false"}
                }
            }            
            steps {
                dir ("${env.WORKSPACE}/${NETAPP_NAME}/") {
                    sh '''
                    docker build -t evolved-5g/${NETAPP_NAME} .
                    '''
                }
            }
        }
        stage('Modify Docker compose for creating tag images') {
            when {
                allOf {
                    expression { DOCKER_VAR == "True"}
                }
            }  
            steps {
                dir ("${env.WORKSPACE}/${NETAPP_NAME}/") {
                    sh '''
                    pwd > commandResult.txt
                    '''
                    trimImage(commandResult+"docker-compose.yaml")
                }
            }
        }

        stage('Build Docker Compose') {
            when {
                allOf {
                    expression { DOCKER_VAR == "True"}
                }
            }  
            steps {
                dir ("${env.WORKSPACE}/${NETAPP_NAME}/") {
                    sh '''
                    docker-compose up --build -d
                    '''
                }
            }
        }        
        stage('Publish in AWS') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'evolved5g-push', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    dir ("${env.WORKSPACE}/iac/terraform/") {
                        sh '''
                        $(aws ecr get-login --no-include-email)
                        docker image tag evolved-5g/${NETAPP_NAME} 709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:${NETAPP_NAME}-${VERSION}.${BUILD_NUMBER}
                        docker image tag evolved-5g/${NETAPP_NAME} 709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:${NETAPP_NAME}-latest
                        docker image push 709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:${NETAPP_NAME}-latest
                        '''
                    }    
                }   
            }
        }
        stage('Publish in Artefactory') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    dir ("${env.WORKSPACE}/dummyapp/") {
                        sh '''
                        docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet
                        docker image tag evolved-5g/${NETAPP_NAME} dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}:${VERSION}.${BUILD_NUMBER}
                        docker image tag evolved-5g/${NETAPP_NAME} dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}:latest
                        docker image push --all-tags dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}
                        '''
                    }
                }
            }
        }
        stage('Cleaning docker images and containers') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    dir ("${env.WORKSPACE}/dummyapp/") {
                        sh '''
                        docker rmi -f $(docker images -a -q)
                        '''
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

