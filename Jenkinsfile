pipeline {
    agent any
    tools { gradle ''; jdk '' }
    options {
        timestamps()
        disableConcurrentBuild(abortPrevious: true)
        buildDiscarder(logRotator(daysToKeepStr: '5', numToKeepStr: '20'))
    }
    parameters {
        booleanParam(name: 'retry', defaultValue: true, description: 'Retry the failed tests')
        choice(name: 'ENVIRONMENT', choices: ['DEV', 'QA', 'PROD'], description: 'test environment')
        choice(name: 'BROWSER', choices: ['CHROME', 'FIREFOX', 'SAFARI'], description: 'test browser')
        text(name: 'CUCUMBER_TAGS', defaultValue: '', description: 'test tags')
    }
    environment {
        JOB_URL = "${env.JOB_URL}"
        GIT_AUTHOR = sh(script: "git log -1 --pretty=%ae ${GIT_COMMIT}", returnStdout: true).trim()
        CRED_DEV = 'dev-secret-cred'
        CRED_STG = 'stg-secret-cred'
        ENVIRONMENT = "${params.ENVIRONMENT}"
    }
    stages {
        stage('Setup') {
            steps {
                script {
                    env.ENVIRONMENT = params.ENVIRONMENT ?: 'DEV'
                    if (env.BRANCH_NAME == 'main') {
                        env.CUCUMBER_TAGS = params.CUCUMBER_TAGS
                    } else {
                        env.CUCUMBER_TAGS = params.CUCUMBER_TAGS ?: '@all'
                    }
                    env.RUN_COMMAND = [
                        'gradle',
                        'cucumberTests',
                        "-Dcucumber.filter.tags=${env.CUCUMBER_TAGS}",
                        "-Dcucumber.features=src/test/java/org/spring/bdd/feature",
                        "-Dcucumber.glue=org.spring.bdd.stepDefs",
                        "-Dcucumber.plugin=pretty,json:target/cucumber-reports/cucumber.json,junit:target/cucumber-report/junit.xml",
                        "-DENVIRONMENT=${env.ENVIRONMENT}"
                    ].join(' ')
                }
            }
        }
        stage('Run tests') {
            steps {
                script {
                    def credentialsId = env.ENVIRONMENT == 'DEV' ? env.CRED_DEV : env.CRED_STG
                    withCredentials([usernamePassword(
                        credentialsId: credentialsId,
                        usernameVariable: 'USER_ID',
                        passwordVariable: 'SECRET_PASSWORD'
                    )]) {
                        sh env.RUN_COMMAND
                    }
                }
            }
        }
    }
    post {
        always {
            echo 'Cleaning up, finalizing steps...'
            // add any cleanup logic here
        }
        failure {
            mail to: 'recipient@example.com',
                 subject: "Build Failed in Jenkins: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Build failed. Please check the Jenkins job: ${env.BUILD_URL}"
        }
    }
}
