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
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.bundling.Zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SchemaZipPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getTasks().register("schemaZip", Zip.class, new Action<Zip>(){
			@Override
			public void execute(Zip zip) {
				zip.setGroup("Distribution");
				zip.getArchiveBaseName().convention("spring-security");
				zip.getArchiveClassifier().convention("schema");
				zip.setDescription("Builds -schema archive containing all XSDs for deployment at static.springframework.org/schema.");

				copyFromSubProjects(project, zip);
			}
		});
	}

	private void copyFromSubProjects(Project project, Zip zip) {
		project.getRootProject().getSubprojects().forEach(module -> {
			module.getPlugins().withType(JavaBasePlugin.class).all(copyFromSubProjectsWithJavaPlugin(project, zip));
		});
	}

	private Action<JavaBasePlugin> copyFromSubProjectsWithJavaPlugin(Project project, Zip zip) {
		return new Action<JavaBasePlugin>() {
			@Override
			public void execute(JavaBasePlugin java) {
				processJavaProject(project, zip);
			}
		};
	}

	private void processJavaProject(Project project, Zip zip) {
		Properties schemas = new Properties();
		JavaPluginConvention javaConvention = project.getConvention().findPlugin(JavaPluginConvention.class);
		SourceDirectorySet mainResources = javaConvention.getSourceSets().getByName("main").getResources();
		File springSchemas = findFileEndsWith(mainResources, "Meta-INF/spring.schemas");
		try (InputStream in = new FileInputStream(springSchemas)) {
			schemas.load(in);
		} catch (IOException ex) {
			throw new RuntimeException("Failed to read " + springSchemas, ex);
		}
		for (String key : schemas.stringPropertyNames()) {
			String shortName = key.replaceAll("/http.*schema.(.*).spring-.*/", "$1");
			if (key.equals(shortName)) {
				throw new IllegalStateException("Unable to extract the shortName from " + key);
			}
			String path = schemas.getProperty(key);
			File xsdFile = findFileEndsWith(mainResources, path);
			if (xsdFile == null) {
				throw new IllegalStateException("Failed to find xsdFile for " + key + "=" + path);
			}
			zip.into(shortName, new Action<CopySpec>() {
				@Override
				public void execute(CopySpec copy) {
					copy.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
					copy.from(xsdFile.getPath());
				}
			});
		}
	}

	private File findFileEndsWith(SourceDirectorySet sources, String endsWith) {
		return sources.filter(file -> file.getPath().endsWith(endsWith)).getSingleFile();
	}
}
