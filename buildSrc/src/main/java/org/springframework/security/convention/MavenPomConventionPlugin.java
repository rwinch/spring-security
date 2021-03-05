/*
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.convention;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.PublicationContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

public class MavenPomConventionPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getPlugins().withType(MavenPublishPlugin.class).all((publishingPlugin) -> {
			PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
			PublicationContainer publications = publishing.getPublications();
			publications.withType(MavenPublication.class, (mavenPublication) ->
				mavenPublication.pom((pom) -> customizeMavenPomForProject(pom, project))
			);
		});
	}

	private void customizeMavenPomForProject(MavenPom pom, Project project) {
		Provider<String> defaultNameProvider = project.provider(() -> project.getGroup() + ":" + project.getName());
		pom.getName().set(defaultNameProvider);
		pom.getDescription().set(defaultNameProvider);
		pom.getUrl().set("https://spring.io/spring-security");
		pom.developers((developers) -> {
			developers.developer((developer) -> {
				developer.getName().set("Pivotal");
				developer.getEmail().set("info@pivotal.io");
				developer.getOrganization().set("Pivotal Software, Inc.");
				developer.getOrganizationUrl().set("https://www.spring.io");
			});
		});
		pom.issueManagement((issues) -> {
			issues.getSystem().set("GitHub");
			issues.getUrl().set("https://github.com/spring-projects/spring-security/issues");
		});
		pom.licenses((licenses) -> {
			licenses.license((license) -> {
				license.getName().set("The Apache Software License, Version 2.0");
				license.getUrl().set("https://www.apache.org/licenses/LICENSE-2.0");
			});
		});
		pom.organization((organization) -> {
			organization.getName().set("Spring");
			organization.getUrl().set("https://spring.io/");
		});
		pom.scm((scm) -> {
			scm.getConnection().set("scm:git:git://github.com/spring-projects/spring-security");
			scm.getDeveloperConnection().set("scm:git:ssh://git@github.com/spring-projects/spring-security.git");
			scm.getUrl().set("https://github.com/spring-projects/spring-security");
		});
	}
}
