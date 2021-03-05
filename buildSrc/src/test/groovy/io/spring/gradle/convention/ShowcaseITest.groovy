/*
 * Copyright 2016-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.spring.gradle.convention

import io.spring.gradle.testkit.junit.rules.TestKit
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import spock.lang.Specification

class ShowcaseITest extends Specification {

	@Rule final TestKit testKit = new TestKit()

	def "build"() {
		when:
		BuildResult result = testKit.withProjectResource("samples/showcase/")
				.withArguments('build','--stacktrace')
				.forwardOutput()
				.build();
		then: 'entire build passes'
		result.output.contains("BUILD SUCCESSFUL")

	}

	def "publishToMavenLocal"() {
		when:
		BuildResult result = testKit.withProjectResource("samples/showcase/")
				.withArguments('publishToMavenLocal','--stacktrace')
				.build();
        println result.output
		then:
		result.output.contains("SUCCESS")

		and: 'pom exists'
		File pom = new File(testKit.getRootDir(), 'sgbcs-core/build/publications/maven/pom-default.xml')
		pom.exists()
		String pomText = pom.getText()

		and: 'pom does not contain <dependencyManagement>'
		!pomText.contains('<dependencyManagement>')

		and: 'creates optional dependencies correctly'

        pomText.replaceAll('\\s','').contains("""<dependency>
			<groupId>ch.qos.logback</groupId>
			<artifactId>logback-classic</artifactId>
			<version>1.1.9</version>
			<scope>compile</scope>
			<optional>true</optional>
		</dependency>""".replaceAll('\\s',''))

// FIXME: Should we add test dependencies? Gradle does not do this by default
//        and: 'test dependencies correctly'
//		pomText.replaceAll('\\s','').contains("""<dependency>
//			<groupId>org.springframework</groupId>
//			<artifactId>spring-test</artifactId>
//			<scope>test</scope>
//			<version>4.3.6.RELEASE</version>
//		</dependency>""".replaceAll('\\s',''))

		and: 'adds author'
		pomText.replaceAll('\\s','').contains("""<developers>
			<developer>
				<name>Pivotal</name>
				<email>info@pivotal.io</email>
				<organization>Pivotal Software, Inc.</organization>
				<organizationUrl>https://www.spring.io</organizationUrl>
			</developer>
		</developers>""".replaceAll('\\s',''))

		and: 'adds description & url'
		pomText.contains('<description>org.springframework.build.test:sgbcs-core</description>')
		pomText.contains('<url>https://spring.io/spring-security</url>')

		and: 'adds group'
		pomText.contains('''<groupId>org.springframework.build.test</groupId>''')

		and: 'adds organization'
		pomText.replaceAll('\\s','').contains('''<organization>
			<name>Spring</name>
			<url>https://spring.io/</url>
		</organization>'''.replaceAll('\\s',''))

		and: 'adds licenses'
		pomText.replaceAll('\\s','').contains('''	<licenses>
			<license>
				<name>The Apache Software License, Version 2.0</name>
				<url>https://www.apache.org/licenses/LICENSE-2.0</url>
			</license>
		</licenses>'''.replaceAll('\\s',''))

		and: 'adds scm'
        pomText.replaceAll('\\s','').contains("""<scm>
			<connection>scm:git:git://github.com/spring-projects/spring-security</connection>
			<developerConnection>scm:git:ssh://git@github.com/spring-projects/spring-security.git</developerConnection>
			<url>https://github.com/spring-projects/spring-security</url>
		</scm>""".replaceAll('\\s',''))

		and: 'bom created'
		File bom = new File(testKit.getRootDir(), 'bom/build/publications/maven/pom-default.xml')
		bom.exists()
		String bomText = bom.getText()
		bomText.contains("""<artifactId>sgbcs-core</artifactId>""")
	}

}
