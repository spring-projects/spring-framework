/*
 * Copyright 2002-2019 the original author or authors.
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
package org.springframework.build.api;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import me.champeau.gradle.japicmp.JapicmpPlugin;
import me.champeau.gradle.japicmp.JapicmpTask;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Plugin} that applies the {@code "japicmp-gradle-plugin"}
 * and create tasks for all subprojects named {@code "spring-*"}, diffing the public API one by one
 * and creating the reports in {@code "build/reports/api-diff/$OLDVERSION_to_$NEWVERSION/"}.
 * <p>{@code "./gradlew apiDiff -PbaselineVersion=5.1.0.RELEASE"} will output the
 * reports for the API diff between the baseline version and the current one for all modules.
 * You can limit the report to a single module with
 * {@code "./gradlew :spring-core:apiDiff -PbaselineVersion=5.1.0.RELEASE"}.
 *
 * @author Brian Clozel
 */
public class ApiDiffPlugin implements Plugin<Project> {

	private static final Logger logger = LoggerFactory.getLogger(ApiDiffPlugin.class);

	public static final String TASK_NAME = "apiDiff";

	private static final String BASELINE_VERSION_PROPERTY = "baselineVersion";

	private static final List<String> PACKAGE_INCLUDES = Collections.singletonList("org.springframework.*");

	private static final URI SPRING_MILESTONE_REPOSITORY = URI.create("https://repo.spring.io/milestone");

	@Override
	public void apply(Project project) {
		if (project.hasProperty(BASELINE_VERSION_PROPERTY) && project.equals(project.getRootProject())) {
			project.getPluginManager().apply(JapicmpPlugin.class);
			project.getPlugins().withType(JapicmpPlugin.class,
					plugin -> applyApiDiffConventions(project));
		}
	}

	private void applyApiDiffConventions(Project project) {
		String baselineVersion = project.property(BASELINE_VERSION_PROPERTY).toString();
		project.subprojects(subProject -> {
			if (subProject.getName().startsWith("spring-")) {
				createApiDiffTask(baselineVersion, subProject);
			}
		});
	}

	private void createApiDiffTask(String baselineVersion, Project project) {
		if (isProjectEligible(project)) {
			// Add Spring Milestone repository for generating diffs against previous milestones
			project.getRootProject()
					.getRepositories()
					.maven(mavenArtifactRepository -> mavenArtifactRepository.setUrl(SPRING_MILESTONE_REPOSITORY));
			JapicmpTask apiDiff = project.getTasks().create(TASK_NAME, JapicmpTask.class);
			apiDiff.setDescription("Generates an API diff report with japicmp");
			apiDiff.setGroup(JavaBasePlugin.DOCUMENTATION_GROUP);

			apiDiff.setOldClasspath(createBaselineConfiguration(baselineVersion, project));
			TaskProvider<Jar> jar = project.getTasks().withType(Jar.class).named("jar");
			apiDiff.setNewArchives(project.getLayout().files(jar.get().getArchiveFile().get().getAsFile()));
			apiDiff.setNewClasspath(getRuntimeClassPath(project));
			apiDiff.setPackageIncludes(PACKAGE_INCLUDES);
			apiDiff.setOnlyModified(true);
			apiDiff.setIgnoreMissingClasses(true);
			// Ignore Kotlin metadata annotations since they contain
			// illegal HTML characters and fail the report generation
			apiDiff.setAnnotationExcludes(Collections.singletonList("@kotlin.Metadata"));

			apiDiff.setHtmlOutputFile(getOutputFile(baselineVersion, project));

			apiDiff.dependsOn(project.getTasks().getByName("jar"));
		}
	}

	private boolean isProjectEligible(Project project) {
		return project.getPlugins().hasPlugin(JavaPlugin.class)
				&& project.getPlugins().hasPlugin(MavenPublishPlugin.class);
	}

	private Configuration createBaselineConfiguration(String baselineVersion, Project project) {
		String baseline = String.join(":",
				project.getGroup().toString(), project.getName(), baselineVersion);
		Dependency baselineDependency = project.getDependencies().create(baseline + "@jar");
		Configuration baselineConfiguration = project.getRootProject().getConfigurations().detachedConfiguration(baselineDependency);
		try {
			// eagerly resolve the baseline configuration to check whether this is a new Spring module
			baselineConfiguration.resolve();
			return baselineConfiguration;
		}
		catch (GradleException exception) {
			logger.warn("Could not resolve {} - assuming this is a new Spring module.", baseline);
		}
		return project.getRootProject().getConfigurations().detachedConfiguration();
	}

	private Configuration getRuntimeClassPath(Project project) {
		return project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
	}

	private File getOutputFile(String baseLineVersion, Project project) {
		Path outDir = Paths.get(project.getRootProject().getBuildDir().getAbsolutePath(),
				"reports", "api-diff",
				baseLineVersion + "_to_" + project.getRootProject().getVersion());
		return project.file(outDir.resolve(project.getName() + ".html").toString());
	}

}