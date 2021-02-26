package org.springframework.security.convention;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.PluginManager;

public class SpringModulePlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		PluginManager plugins = project.getPluginManager();
		plugins.apply(JavaLibraryPlugin.class);

		project.getPlugins().withType(JavaPlugin.class).all((javaPlugin) -> {
			JavaPluginExtension extension = project.getExtensions().getByType(JavaPluginExtension.class);
			extension.withJavadocJar();
			extension.withSourcesJar();
		});
	}
}
