pipeline {

    agent any

    tools {
        maven 'Maven-3.9'
        jdk   'JDK-17'
    }

    environment {
        APP_NAME     = 'employee-service'
        APP_VERSION  = '1.0.0'
        DOCKER_IMAGE = "yourdockerhubusername/employee-service"
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
                echo "Branch  : ${GIT_BRANCH}"
                echo "Commit  : ${GIT_COMMIT}"
                echo "Build # : ${BUILD_NUMBER}"
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo "Building ${APP_NAME}..."
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
                sh "docker build -t ${DOCKER_IMAGE}:${IMAGE_TAG} ."
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                        echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                        docker push ${DOCKER_IMAGE}:${IMAGE_TAG}
                        docker tag ${DOCKER_IMAGE}:${IMAGE_TAG} ${DOCKER_IMAGE}:latest
                        docker push ${DOCKER_IMAGE}:latest
                    """
                }
            }
        }

    }

    post {
        always {
            echo "Pipeline finished."
            sh "docker rmi ${DOCKER_IMAGE}:${IMAGE_TAG} || true"
        }
        success {
            echo "✅ BUILD SUCCEEDED — ${APP_NAME} #${BUILD_NUMBER}"
        }
        failure {
            echo "❌ BUILD FAILED — ${APP_NAME} #${BUILD_NUMBER}"
        }
        unstable {
            echo "⚠️ BUILD UNSTABLE — ${APP_NAME} #${BUILD_NUMBER}"
        }
    }
}