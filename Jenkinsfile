pipeline {
    agent any
    tools { maven 'M3'
    jdk 'jdk17' }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build') {
            steps {
                sh 'mvn clean install'
            }
        }
    }
}
