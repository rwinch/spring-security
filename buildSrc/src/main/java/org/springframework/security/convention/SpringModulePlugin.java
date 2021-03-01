package org.springframework.security.convention;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.ConfigurationVariantDetails;
import org.gradle.api.plugins.*;

public class SpringModulePlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		PluginManager plugins = project.getPluginManager();
		plugins.apply(JavaLibraryPlugin.class);
		plugins.apply(JavaTestFixturesPlugin.class);

		project.getPlugins().withType(JavaPlugin.class).all((javaPlugin) -> {
			JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
			java.withJavadocJar();
			java.withSourcesJar();
			registerOptionalFeature(project, java);
		});

		skipPublishingTestFixtures(project);
	}

	private void registerOptionalFeature(Project project, JavaPluginExtension java) {
		JavaPluginConvention javaConvention = project.getConvention().findPlugin(JavaPluginConvention.class);
		java.registerFeature("optional", new Action<FeatureSpec>() {
			@Override
			public void execute(FeatureSpec optional) {
				optional.usingSourceSet(javaConvention.getSourceSets().getByName("main"));
			}
		});
	}

	private void skipPublishingTestFixtures(Project project) {
		AdhocComponentWithVariants javaComponent = (AdhocComponentWithVariants) project.getComponents().getByName("java");
		ConfigurationContainer configurations = project.getConfigurations();
		Action<ConfigurationVariantDetails> skipPublishing = (details) -> details.skip();
		javaComponent.withVariantsFromConfiguration(configurations.getByName("testFixturesApiElements"), skipPublishing);
		javaComponent.withVariantsFromConfiguration(configurations.getByName("testFixturesRuntimeElements"), skipPublishing);
	}
}
