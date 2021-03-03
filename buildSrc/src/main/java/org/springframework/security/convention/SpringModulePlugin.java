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

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.ConfigurationVariantDetails;
import org.gradle.api.plugins.*;
import org.gradle.api.plugins.quality.CheckstylePlugin;

public class SpringModulePlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		PluginManager plugins = project.getPluginManager();
		plugins.apply(JavaLibraryPlugin.class);
		plugins.apply(JavaTestFixturesPlugin.class);
		plugins.apply(CheckstylePlugin.class);
		plugins.apply(CheckstyleConventionsPlugin.class);

		project.getPlugins().withType(JavaPlugin.class).all((javaPlugin) -> {
			JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
			java.withJavadocJar();
			java.withSourcesJar();
			registerOptionalFeature(project, java);
			testImplementationExtendsCompileOnly(project);
		});

		skipPublishingTestFixtures(project);
	}

	private void testImplementationExtendsCompileOnly(Project project) {
		ConfigurationContainer configurations = project.getConfigurations();
		configurations.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME));
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
