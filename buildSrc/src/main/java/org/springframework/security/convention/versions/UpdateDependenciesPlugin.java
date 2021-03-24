package org.springframework.security.convention.versions;

import com.github.benmanes.gradle.versions.reporter.result.DependencyOutdated;
import com.github.benmanes.gradle.versions.reporter.result.Result;
import com.github.benmanes.gradle.versions.reporter.result.VersionAvailable;
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class UpdateDependenciesPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getTasks().register("updateDependencies", DependencyUpdatesTask.class, new Action<DependencyUpdatesTask>() {
			@Override
			public void execute(DependencyUpdatesTask updateDependencies) {
				updateDependencies.setDescription("Update the dependencies");
				updateDependencies.setCheckConstraints(true);
				updateDependencies.setOutputFormatter(new Closure<Void>(null) {
					@Override
					public Void call(Object argument) {
						Result result = (Result) argument;
						SortedSet<DependencyOutdated> dependencies = result.getOutdated().getDependencies();
						if (dependencies.isEmpty()) {
							return null;
						}
						Map<String, List<DependencyOutdated>> groups = new LinkedHashMap<>();
						dependencies.forEach(outdated -> {
							groups.getOrDefault(outdated.getGroup(), new ArrayList<>()).add(outdated);
						});
						groups.forEach((group, outdated) -> {
							outdated.forEach((dependency) -> {
								updateDependencyInlineVersion(project, dependency);
								updateDependencyWithVersionVariable(project, dependency);
							});
						});
						return null;
					}
				});
			}
		});
	}

	private static void updateDependencyInlineVersion(Project project, DependencyOutdated dependency){
		String ga = dependency.getGroup() + ":" + dependency.getName() + ":";
		String originalDependency = ga + dependency.getVersion();
		String replacementDependency = ga + updatedVersion(dependency);
		File buildFile = project.getBuildFile();
		String buildFileText = readString(project.getBuildFile());
		String updatedBuildFileText = buildFileText.replace(originalDependency, replacementDependency);
		writeString(buildFile, updatedBuildFileText);
	}

	private static String readString(File file) {
		try {
			byte[] bytes = Files.readAllBytes(file.toPath());
			return new String(bytes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void writeString(File file, String text) {
		try {
			Files.write(file.toPath(), text.getBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void updateDependencyWithVersionVariable(Project project, DependencyOutdated dependency) {

	}

	private static String updatedVersion(DependencyOutdated dependency) {
		VersionAvailable available = dependency.getAvailable();
		String release = available.getRelease();
		if (release != null) {
			return release;
		}
		return available.getMilestone();
	}
}
