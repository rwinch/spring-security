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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateDependenciesPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		UpdateDependenciesExtension updateDependenciesSettings = project.getExtensions().create("updateDependenciesSettings", UpdateDependenciesExtension.class, defaultFiles(project));
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
							// FIXME need to ensure gradle version can be updated
							return null;
						}
						Map<String, List<DependencyOutdated>> groups = new LinkedHashMap<>();
						dependencies.forEach(outdated -> {
							String ga = outdated.getGroup() + ":" + outdated.getName() + ":";
							String originalDependency = ga + outdated.getVersion();
							String replacementDependency = ga + updatedVersion(outdated);
							System.out.println("Update " + originalDependency + " to " + replacementDependency);
							groups.computeIfAbsent(outdated.getGroup(), (key) -> new ArrayList<>()).add(outdated);
						});
						File gradlePropertiesFile = project.getRootProject().file(Project.GRADLE_PROPERTIES);

						List<File> filesWithDependencies = updateDependenciesSettings.getFiles().get();
						groups.forEach((group, outdated) -> {
							outdated.forEach((dependency) -> {
								filesWithDependencies.forEach((fileWithDependency) -> {
									updateDependencyInlineVersion(fileWithDependency, dependency);
									updateDependencyWithVersionVariable(fileWithDependency, gradlePropertiesFile, dependency);
								});
							});

							// commit
							DependencyOutdated firstDependency = outdated.get(0);
							String updatedVersion = updatedVersion(firstDependency);
							String title = outdated.size() == 1 ? "Update " + firstDependency.getName() + " to " + updatedVersion : "Update " + firstDependency.getGroup() + " to " + updatedVersion;
//							runCommand(project.getRootDir(), "git", "checkout", "-b", "bot-"+title.replace(' ', '-').toLowerCase());
							runCommand(project.getRootDir(), "git", "commit", "-am", title);
//							runCommand(project.getRootDir(), "git", "checkout", "-");
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
								updateDependenciesSettings.getExcludes().getActions().forEach((action) -> {
									components.all(action);
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
								components.withModule("commons-collections:commons-collections", excludeWithRegex("^\\d{3,}.*", "Reject commons-collections date based releases"));
							}
						});
					}
				});
			}
		});
	}

	private static Supplier<List<File>> defaultFiles(Project project) {
		return () -> {
			List<File> result = new ArrayList<>();
			result.add(project.getBuildFile());
			project.getChildProjects().values().forEach((childProject) ->
					result.add(childProject.getBuildFile())
			);
			result.add(project.getRootProject().file("buildSrc/build.gradle"));
			return result;
		};
	}

	static void runCommand(File dir, String... args) {
		try {
			if (new ProcessBuilder().directory(dir).command(args).start().waitFor() != 0) {
				new RuntimeException("Failed to run " + Arrays.toString(args));
			}
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Failed to run " + Arrays.toString(args), e);
		}
	}

	static Action<ComponentSelectionWithCurrent> excludeWithRegex(String regex, String reason) {
		Pattern pattern = Pattern.compile(regex);
		return (selection) -> {
			String candidateVersion = selection.getCandidate().getVersion();
			if (pattern.matcher(candidateVersion).matches()) {
				selection.reject(candidateVersion + " is not allowed because it is " + reason);
			}
		};
	}

	static void updateDependencyInlineVersion(File buildFile, DependencyOutdated dependency){
		String ga = dependency.getGroup() + ":" + dependency.getName() + ":";
		String originalDependency = ga + dependency.getVersion();
		String replacementDependency = ga + updatedVersion(dependency);
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

	static void updateDependencyWithVersionVariable(File scanFile, File gradlePropertiesFile, DependencyOutdated dependency) {
		if (!gradlePropertiesFile.exists()) {
			return;
		}
		replaceFileText(gradlePropertiesFile, (gradlePropertiesText) -> {
			String ga = dependency.getGroup() + ":" + dependency.getName() + ":";
			Pattern pattern = Pattern.compile("\"" + ga + "\\$\\{?([^'\"]+?)\\}?\"");
			String buildFileText = readString(scanFile);
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
