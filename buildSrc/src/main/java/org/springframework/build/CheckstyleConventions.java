/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.build;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.spring.javaformat.gradle.SpringJavaFormatPlugin;
import io.spring.nohttp.gradle.NoHttpExtension;
import io.spring.nohttp.gradle.NoHttpPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;

/**
 * {@link Plugin} that applies conventions for checkstyle.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 */
public class CheckstyleConventions {

	/**
	 * Applies the Spring Java Format and Checkstyle plugins with the project conventions.
	 * @param project the current project
	 */
	public void apply(Project project) {
		project.getPlugins().withType(JavaBasePlugin.class, (java) -> {
			if (project.getRootProject() == project) {
				configureNoHttpPlugin(project);
			}
			project.getPlugins().apply(CheckstylePlugin.class);
			project.getTasks().withType(Checkstyle.class).forEach(checkstyle -> checkstyle.getMaxHeapSize()
					.set("checkstyleNohttp".equals(checkstyle.getName()) ? "1536m" : "1g"));
			CheckstyleExtension checkstyle = project.getExtensions().getByType(CheckstyleExtension.class);
			checkstyle.setToolVersion("14.1.0");
			checkstyle.getConfigDirectory().set(project.getRootProject().file("src/checkstyle"));
			String version = SpringJavaFormatPlugin.class.getPackage().getImplementationVersion();
			DependencySet checkstyleDependencies = project.getConfigurations().getByName("checkstyle").getDependencies();
			checkstyleDependencies.add(
					project.getDependencies().create("io.spring.javaformat:spring-javaformat-checkstyle:" + version));
		});
	}

	private static void configureNoHttpPlugin(Project project) {
		project.getPlugins().apply(NoHttpPlugin.class);
		NoHttpExtension noHttp = project.getExtensions().getByType(NoHttpExtension.class);
		noHttp.setAllowlistFile(project.file("src/nohttp/allowlist.lines"));
		noHttp.getSource().exclude("**/test-output/**", "**/.settings/**", "**/.classpath",
				"**/.project", "**/.gradle/**", "**/node_modules/**", "**/spring-jcl/**", "buildSrc/build/**",
				".claude/**");
		excludeGitIgnoredPaths(project, noHttp);
		List<String> buildFolders = List.of("bin", "build", "out");
		project.allprojects(subproject -> {
			Path rootPath = project.getRootDir().toPath();
			Path projectPath = rootPath.relativize(subproject.getProjectDir().toPath());
			for (String buildFolder : buildFolders) {
				Path innerBuildDir = projectPath.resolve(buildFolder);
				noHttp.getSource().exclude(innerBuildDir + File.separator + "**");
			}
		});
	}

	/**
	 * Additionally exclude everything matched by the root {@code .gitignore} file,
	 * so that new ignored paths (build output, IDE metadata, local git worktrees,
	 * etc.) are automatically kept out of nohttp scanning without having to
	 * remember to mirror every {@code .gitignore} change here as well.
	 * <p>Negated patterns (lines starting with {@code !}) are not supported and are
	 * simply skipped, since there is no useful Ant-glob equivalent for them here.
	 */
	private static void excludeGitIgnoredPaths(Project project, NoHttpExtension noHttp) {
		File gitignore = project.getRootProject().file(".gitignore");
		if (!gitignore.exists()) {
			return;
		}
		try {
			for (String line : Files.readAllLines(gitignore.toPath())) {
				String pattern = line.strip();
				if (pattern.isEmpty() || pattern.startsWith("#") || pattern.startsWith("!")) {
					continue;
				}
				boolean directoryOnly = pattern.endsWith("/");
				if (directoryOnly) {
					pattern = pattern.substring(0, pattern.length() - 1);
				}
				// A '/' anywhere but a (now removed) trailing position anchors the
				// pattern to the repository root; otherwise it matches at any depth.
				boolean anchored = pattern.contains("/");
				if (pattern.startsWith("/")) {
					pattern = pattern.substring(1);
				}
				String rootPattern = anchored ? pattern : "**/" + pattern;
				if (directoryOnly) {
					noHttp.getSource().exclude(rootPattern + "/**");
				}
				else {
					// The pattern may match either a file or a directory, so exclude both.
					noHttp.getSource().exclude(rootPattern, rootPattern + "/**");
				}
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read .gitignore for nohttp exclusions", ex);
		}
	}

}
