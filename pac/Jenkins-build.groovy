String netappName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    return var ;
}

String getPathAWS(deployment) {
    String var = deployment
    if("verification".equals(var)) {
        return "";
    }else if("validation".equals(var)){
        return "validation";
    }else {
        return "certification";
    }
}

String getPath(deployment) {
    String var = deployment
    if("verification".equals(var)) {
        return "";
    }else if("validation".equals(var)){
        return "validation/";
    }else {
        return "certification/";
    }
}

pipeline {
    agent { node {label 'evol5-openshift'}  }

    parameters {
        string(name: 'VERSION', defaultValue: '1.0', description: '')
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        choice(name: 'STAGE', choices: ["verification", "validation", "certification"])
    }

    environment {
        GIT_NETAPP_URL="${params.GIT_NETAPP_URL}"
        GIT_CICD_BRANCH="${params.GIT_CICD_BRANCH}"
        GIT_NETAPP_BRANCH="${params.GIT_NETAPP_BRANCH}"
        VERSION="${params.VERSION}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        AWS_ACCOUNT_ID = credentials('AWS_ACCOUNT_NUMBER')
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}").toLowerCase()
        DOCKER_VAR = false
        PATH_DOCKER = getPath("${params.STAGE}")
        PATH_AWS = getPathAWS("${params.STAGE}")
        CHECKPORTS_PATH = 'utils/checkports'
        ARTIFACTORY_CRED=credentials('artifactory_credentials')
    }
    stages {
        stage('Clean workspace') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    sh '''
                    docker ps -a -q | xargs --no-run-if-empty docker stop $(docker ps -a -q)
                    docker system prune -a -f --volumes
                    sudo rm -rf $WORKSPACE/$NETAPP_NAME/
                    docker network create services_default
                    echo $PATH_AWS
                    '''
                }
            }
        }
        stage('Get the code!') {
            options {
                    timeout(time: 10, unit: 'MINUTES')
                    retry(1)
                }
            steps {
                dir ("${env.WORKSPACE}/") {
                    sh '''
                    rm -rf $NETAPP_NAME
                    mkdir $NETAPP_NAME
                    cd $NETAPP_NAME
                    git clone --single-branch --branch $GIT_NETAPP_BRANCH $GIT_NETAPP_URL .
                    '''
                }
           }
        }
        stage('Check if there is a docker-compose in the repository') {
            steps {
                script{
                    DOCKER_VAR = fileExists "${env.WORKSPACE}/${NETAPP_NAME}/docker-compose.yml"
                }
                echo "env DOCKER VAR is ${DOCKER_VAR}"

            }
        }
        //NICE TO HAVE: Makefile to encapsulate docker and docker-compose commands
        stage('Build') {
            when {
                expression {
                    return !"${DOCKER_VAR}".toBoolean()
                }
            }
            steps {
                dir ("${env.WORKSPACE}/${NETAPP_NAME}/") {
                    sh '''
                    docker build -t ${NETAPP_NAME} .
                    docker run -d -P ${NETAPP_NAME}
                    '''
                }
            }
        }
        stage('Build Docker Compose') {
            when {
                expression {
                    return "${DOCKER_VAR}".toBoolean()
                }
            }
            steps {
                dir ("${env.WORKSPACE}/${NETAPP_NAME}/") {
                    sh '''
                    docker network create demo-network
                    make run-dev || true
                    docker-compose up --build --force-recreate -d
                    '''
                }
            }
        }
        //Check Ports on running images
        stage('Check Ports of images generated') {
            steps {
                dir ("${env.WORKSPACE}/") {
                    sh '''
                    pip install -r ${CHECKPORTS_PATH}/requirements.txt
                    python3 ${CHECKPORTS_PATH}/checkportscicd.py $GIT_NETAPP_BRANCH $GIT_NETAPP_URL ${NETAPP_NAME}
                    '''
                }
            }
        }
        //----
        stage('Modify image name and upload to AWS') {
            when {
                expression {
                    return "${DOCKER_VAR}".toBoolean()
                }
            }
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'evolved5g-push', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    script {
                        def cmd = "docker ps --format '{{.Image}}'"
                        def cmd2 = "docker ps --format '{{.Names}}'"
                        def image = sh(returnStdout: true, script: cmd).trim()
                        def name  = sh(returnStdout: true, script: cmd2).trim()
                        sh '''$(aws ecr get-login --no-include-email)'''
                        [image.tokenize(), name.tokenize()].transpose().each { x ->

                            if ( env.PATH_AWS != null){
                            sh """ docker tag ${x[0]} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME}-${x[1]}-${VERSION}"""
                            sh """ docker tag ${x[0]} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME}-${x[1]}-latest"""
                            sh """ docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME}-${x[1]}-${VERSION}"""
                            sh """ docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME}-${x[1]}-latest"""
                            sh """ python3 utils/helpers/add_image_json.py report-build-${NETAPP_NAME}.json ${NETAPP_NAME} aws_images ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME}-${x[1]}-${VERSION}"""
                            sh """ python3 utils/helpers/add_image_json.py report-build-${NETAPP_NAME}.json ${NETAPP_NAME} aws_images ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME}-${x[1]}-latest"""
                            }
                            else{
                            sh """ docker tag ${x[0]} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-${x[1]}-${VERSION}"""
                            sh """ docker tag ${x[0]} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-${x[1]}-latest"""
                            sh """ docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-${x[1]}-${VERSION}"""
                            sh """ docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-${x[1]}-latest"""
                            sh """ python3 utils/helpers/add_image_json.py report-build-${NETAPP_NAME}.json ${NETAPP_NAME} aws_images ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-${x[1]}-${VERSION}"""
                            sh """ python3 utils/helpers/add_image_json.py report-build-${NETAPP_NAME}.json ${NETAPP_NAME} aws_images ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-${x[1]}-latest"""
                            }
                        }
                    }
                }
            }
        }
        stage('Publish in AWS - Dockerfile') {
            when {
                expression {
                    return !"${DOCKER_VAR}".toBoolean()
                }
            }
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'evolved5g-push', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    sh '''
                    $(aws ecr get-login --no-include-email)
                    if [[ -n ${PATH_AWS} ]]
                    then
                        docker image tag ${NETAPP_NAME} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME}-${VERSION}
                        docker image tag ${NETAPP_NAME} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME}-latest
                        docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME}-latest
                        docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME}-${VERSION}
                        python3 utils/helpers/add_image_json.py report-build-${NETAPP_NAME}.json ${NETAPP_NAME} aws_images ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME}-${VERSION}
                        python3 utils/helpers/add_image_json.py report-build-${NETAPP_NAME}.json ${NETAPP_NAME} aws_images ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME}-latest
                    else
                        docker image tag ${NETAPP_NAME} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-${VERSION}
                        docker image tag ${NETAPP_NAME} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-latest
                        docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-latest
                        docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-${VERSION}
                        python3 utils/helpers/add_image_json.py report-build-${NETAPP_NAME}.json ${NETAPP_NAME} aws_images ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-${VERSION}
                        python3 utils/helpers/add_image_json.py report-build-${NETAPP_NAME}.json ${NETAPP_NAME} aws_images ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-latest
                    fi
                    '''
                }
            }
        }
        stage('Modify container name to upload Docker-compose to Artifactory') {
            when {
                expression {
                    return "${DOCKER_VAR}".toBoolean()
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    retry(1){
                        script {
                            sh ''' docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet '''
                            def cmd = "docker ps --format '{{.Image}}'"
                            def cmd2 = "docker ps --format '{{.Names}}'"
                            def image = sh(returnStdout: true, script: cmd).trim()
                            def name  = sh(returnStdout: true, script: cmd2).trim()
                            sh '''$(aws ecr get-login --no-include-email)'''
                            [image.tokenize(), name.tokenize()].transpose().each { x ->
                                if (env.PATH_DOCKER != null){
                                sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME}/"${NETAPP_NAME}-${x[1]}":${VERSION}"""
                                sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME}/"${NETAPP_NAME}-${x[1]}":latest"""
                                sh """ docker image push --all-tags dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME}/"${NETAPP_NAME}-${x[1]}" """
                                sh """ python3 utils/helpers/add_image_json.py report-build-${NETAPP_NAME}.json ${NETAPP_NAME} docker_hub_images dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME}/"${NETAPP_NAME}-${x[1]}":${VERSION}"""
                                sh """ python3 utils/helpers/add_image_json.py report-build-${NETAPP_NAME}.json ${NETAPP_NAME} docker_hub_images dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME}/"${NETAPP_NAME}-${x[1]}":latest"""
                                } else{
                                sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}/"${NETAPP_NAME}-${x[1]}":${VERSION}"""
                                sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}/"${NETAPP_NAME}-${x[1]}":latest"""
                                sh """ docker image push --all-tags dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}/"${NETAPP_NAME}-${x[1]}" """
                                sh """ python3 utils/helpers/add_image_json.py report-build-${NETAPP_NAME}.json ${NETAPP_NAME} docker_hub_images dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}/"${NETAPP_NAME}-${x[1]}":${VERSION}"""
                                sh """ python3 utils/helpers/add_image_json.py report-build-${NETAPP_NAME}.json ${NETAPP_NAME} docker_hub_images dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}/"${NETAPP_NAME}-${x[1]}":latest"""
                                }
                            }
                        }
                    }
                }
            }
        }
        stage('Publish in Artefactory') {
            when {
                expression {
                    return !"${DOCKER_VAR}".toBoolean()
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    retry(1){
                        sh '''
                        docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet
                        if [[ -n ${PATH_DOCKER} ]]
                        then
                            docker image tag ${NETAPP_NAME} dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME}:${VERSION}
                            docker image tag ${NETAPP_NAME} dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME}:latest
                            docker image push --all-tags dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME}
                            python3 utils/helpers/add_image_json.py report-build-${NETAPP_NAME}.json ${NETAPP_NAME} docker_hub_images dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME}:${VERSION}
                            python3 utils/helpers/add_image_json.py report-build-${NETAPP_NAME}.json ${NETAPP_NAME} docker_hub_images dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME}:latest
                        else
                            docker image tag ${NETAPP_NAME} dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}:${VERSION}
                            docker image tag ${NETAPP_NAME} dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}:latest
                            docker image push --all-tags dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}
                            python3 utils/helpers/add_image_json.py report-build-${NETAPP_NAME}.json ${NETAPP_NAME} docker_hub_images dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}:${VERSION}
                            python3 utils/helpers/add_image_json.py report-build-${NETAPP_NAME}.json ${NETAPP_NAME} docker_hub_images dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}:latest
                        fi

                        '''
                    }
                }
            }
        }
        stage('Upload report to Artifactory') {
            steps {
                 dir ("${WORKSPACE}/") {
                    sh '''#!/bin/bash

                        # get Commit Information
                        cd $NETAPP_NAME
                        commit=$(git rev-parse HEAD)
                        cd ..

                        urlT=https://github.com/EVOLVED-5G/$NETAPP_NAME/wiki/Telefonica-Evolved5g-$NETAPP_NAME
                        versionT=${VERSION}

                        python3 utils/report_generator.py --template templates/scan-build.md.j2 --json report-build-$NETAPP_NAME.json --output report-build-$NETAPP_NAME.md --repo ${GIT_NETAPP_URL} --branch ${GIT_NETAPP_BRANCH} --commit $commit --version $versionT --url $urlT
                        docker build  -t pdf_generator utils/docker_generate_pdf/.
                        docker run -v "$WORKSPACE":$DOCKER_PATH pdf_generator markdown-pdf -f A4 -b 1cm -s $DOCKER_PATH/utils/docker_generate_pdf/style.css -o $DOCKER_PATH/report-build-$NETAPP_NAME.pdf $DOCKER_PATH/report-build-$NETAPP_NAME.md
                        declare -a files=("json" "md" "pdf")

                        for x in "${files[@]}"
                            do
                                report_file="report-build-$NETAPP_NAME.$x"
                                url="$ARTIFACTORY_URL/$NETAPP_NAME/$BUILD_ID/$report_file"

                                curl -v -f -i -X PUT -u $ARTIFACTORY_CRED \
                                    --data-binary @"$report_file" \
                                    "$url"
                            done
                    '''
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