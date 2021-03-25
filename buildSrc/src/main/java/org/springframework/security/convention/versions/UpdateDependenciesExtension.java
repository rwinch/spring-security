package org.springframework.security.convention.versions;

import com.github.benmanes.gradle.versions.updates.resolutionstrategy.ComponentSelectionWithCurrent;
import org.gradle.api.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class UpdateDependenciesExtension {
	private DependencyExcludes dependencyExcludes = new DependencyExcludes();

	DependencyExcludes getExcludes() {
		return dependencyExcludes;
	}

	public void dependencyExcludes(Action<DependencyExcludes> excludes) {
		excludes.execute(this.dependencyExcludes);
	}

	/**
	 * Consider creating some Predicates instead since they are composible
	 */
	public class DependencyExcludes {
		private List<Action<ComponentSelectionWithCurrent>> actions = new ArrayList<>();

		List<Action<ComponentSelectionWithCurrent>> getActions() {
			return actions;
		}

		public DependencyExcludes alphaBeta() {
			this.actions.add(excludeVersionWithRegex("(?i).*?(alpha|beta).*", "an alpha or beta version"));
			return this;
		}

		public DependencyExcludes majorVersionBump() {
			this.actions.add((selection) -> {
				String currentVersion = selection.getCurrentVersion();
				int separator = currentVersion.indexOf(".");
				String major = separator > 0 ? currentVersion.substring(0, separator) : currentVersion;
				String candidateVersion = selection.getCandidate().getVersion();
				Pattern calVerPattern = Pattern.compile("\\d\\d\\d\\d.*");
				boolean isCalVer = calVerPattern.matcher(candidateVersion).matches();
				if (!isCalVer && !candidateVersion.startsWith(major)) {
					selection.reject("Cannot upgrade to new Major Version");
				}
			});
			return this;
		}

		public DependencyExcludes releaseCandidates() {
			this.actions.add(excludeVersionWithRegex("(?i).*?rc\\d+.*", "a release candidate version"));
			return this;
		}

		public DependencyExcludes milestones() {
			this.actions.add(excludeVersionWithRegex("(?i).*?m\\d+.*", "a milestone version"));
			return this;
		}

		public DependencyExcludes snapshots() {
			this.actions.add(excludeVersionWithRegex(".*?-SNAPSHOT.*", "a SNAPSHOT version"));
			return this;
		}

		private Action<ComponentSelectionWithCurrent> excludeVersionWithRegex(String regex, String reason) {
			Pattern pattern = Pattern.compile(regex);
			return (selection) -> {
				String candidateVersion = selection.getCandidate().getVersion();
				if (pattern.matcher(candidateVersion).matches()) {
					selection.reject(candidateVersion + " is not allowed because it is " + reason);
				}
			};
		}
	}
}
