pipeline {
    agent any
    tools {
        maven 'Maven 3'
        jdk 'Java 17'
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'bootstrap/**/target/*.jar', excludes: 'bootstrap/**/target/original-*.jar', fingerprint: true
                }
            }
        }
    }
    post {
        always {
            deleteDir()
        }
    }
}