package org.springframework.security.convention;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class VersionsPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getPlugins().apply("com.github.ben-manes.versions");
	}
}
