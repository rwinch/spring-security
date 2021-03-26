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

package org.springframework.security.convention;

import io.spring.javaformat.gradle.SpringJavaFormatPlugin;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;

import java.io.File;

/**
 * Adds and configures Checkstyle plugin.
 *
 * @author Rob Winch
 */
public class CheckstyleConventionsPlugin implements Plugin<Project> {

	final String CHECKSTYLE_DIR = "etc/checkstyle";

	@Override
	public void apply(Project project) {
		project.getPlugins().withType(CheckstylePlugin.class).all(new Action<CheckstylePlugin>() {
			@Override
			public void execute(CheckstylePlugin checkstylePlugin) {
				CheckstyleExtension checkstyle = project.getExtensions().findByType(CheckstyleExtension.class);
				File checkstyleDir = project.getRootProject().file(CHECKSTYLE_DIR);
				if (checkstyleDir.exists() && checkstyleDir.isDirectory()) {
					checkstyle.getConfigDirectory().fileValue(checkstyleDir);
				}
				String javaformatVersion = SpringJavaFormatPlugin.class.getPackage().getImplementationVersion();
				DependencySet checkstyleDependencies = project.getConfigurations().getByName("checkstyle").getDependencies();
				DependencyHandler dependencyHandler = project.getDependencies();
				checkstyleDependencies.add(dependencyHandler.create("io.spring.javaformat:spring-javaformat-checkstyle:" + javaformatVersion));
				checkstyleDependencies.add(dependencyHandler.create("io.spring.nohttp:nohttp-checkstyle:0.0.5.RELEASE"));
			}
		});
	}
}

