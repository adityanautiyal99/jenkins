@Library('jenkis') _

node {
    try{
    stage("checking dependencies"){
        job()
    }
    stage("cloning git repo"){
        git_url.url "https://github.com/opstree/spring3hibernate.git"
    }
    stage("compiling the repo"){
        compile()
    }
    stage("Testing"){
        try{
            test.test()
        } catch (e) {
                currentBuild.result = "FAILED"
                throw e
            }
    }
    stage("sending analysis and creating package"){
        parallel([
            sonar: {
                /*withSonarQubeEnv('sonar') {
                        sh "mvn sonar:sonar"
                     }*/
            echo "hi"
            } ,
            war: {
                try{
                    package.war()
                } catch (e) {
                    currentBuild.result = "FAILED"
                    throw e
                }
            }
        ])
            
    }
    stage("ArchiveArtifact"){
        sh "mvn surefire-report:report"
        archiveArtifacts 'target/*.war'  
    }
    stage("Uploading Artifact"){
        /*upload()*/
        echo "done"
    } 
    } catch (e) {
        echo 'This will run only if failed'
        throw e
    } finally {
        def currentResult = currentBuild.result ?: 'SUCCESS'
        if (currentResult == 'SUCCESS') {
        notifySuccessful()
        cobertura autoUpdateHealth: false, autoUpdateStability: false, coberturaReportFile: ' **/target/site/cobertura/coverage.xml', conditionalCoverageTargets: '70, 0, 0', failUnhealthy: false, failUnstable: false, lineCoverageTargets: '80, 0, 0', maxNumberOfBuilds: 0, methodCoverageTargets: '80, 0, 0', onlyStable: false, sourceEncoding: 'ASCII', zoomCoverageChart: false
        publishCoverage adapters: [coberturaAdapter('**/target/site/cobertura/coverage.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')
        publishHTML(target : [allowMissing: false, alwaysLinkToLastBuild: true, keepAll: false, reportDir: '/var/lib/jenkins/workspace/pi/target/site', reportFiles: 'surefire-report.html', reportName: 'HTML Report', reportTitles: ''])
        }
        else{
                notifyFailed()
        }
    }
}
def notifyFailed() {
    slackSend (color: 'danger', message: "Failed: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
}
def notifySuccessful() {
    emailext attachLog: true,
    attachmentsPattern: 'target/site/surefire-report.html',
                body: "${currentBuild.currentResult}: Job ${env.JOB_NAME} build ${env.BUILD_NUMBER}\n More info at: ${env.BUILD_URL}",
                recipientProviders: [developers(), requestor()],
                subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}"
    slackSend (color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
    build job: 'upload',
        parameters: [
            string(name: 'BUILD_NU', value: 'Build_' + String.valueOf(env.BUILD_NUMBER))
        ]
}
