/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
import org.gradle.api.tasks.SourceSetContainer;

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
			checkstyle.setToolVersion("14.4.0");
			checkstyle.getConfigDirectory().set(project.getRootProject().file("src/checkstyle"));
			String version = SpringJavaFormatPlugin.class.getPackage().getImplementationVersion();
			DependencySet checkstyleDependencies = project.getConfigurations().getByName("checkstyle").getDependencies();
			checkstyleDependencies.add(
					project.getDependencies().create("io.spring.javaformat:spring-javaformat-checkstyle:" + version));
			includeKotlinSources(project);
		});
	}

	/**
	 * Checkstyle only processes Java sources by default. Add the Kotlin sources of
	 * each source set so that file-level checks, such as the license header, apply to
	 * them as well.
	 */
	private static void includeKotlinSources(Project project) {
		SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
		sourceSets.all(sourceSet -> {
			String taskName = "checkstyle" + capitalize(sourceSet.getName());
			project.getTasks().withType(Checkstyle.class).matching(task -> task.getName().equals(taskName))
					.configureEach(task -> task.source(project.fileTree("src/" + sourceSet.getName() + "/kotlin",
							tree -> tree.include("**/*.kt"))));
		});
	}

	private static String capitalize(String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
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
