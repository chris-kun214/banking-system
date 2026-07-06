pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    parameters {
        booleanParam(
            name: 'DO_DEPLOY',
            defaultValue: false,
            description: 'Whether to run a "simple CD" step: copy the built jar into DEPLOY_DIR and write version metadata (no service start, no Docker).'
        )
        string(
            name: 'DEPLOY_DIR',
            defaultValue: '/var/lib/jenkins/deploy/banking_backend',
            description: 'Publish directory on the Jenkins machine (local path) for the simple CD step.'
        )
    }

    environment {
        // CI: disable Gradle daemon to avoid long-lived processes on Jenkins agents.
        GRADLE_ARGS = '--no-daemon --stacktrace'
        // If the agent does not have JDK 21 installed, allow Gradle toolchains to auto-download (requires outbound internet).
        GRADLE_OPTS = '-Dorg.gradle.java.installations.auto-download=true'
    }

    stages {
        stage('1. Checkout') {
            steps {
                echo "Checking out source code from SCM..."
                checkout scm
            }
        }

        stage('2. Build + Test (CI)') {
            steps {
                echo "Running unit tests and building the jar..."
                sh 'chmod +x gradlew'
                sh '''
                    set -e
                    java -version
                    ./gradlew --version
                    ./gradlew clean test bootJar ${GRADLE_ARGS}
                '''
            }
        }

        stage('3. Reports + Artifacts') {
            steps {
                echo "Archiving test reports and build artifacts..."
            }
            post {
                always {
                    junit testResults: 'build/test-results/test/*.xml', allowEmptyResults: true
                    archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true, allowEmptyArchive: true
                }
            }
        }

        stage('4. Simple CD - Publish Jar (Optional)') {
            when {
                allOf {
                    branch 'main'
                    expression { return params.DO_DEPLOY }
                }
            }
            steps {
                script {
                    def jarPath = sh(returnStdout: true, script: 'ls -1 build/libs/*.jar | head -n 1').trim()
                    def gitSha = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                    def versionTag = "${env.BUILD_NUMBER}-${gitSha}"

                    echo "Publishing jar: ${jarPath}"
                    echo "Publish directory: ${params.DEPLOY_DIR}"

                    sh """
                        set -e
                        mkdir -p '${params.DEPLOY_DIR}/releases'
                        cp '${jarPath}' '${params.DEPLOY_DIR}/releases/app-${versionTag}.jar'
                        ln -sfn '${params.DEPLOY_DIR}/releases/app-${versionTag}.jar' '${params.DEPLOY_DIR}/current.jar'
                        printf 'BUILD_NUMBER=%s\\nGIT_SHA=%s\\nJAR=%s\\n' '${env.BUILD_NUMBER}' '${gitSha}' 'app-${versionTag}.jar' > '${params.DEPLOY_DIR}/VERSION'
                        ls -lah '${params.DEPLOY_DIR}'
                    """
                }
            }
        }
    }

    post {
        success {
            echo "CI/CD pipeline completed successfully."
        }
        failure {
            echo "Pipeline failed. Please check the Jenkins console output for details."
        }
    }
}