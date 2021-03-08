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

import io.spring.gradle.convention.ArtifactoryPlugin;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyConstraintHandler;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.PluginManager;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

public class SpringBomPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.setGroup(project.getRootProject().getGroup());
		PluginManager plugins = project.getPluginManager();
		plugins.apply(JavaPlatformPlugin.class);
		plugins.apply(ArtifactoryPlugin.class);
		plugins.apply(MavenPublishPlugin.class);
		plugins.apply(MavenPomConventionPlugin.class);

		project.getDependencies().constraints((constraint) -> {
			Project rootProject = project.getRootProject();
			rootProject.getChildProjects().values().forEach((childProject) -> {
				if (project != childProject) {
					addConstraintForSpringModules(constraint, childProject);
				}
			});
			if (project != rootProject) {
				addConstraintForSpringModules(constraint, rootProject);
			}
		});

		PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
		publishing.getPublications().register("maven", MavenPublication.class, new Action<MavenPublication>() {
			@Override
			public void execute(MavenPublication mavenPublication) {
				mavenPublication.from(project.getComponents().findByName("javaPlatform"));
			}
		});
	}

	private void addConstraintForSpringModules(DependencyConstraintHandler constraints, Project project) {
		project.getPlugins().withType(SpringModulePlugin.class).all((springModule) -> {
			constraints.add(JavaPlugin.API_CONFIGURATION_NAME, constraints.create(project));
		});
	}
}
