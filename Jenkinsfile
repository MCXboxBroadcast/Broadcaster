pipeline {
    agent any
    tools {
        gradle 'Gradle 8'
        jdk 'Java 17'
    }
    stages {
        stage('Build') {
            steps {
                sh './gradlew clean build'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'bootstrap/**/build/libs/*.jar', excludes: 'bootstrap/**/target/bootstrap-*.jar', fingerprint: true
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