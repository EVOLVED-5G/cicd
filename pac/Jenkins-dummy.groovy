pipeline {
    agent any

    parameters {
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
    }

    stages {
        stage('Say Hello and Goobye!') {
            steps {
                echo "Hello and Goodbye"
           }
        }
    }
}
