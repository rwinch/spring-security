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

import io.spring.gradle.convention.DeployDocsPlugin;
import org.asciidoctor.gradle.jvm.AsciidoctorJPlugin;
import org.asciidoctor.gradle.jvm.pdf.AsciidoctorJPdfPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.PluginManager;

public class DocsPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		PluginManager pluginManager = project.getPluginManager();
		pluginManager.apply(AggregateJavadocPlugin.class);
		pluginManager.apply(AsciidoctorJPlugin.class);
		pluginManager.apply(AsciidoctorJPdfPlugin.class);
		pluginManager.apply(AsciidoctorConventionPlugin.class);
		pluginManager.apply(DocsZipPlugin.class);
		pluginManager.apply(DeployDocsPlugin.class);
	}
}
