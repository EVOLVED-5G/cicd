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
        timeout(time: 35, unit: 'MINUTES')
        retry(1)
    }

    parameters {
        string(name: 'VERSION', defaultValue: '0.0.1', description: '')
        string(name: 'GIT_TSN_URL', defaultValue: 'https://github.com/EVOLVED-5G/TSN_FrontEnd', description: 'URL of the Github Repository')
        string(name: 'GIT_TSN_BRANCH', defaultValue: 'main', description: 'Branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens", "kubernetes-uma"])
    }

    environment {
        GIT_CICD_BRANCH="${params.GIT_CICD_BRANCH}"
        VERSION="${params.VERSION}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        AWS_ACCOUNT_ID = credentials('AWS_ACCOUNT_NUMBER')
        AWS_REGION = "eu-central-1"
        TSN_NAME = "tns-frontend"
    }
    stages {
        stage('Clean workspace') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    sh '''
                    docker ps -a -q | xargs --no-run-if-empty docker stop $(docker ps -a -q)
                    docker system prune -a --force --volumes
                    sudo rm -rf $WORKSPACE/$TSN_NAME/
                    '''
                }
            }
        }
        stage('Get the code!') {
            steps {
                dir ("${env.WORKSPACE}/") {
                    sh '''
                    rm -rf $TSN_NAME 
                    mkdir $TSN_NAME 
                    cd $TSN_NAME
                    git clone --single-branch --branch $GIT_TSN_BRANCH $GIT_TSN_URL .
                    '''
                }
           }
        }
        stage('Build TSN FrontEnd') { 
            steps {
                dir ("${env.WORKSPACE}/${TSN_NAME}/") {
                    sh '''
                    echo "### preparing Dockerfile ###"
                    echo "Checking for 'jq' availability..."
                    jq --version
                    
                    if [ $? -ne 0 ]; then                   
	                    echo "'jq' command is not available. Please install and re-run this script."                    
	                    echo "See 'https://stedolan.github.io/jq/download/' for more information."                  
	                    exit
                    fi                  

                    port=$(jq .FrontEnd.Port config.json)                   

                    echo ""
                    echo "Generating Dockerfile (FrontEnd Port in config.json is $port)..."                 

                    cp Dockerfile.Template Dockerfile
                    sed -i "s/{{PORT}}/$port/" Dockerfile                   

                    echo ""
                    echo "Building docker image..."                 

                    docker build -t $TSN_NAME .                  

                    echo " ### build has finished ###"
                    '''
                }
            }
        }
        stage('Getting image name and publishing in AWS') {
            steps {
                script { 
                    sh '''
                        echo "### Signing in AWS ECR ###"
                        aws ecr get-login-password --region $AWS_REGION \
                        | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
                        
                        echo "### tagging image latest, $VERSION ###"
                        IMAGE=$(docker image ls $TSN_NAME --format "{{ .Repository }}")
                        docker tag $IMAGE $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/evolved5g:$TSN_NAME-latest
                        docker tag $IMAGE $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/evolved5g:$TSN_NAME-$VERSION

                        echo "### pushing image to (latest, $VERSION) ###"
                        docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/evolved5g:$TSN_NAME-$VERSION
                        docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/evolved5g:$TSN_NAME-latest


                    '''
                    }                    
            }
        }  
        stage('Publish in Artifacotry') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    dir ("${env.WORKSPACE}/${TSN_NAME}/services") {
                        script{
                            sh ''' docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet'''
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            sh '''
            docker ps -a -q | xargs --no-run-if-empty docker stop $(docker ps -a -q)
            docker system prune -a --force
            docker system prune -a --force --volumes
            sudo rm -rf $WORKSPACE/$TSN_NAME/
            '''
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