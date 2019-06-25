#!/usr/bin/env groovy
@Library('peon-pipeline') _

node {
    def appToken
    def commitHash
    try {
        cleanWs()

        def version
        stage("checkout") {
            appToken = github.generateAppToken()

            sh "git init"
            sh "git pull https://x-access-token:$appToken@github.com/navikt/peproxy.git"


            sh "make bump-version"

            version = sh(script: 'cat VERSION', returnStdout: true).trim()
            commitHash = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()

            github.commitStatus("pending", "navikt/peproxy", appToken, commitHash)
        }

        stage("build") {
            sh "make"
        }

        stage("release") {
            withCredentials([usernamePassword(credentialsId: 'nexusUploader', usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
                sh "docker login -u ${env.NEXUS_USERNAME} -p ${env.NEXUS_PASSWORD} repo.adeo.no:5443"
            }

            sh "make release"

            sh "git push --tags https://x-access-token:$appToken@github.com/navikt/peproxy HEAD:master"
        }

        stage("deploy preprod") {
            sh "make deploy-preprod"
            slackSend([
                    color  : 'good',
                    message: "<${env.BUILD_URL}|Build #${env.BUILD_NUMBER}> :penguin: peproxy deployed to preprod"
            ])
        }

        github.commitStatus("success", "navikt/peproxy", appToken, commitHash)
    } catch (err) {
        github.commitStatus("failure", "navikt/peproxy", appToken, commitHash)

        throw err
    }
}