// ════════════════════════════════════════════════════════════════════
// Jenkinsfile — Declarative Pipeline for employee-service
//
// This file lives in the root of the Git repo.
// Jenkins reads it automatically on every push (via GitHub webhook).
// ════════════════════════════════════════════════════════════════════

pipeline {

    // "any" → run on any available Jenkins agent (node)
    // In production you'd use: agent { label 'docker-agent' }
    agent any

    // ── Tool versions managed by Jenkins Global Tool Configuration ──
    tools {
        maven 'Maven-3.9'   // name must match what you configured in Jenkins UI
        jdk   'JDK-17'      // name must match what you configured in Jenkins UI
    }

    // ── Environment variables available to ALL stages ───────────────
    environment {

        // Application info
        APP_NAME        = 'employee-service'
        APP_VERSION     = '1.0.0'

        // Docker image name — change 'yourdockerhubusername' to your actual username
        DOCKER_IMAGE    = "yourdockerhubusername/${APP_NAME}"

        // Image tag = Git commit hash (first 7 chars) — unique per build, traceable
        // e.g. employee-service:a1b2c3d
        IMAGE_TAG       = "${GIT_COMMIT[0..6]}"

        // Jenkins credential IDs — you create these in:
        // Jenkins → Manage Jenkins → Credentials
        DOCKER_CREDS    = credentials('dockerhub-credentials')  // DockerHub username+password
        SONAR_TOKEN     = credentials('sonarqube-token')        // SonarQube auth token

        // SonarQube server name — must match Jenkins → Configure System → SonarQube
        SONAR_SERVER    = 'SonarQube'

        // Slack channel to send notifications to
        SLACK_CHANNEL   = '#cicd-pipeline'
    }

    // ── Pipeline-wide options ────────────────────────────────────────
    options {
        // Keep only last 10 builds — saves disk space on Jenkins server
        buildDiscarder(logRotator(numToKeepStr: '10'))

        // Kill the entire build if it runs longer than 30 minutes
        timeout(time: 30, unit: 'MINUTES')

        // Don't allow the same pipeline to run twice at the same time
        disableConcurrentBuilds()

        // Add timestamps to every log line — essential for debugging slow stages
        timestamps()
    }

    // ════════════════════════════════════════════════════════════════
    // STAGES — each is a discrete step shown in Jenkins Blue Ocean UI
    // ════════════════════════════════════════════════════════════════
    stages {

        // ── STAGE 1: CHECKOUT ────────────────────────────────────────
        // Jenkins clones your GitHub repo onto the agent workspace.
        // This is automatic with declarative pipelines — we just print info.
        stage('Checkout') {
            steps {
                echo "╔══════════════════════════════════╗"
                echo "║  Branch  : ${GIT_BRANCH}         ║"
                echo "║  Commit  : ${GIT_COMMIT}         ║"
                echo "║  Author  : ${GIT_AUTHOR_NAME}    ║"
                echo "╚══════════════════════════════════╝"

                // Clean workspace before build — avoids stale artifacts
                cleanWs()

                // Check out the code from GitHub
                checkout scm
            }
        }

        // ── STAGE 2: BUILD ───────────────────────────────────────────
        // Compiles the Java source code and packages it into a JAR.
        // -DskipTests → tests run in the next dedicated stage (separate reporting)
        stage('Build') {
            steps {
                echo "Building ${APP_NAME} v${APP_VERSION}..."
                sh 'mvn clean package -DskipTests -B'
            }
            post {
                success {
                    // Archive the JAR so it's downloadable from Jenkins UI
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        // ── STAGE 3: TEST + CODE COVERAGE ────────────────────────────
        // Runs all JUnit tests and generates a JaCoCo coverage report.
        // Build FAILS here if coverage drops below 70% (defined in pom.xml).
        stage('Test & Coverage') {
            steps {
                echo "Running unit tests..."
                // -Dspring.profiles.active=test → uses H2 in-memory DB, not PostgreSQL
                sh 'mvn test -Dspring.profiles.active=test -B'
            }
            post {
                always {
                    // Publish JUnit test results → shown as graphs in Jenkins UI
                    junit 'target/surefire-reports/*.xml'

                    // Publish JaCoCo coverage report → shown as coverage trend graph
                    jacoco(
                        execPattern:        'target/jacoco.exec',
                        classPattern:       'target/classes',
                        sourcePattern:      'src/main/java',
                        exclusionPattern:   '**/*Application*,**/model/**',
                        minimumLineCoverage: '70'
                    )
                }
            }
        }

        // ── STAGE 4: SONARQUBE ANALYSIS ──────────────────────────────
        // Sends code to SonarQube for:
        //   • Code smells detection
        //   • Bug detection
        //   • Security vulnerability scanning (SAST)
        //   • Technical debt measurement
        // Build FAILS if the SonarQube Quality Gate is not passed.
        stage('SonarQube Analysis') {
            steps {
                echo "Running SonarQube static analysis..."
                withSonarQubeEnv("${SONAR_SERVER}") {
                    sh """
                        mvn sonar:sonar \
                            -Dsonar.projectKey=${APP_NAME} \
                            -Dsonar.projectName='Employee Service' \
                            -Dsonar.projectVersion=${APP_VERSION} \
                            -Dsonar.java.coveragePlugin=jacoco \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                            -B
                    """
                }
            }
        }

        // ── STAGE 4b: SONARQUBE QUALITY GATE ─────────────────────────
        // Waits for SonarQube to finish analysis and checks the gate result.
        // Runs AFTER the analysis — SonarQube processes async.
        stage('Quality Gate') {
            steps {
                echo "Waiting for SonarQube Quality Gate result..."
                // Waits up to 5 minutes for SonarQube to respond
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // ── STAGE 5: TRIVY SECURITY SCAN ─────────────────────────────
        // Trivy scans the Docker image for known CVEs (vulnerabilities)
        // in OS packages and Java libraries BEFORE pushing to DockerHub.
        // HIGH/CRITICAL vulnerabilities fail the build.
        stage('Security Scan (Trivy)') {
            steps {
                echo "Building image for security scan..."
                // Build image locally first (not pushed yet)
                sh "docker build -t ${DOCKER_IMAGE}:scan-${IMAGE_TAG} ."

                echo "Scanning image for vulnerabilities..."
                sh """
                    // Run Trivy in Docker (no installation needed on agent)
                    docker run --rm \
                        -v /var/run/docker.sock:/var/run/docker.sock \
                        -v \$HOME/.cache:/root/.cache \
                        aquasec/trivy:latest image \
                        --exit-code 1 \
                        --severity HIGH,CRITICAL \
                        --no-progress \
                        --format table \
                        ${DOCKER_IMAGE}:scan-${IMAGE_TAG}
                """
            }
            post {
                always {
                    // Also generate a JSON report for audit trail
                    sh """
                        docker run --rm \
                            -v /var/run/docker.sock:/var/run/docker.sock \
                            -v \$HOME/.cache:/root/.cache \
                            -v \$(pwd):/output \
                            aquasec/trivy:latest image \
                            --format json \
                            --output /output/trivy-report.json \
                            ${DOCKER_IMAGE}:scan-${IMAGE_TAG}
                    """
                    // Archive the Trivy JSON report as a build artifact
                    archiveArtifacts artifacts: 'trivy-report.json', allowEmptyArchive: true
                }
            }
        }

        // ── STAGE 6: DOCKER BUILD & PUSH ─────────────────────────────
        // Only runs on the 'main' branch — not on feature branches.
        // Tags the image with both the commit hash AND 'latest'.
        // Pushes to DockerHub so Kubernetes can pull it in Step 6.
        stage('Docker Build & Push') {
            // Condition: only push from main branch
            when {
                branch 'main'
            }
            steps {
                echo "Pushing ${DOCKER_IMAGE}:${IMAGE_TAG} to DockerHub..."
                script {
                    docker.withRegistry('https://registry.hub.docker.com', 'dockerhub-credentials') {
                        def image = docker.build("${DOCKER_IMAGE}:${IMAGE_TAG}")

                        // Push with commit hash tag — e.g. employee-service:a1b2c3d
                        image.push()

                        // Also push 'latest' tag — used by Kubernetes as default
                        image.push('latest')

                        echo "✅ Pushed: ${DOCKER_IMAGE}:${IMAGE_TAG}"
                        echo "✅ Pushed: ${DOCKER_IMAGE}:latest"
                    }
                }
            }
        }

        // ── STAGE 7: UPDATE K8S MANIFEST ─────────────────────────────
        // Updates the Kubernetes deployment YAML with the new image tag.
        // ArgoCD (Step 7) watches this file and auto-deploys the change.
        // This is the GitOps pattern — Git is the source of truth.
        stage('Update K8s Manifest') {
            when {
                branch 'main'
            }
            steps {
                script {
                    echo "Updating Kubernetes manifest with new image tag: ${IMAGE_TAG}"
                    sh """
                        // Replace the image tag in the deployment YAML
                        sed -i 's|image: ${DOCKER_IMAGE}:.*|image: ${DOCKER_IMAGE}:${IMAGE_TAG}|g' \
                            kubernetes/deployment.yaml

                        // Commit and push the updated manifest back to GitHub
                        git config user.email "jenkins@cicd-pipeline.com"
                        git config user.name "Jenkins CI"
                        git add kubernetes/deployment.yaml
                        git commit -m "ci: update image tag to ${IMAGE_TAG} [skip ci]"
                        git push origin main
                    """
                    // [skip ci] in the commit message prevents an infinite loop
                    // — Jenkins won't trigger a new build for this commit
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // POST — runs after ALL stages, regardless of success/failure
    // ════════════════════════════════════════════════════════════════
    post {

        // Runs on every build completion
        always {
            echo "Pipeline finished. Cleaning up Docker images from agent..."
            sh "docker rmi ${DOCKER_IMAGE}:scan-${IMAGE_TAG} || true"
            // '|| true' → don't fail if image doesn't exist (e.g. build failed early)
        }

        success {
            echo "✅ BUILD SUCCEEDED"
            // Send Slack success notification
            slackSend(
                channel: "${SLACK_CHANNEL}",
                color:   'good',          // green
                message: """✅ *BUILD PASSED* — ${APP_NAME}
                    Branch  : ${GIT_BRANCH}
                    Commit  : ${GIT_COMMIT[0..6]}
                    Image   : ${DOCKER_IMAGE}:${IMAGE_TAG}
                    Duration: ${currentBuild.durationString}
                    Build   : ${BUILD_URL}"""
            )
        }

        failure {
            echo "❌ BUILD FAILED"
            // Send Slack failure notification with which stage failed
            slackSend(
                channel: "${SLACK_CHANNEL}",
                color:   'danger',        // red
                message: """❌ *BUILD FAILED* — ${APP_NAME}
                    Branch : ${GIT_BRANCH}
                    Commit : ${GIT_COMMIT[0..6]}
                    Stage  : ${FAILED_STAGE}
                    Build  : ${BUILD_URL}"""
            )
        }

        unstable {
            // 'unstable' = tests ran but some failed (yellow in Jenkins)
            slackSend(
                channel: "${SLACK_CHANNEL}",
                color:   'warning',       // yellow
                message: "⚠️ *BUILD UNSTABLE* — ${APP_NAME} | ${GIT_BRANCH} | ${BUILD_URL}"
            )
        }
    }
}