pipeline {
    agent {
        node {
            label 'evol5-sonarqube'
        }
    }
    options {
        timeout(time: 30, unit: 'MINUTES')
    }
    environment {
        //ARTIFACTORY_CRED=credentials('artifactory_credentials')
        SCANNERHOME = tool 'Sonar Scanner 5';
    }
    stages {

        stage('SonarQube analysis') {
            steps {
                    sh '''
                        sudo ${SCANNERHOME}/bin/sonar-scanner -X \
                            -Dsonar.projectKey=Evolved5g-master\
                            -Dsonar.projectBaseDir=/home/ams@hi.inet//sonar-scanning-examples/sonarqube-scanner/src/python/ \
                            -Dsonar.host.url=http://10.95.133.49 \
                            -Dsonar.login=40f1332530d31e2372160616f6a458b82c5e429d \
                            -Dsonar.projectName=Evolved5g-master \
                            -Dsonar.language=python \
                            -Dsonar.sourceEncoding=UTF-8 \
                    '''
                }
            }
        }
    }
