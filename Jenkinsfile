pipeline {

    agent any

    tools {
        maven 'Maven-3.9'
        jdk   'JDK-17'
    }

    environment {
        APP_NAME     = 'employee-service'
        DOCKER_IMAGE = 'yourdockerhubusername/employee-service'
        IMAGE_TAG    = "${BUILD_NUMBER}"
        SONAR_SERVER = 'SonarQube'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        timestamps()
    }

    stages {

        stage('Checkout') {
            steps {
                // Wipe workspace first — fixes corrupted git directory issues
                deleteDir()
                checkout scm
                echo "Branch : ${GIT_BRANCH}"
                echo "Commit : ${GIT_COMMIT}"
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests -B'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Test & Coverage') {
            steps {
                sh 'mvn test -Dspring.profiles.active=test -B'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv("${SONAR_SERVER}") {
                    sh """
                        mvn sonar:sonar \
                            -Dsonar.projectKey=employee-service \
                            -Dsonar.projectName='Employee Service' \
                            -B
                    """
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Build & Push') {
            when {
                branch 'main'
            }
            steps {
                sh "docker build -t yourdockerhubusername/employee-service:${BUILD_NUMBER} ."
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                        echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                        docker push yourdockerhubusername/employee-service:${BUILD_NUMBER}
                        docker tag yourdockerhubusername/employee-service:${BUILD_NUMBER} yourdockerhubusername/employee-service:latest
                        docker push yourdockerhubusername/employee-service:latest
                    """
                }
            }
        }

    }

    post {
            always {
                echo "Pipeline finished — employee-service #${BUILD_NUMBER}"
            }
            success {
                echo "✅ BUILD SUCCEEDED — employee-service #${BUILD_NUMBER}"
            }
            failure {
                echo "❌ BUILD FAILED — employee-service #${BUILD_NUMBER}"
            }
        }
}