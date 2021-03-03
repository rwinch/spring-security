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

import org.asciidoctor.gradle.base.AbstractAsciidoctorBaseTask;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskOutputs;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.api.tasks.javadoc.Javadoc;

public class DocsZipPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getTasks().register("docsZip", Zip.class, new Action<Zip>() {
			@Override
			public void execute(Zip zip) {
				taskDependsOn(project, zip, Javadoc.class);
				taskDependsOn(project, zip, AbstractAsciidoctorBaseTask.class);
				zip.setGroup("Distribution");
				zip.getArchiveBaseName().convention(project.getRootProject().getName());
				zip.getArchiveClassifier().convention("docs");
				zip.setDescription("Builds -docs archive containing all Docs for deployment at docs.spring.io");
				zip.from(outputForTaskWithName(project, "asciidoctor"))
						.into("reference/html5")
						.include("**");
				zip.from(outputForTaskWithName(project, "asciidoctorPdf"))
						.into("reference/pdf")
						.include("**")
						.rename("index.pdf", "spring-security-reference.pdf");
			}
		});
	}

	private TaskOutputs outputForTaskWithName(Project project, String taskName) {
		return project.getTasks().getByName(taskName).getOutputs();
	}

	private void taskDependsOn(Project project, Task task, Class<? extends Task> dependsOnType) {
		project.getTasks().withType(dependsOnType).all(new Action<Task>() {
			@Override
			public void execute(Task dependsOn) {
				task.dependsOn(dependsOn);
			}
		});
	}
}
