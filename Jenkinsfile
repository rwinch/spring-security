def projectProperties = [
	[$class: 'BuildDiscarderProperty',
		strategy: [$class: 'LogRotator', numToKeepStr: '5']],
	pipelineTriggers([cron('@daily')])
]
properties(projectProperties)

parallel check: {
	stage('Check') {
		node {
			checkout scm
			try {
				sh "./gradlew :spring-security-samples-xml-cassample:clean :spring-security-samples-xml-cassample:iTest --no-daemon"
			} finally {
				junit '**/build/*-results/*.xml'
			}
		}
	}
}
