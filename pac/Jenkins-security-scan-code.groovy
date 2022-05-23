pipeline {
    agent any
    stages {
        stage('Say Hello and Goobye!') {
            steps {
                echo "Hello and Goodbye"
           }
        }
    }
}

