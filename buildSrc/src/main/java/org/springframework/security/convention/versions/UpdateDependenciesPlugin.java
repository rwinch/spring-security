package org.springframework.security.convention.versions;

import com.github.benmanes.gradle.versions.reporter.result.DependencyOutdated;
import com.github.benmanes.gradle.versions.reporter.result.Result;
import com.github.benmanes.gradle.versions.reporter.result.VersionAvailable;
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask;
import com.github.benmanes.gradle.versions.updates.resolutionstrategy.ComponentSelectionRulesWithCurrent;
import com.github.benmanes.gradle.versions.updates.resolutionstrategy.ComponentSelectionWithCurrent;
import com.github.benmanes.gradle.versions.updates.resolutionstrategy.ResolutionStrategyWithCurrent;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
							groups.computeIfAbsent(outdated.getGroup(), (key) -> new ArrayList<>()).add(outdated);
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
				updateDependencies.resolutionStrategy(new Action<ResolutionStrategyWithCurrent>() {
					@Override
					public void execute(ResolutionStrategyWithCurrent resolution) {
						resolution.componentSelection(new Action<ComponentSelectionRulesWithCurrent>() {
							@Override
							public void execute(ComponentSelectionRulesWithCurrent components) {
								components.all(excludeWithRegex("(?i).*?(alpha|beta|m\\d+|rc\\d+).*", "an alpha or beta version"));
								components.all(excludeWithRegex("(?i).*?rc\\d+.*", "a release candidate version"));
								components.all((selection) -> {
									String currentVersion = selection.getCurrentVersion();
									int separator = currentVersion.indexOf(".");
									String major = separator > 0 ? currentVersion.substring(0, separator) : currentVersion;
									String candidateVersion = selection.getCandidate().getVersion();
									Pattern calVerPattern = Pattern.compile("\\d\\d\\d\\d.*");
									boolean isCalVer = calVerPattern.matcher(candidateVersion).matches();
									if (!isCalVer && !candidateVersion.startsWith(major)) {
										selection.reject("Cannot grade to new Major Version");
									}
								});
								components.all((selection) -> {
									ModuleComponentIdentifier candidate = selection.getCandidate();
									if ("org.apache.directory.server" .equals(candidate.getGroup()) && !candidate.getVersion().equals(selection.getCurrentVersion())) {
										selection.reject("org.apache.directory.server has breaking changes in newer versions");
									}
								});
								String jaxbBetaRegex = ".*?b\\d+.*";
								components.withModule("javax.xml.bind:jaxb-api", excludeWithRegex(jaxbBetaRegex, "Reject jaxb-api beta versions"));
								components.withModule("com.sun.xml.bind:jaxb-impl", excludeWithRegex(jaxbBetaRegex, "Reject jaxb-api beta versions"));
							}
						});
					}
				});
			}
		});
	}

	static Action<ComponentSelectionWithCurrent> excludeWithRegex(String regex, String reason) {
		Pattern pattern = Pattern.compile(regex);
		return (selection) -> {
			String candidateVersion = selection.getCandidate().getVersion();
			if (pattern.matcher(candidateVersion).matches()) {
				selection.reject(candidateVersion + " is not allowed becuase it is " + reason);
			}
		};
	}

	static void updateDependencyInlineVersion(Project project, DependencyOutdated dependency){
		String ga = dependency.getGroup() + ":" + dependency.getName() + ":";
		String originalDependency = ga + dependency.getVersion();
		String replacementDependency = ga + updatedVersion(dependency);
		File buildFile = project.getBuildFile();
		replaceFileText(buildFile, buildFileText -> buildFileText.replace(originalDependency, replacementDependency));
	}

	static void replaceFileText(File file, Function<String, String> replaceText) {
		String buildFileText = readString(file);
		String updatedBuildFileText = replaceText.apply(buildFileText);
		writeString(file, updatedBuildFileText);
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

	static void updateDependencyWithVersionVariable(Project project, DependencyOutdated dependency) {
		File gradlePropertiesFile = project.getRootProject().file(Project.GRADLE_PROPERTIES);
		if (!gradlePropertiesFile.exists()) {
			return;
		}
		replaceFileText(gradlePropertiesFile, (gradlePropertiesText) -> {
			String ga = dependency.getGroup() + ":" + dependency.getName() + ":";
			Pattern pattern = Pattern.compile("\"" + ga + "\\$\\{?([^'\"]+?)\\}?\"");
			String buildFileText = readString(project.getBuildFile());
			Matcher matcher = pattern.matcher(buildFileText);
			while (matcher.find()) {
				String versionVariable = matcher.group(1);
				gradlePropertiesText = gradlePropertiesText.replace(versionVariable + "=" + dependency.getVersion(), versionVariable + "=" + updatedVersion(dependency));
			}
			return gradlePropertiesText;
		});
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
