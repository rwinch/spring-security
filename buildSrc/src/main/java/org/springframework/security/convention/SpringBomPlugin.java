package org.springframework.security.convention;

import io.spring.gradle.convention.ArtifactoryPlugin;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyConstraintHandler;
import org.gradle.api.plugins.*;
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
				System.out.println("childProject " + childProject);
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
		System.out.println("withType " + project);
		project.getPlugins().withType(SpringModulePlugin.class).all((springModule) -> {
			System.out.println("add constraints " + project);
			constraints.add(JavaPlugin.API_CONFIGURATION_NAME, constraints.create(project));
		});
	}
}
