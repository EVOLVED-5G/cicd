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
        timeout(time: 35, unit: 'MINUTES')
        retry(1)
    }

    parameters {
        string(name: 'VERSION', defaultValue: '3.0', description: '')
        string(name: 'GIT_CAPIF_URL', defaultValue: 'https://github.com/EVOLVED-5G/CAPIF_API_Services', description: 'URL of the Github Repository')
        string(name: 'GIT_CAPIF_BRANCH', defaultValue: 'develop', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        choice(name: 'DEPLOYMENT', choices: ['openshift', 'kubernetes-athens', 'kubernetes-uma', 'kubernetes-cosmote'])
    }

    environment {
        GIT_NETAPP_URL="${params.GIT_NETAPP_URL}"
        GIT_CICD_BRANCH="${params.GIT_CICD_BRANCH}"
        GIT_NETAPP_BRANCH="${params.GIT_NETAPP_BRANCH}"
        VERSION="${params.VERSION}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        AWS_ACCOUNT_ID = credentials('AWS_ACCOUNT_NUMBER')
        NETAPP_NAME = "capif"
        DOCKER_VAR = false
        CAPIF_SERVICES_DIRECTORY = "${WORKSPACE}/services"
    }
    stages {
        stage('Clean workspace') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    sh '''
                    docker ps -a -q | xargs --no-run-if-empty docker stop $(docker ps -a -q)
                    docker system prune -a -f --volumes
                    sudo rm -rf $WORKSPACE/$NETAPP_NAME/
                    '''
                }
            }
        }
        stage('Get the code!') {
            steps {
                dir ("${env.WORKSPACE}/") {
                    sh '''
                    rm -rf $NETAPP_NAME 
                    mkdir $NETAPP_NAME 
                    cd $NETAPP_NAME
                    git clone --single-branch --branch $GIT_CAPIF_BRANCH $GIT_CAPIF_URL .
                    '''
                }
           }
        }
        stage('Build CAPIF') { 
            steps {
                dir ("${env.WORKSPACE}/${NETAPP_NAME}/services") {
                    sh '''
                    docker-compose up --build --force-recreate -d
                    '''
                }
            }
        }
        stage('Modify image name and publish in AWS') { 
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'evolved5g-push', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {               
                    script {    
                        def cmd = "docker ps --format '{{.Image}}'"
                        def cmd2 = "docker ps --format '{{.Names}}'"
                        def image = sh(returnStdout: true, script: cmd).trim()
                        def name  = sh(returnStdout: true, script: cmd2).trim()
                        sh '''$(aws ecr get-login --no-include-email)'''
                        [image.tokenize(), name.tokenize()].transpose().each { x ->
                            sh """ docker tag ${x[0]} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-${x[1]}-${VERSION}"""
                            sh """ docker tag ${x[0]} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-${x[1]}-latest"""
                            sh """ docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-${x[1]}-${VERSION}"""
                            sh """ docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-${x[1]}-latest"""
                        }
                    }
                }
            }
        }  
        stage('Publish in Artifacotry') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    dir ("${env.WORKSPACE}/${NETAPP_NAME}/services") {
                        script{
                            sh ''' docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet'''
                            def cmd = "docker ps --format '{{.Image}}'"
                            def cmd2 = "docker ps --format '{{.Names}}'"
                            def image = sh(returnStdout: true, script: cmd).trim()
                            def name  = sh(returnStdout: true, script: cmd2).trim()
                        [image.tokenize(), name.tokenize()].transpose().each { x ->
                            sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/capif/"${x[1]}":${VERSION}"""
                            sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/capif/"${x[1]}":latest"""
                            sh """ docker image push --all-tags dockerhub.hi.inet/evolved-5g/capif/"${x[1]}" """
                            }
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
            docker system prune -a -f --volumes
            sudo rm -rf $WORKSPACE/$NETAPP_NAME/
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