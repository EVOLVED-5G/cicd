String forceDockerCleanBuild(String options) {
    return (options && options == 'True') ? '--no-cache' : ' '
}

String dockerVersion(String options) {
    return (options) ? options : 'latest'
}

pipeline {
    agent { node { label 'evol5-slave' } }
    options {
        disableConcurrentBuilds()
        ansiColor('xterm')
    }
    parameters {
        string(name: 'BRANCH_NAME', defaultValue: 'main', description: 'Deployment git branch name')
        choice(name: 'FORCE_DOCKER_CLEAN_BUILD', choices: ['False', 'True'], description: 'Force Docker Clean Build. Default use cached images (False)')
        string(name: 'PDF_GENERATOR_VERSION', defaultValue: 'latest', description: 'Robot Docker image version')
        booleanParam(name: 'GENERATE_PDF_GENERATOR', defaultValue: false, description: 'Check if robot docker image should be generated')
    }
    environment {
        BRANCH_NAME = "${params.BRANCH_NAME}"
        CACHE = forceDockerCleanBuild("${params.FORCE_DOCKER_CLEAN_BUILD}")
        PDF_GENERATOR_VERSION = dockerVersion("${params.PDF_GENERATOR_VERSION}")
        GENERATE_PDF_GENERATOR = "${params.GENERATE_PDF_GENERATOR}"
        PDF_GENERATOR_IMAGE_NAME="dockerhub.hi.inet/evolved-5g/evolved-pdf-generator"
    }
    stages {
        stage ('Generate Evolved Robot Docker tool') {
            when {
                expression { GENERATE_PDF_GENERATOR == 'true' }
            }
            steps {
                dir ("${WORKSPACE}/utils/docker_generate_pdf") {
                    withCredentials([usernamePassword(
                    credentialsId: 'docker_pull_cred',
                    usernameVariable: 'USER',
                    passwordVariable: 'PASS'
                   )]) {
                        sh '''
                        docker login --username ${USER} --password ${PASS} dockerhub.hi.inet
                        docker build ${CACHE} . -t ${PDF_GENERATOR_IMAGE_NAME}:${PDF_GENERATOR_VERSION}
                        docker push ${PDF_GENERATOR_IMAGE_NAME}:${PDF_GENERATOR_VERSION}
                      '''
                   }
                }
            }
        }
    }
    post {
        always {
            echo 'tools built'
            echo ' clean dockerhub credentials'
            sh 'rm -f ${HOME}/.docker/config.json'
        }
    }
}
