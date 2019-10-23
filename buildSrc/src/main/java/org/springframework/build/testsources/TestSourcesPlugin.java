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

package org.springframework.build.testsources;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;

import org.springframework.build.optional.OptionalDependenciesPlugin;

/**
 * {@link Plugin} that automatically updates testCompile dependencies to include
 * the test source sets of {@code project()} dependencies.
 *
 * <p>This plugin is used in the Spring Framework build to share test utilities and fixtures
 * between projects.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 */
public class TestSourcesPlugin implements Plugin<Project> {

	/**
	 * List of configurations this plugin should look for project dependencies in.
	 */
	@SuppressWarnings("deprecation")
	private static final List<String> CONFIGURATIONS = Arrays.asList(
			JavaPlugin.COMPILE_CONFIGURATION_NAME,
			JavaPlugin.API_CONFIGURATION_NAME,
			JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
			OptionalDependenciesPlugin.OPTIONAL_CONFIGURATION_NAME,
			JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME);

	@Override
	public void apply(Project project) {
		project.getPlugins().withType(JavaPlugin.class, (plugin) -> addTestSourcesToProject(project));
	}

	private void addTestSourcesToProject(Project project) {
		project.afterEvaluate(currentProject -> {
			Set<ProjectDependency> projectDependencies = new LinkedHashSet<>();
			collectProjectDependencies(projectDependencies, project);
			projectDependencies.forEach(dep -> addTestSourcesFromDependency(currentProject, dep));
		});
	}

	private void collectProjectDependencies(Set<ProjectDependency> projectDependencies, Project project) {
		for (String configurationName : CONFIGURATIONS) {
			Configuration configuration = project.getConfigurations().findByName(configurationName);
			if (configuration != null) {
				configuration.getDependencies().forEach(dependency -> {
					if (dependency instanceof ProjectDependency) {
						ProjectDependency projectDependency = (ProjectDependency) dependency;
						projectDependencies.add(projectDependency);
						collectProjectDependencies(projectDependencies, projectDependency.getDependencyProject());
					}
				});
			}
		}
	}

	private void addTestSourcesFromDependency(final Project currentProject, ProjectDependency dependency) {
		Project dependencyProject = dependency.getDependencyProject();
		dependencyProject.getPlugins().withType(JavaPlugin.class, plugin -> {
			final JavaPluginConvention javaPlugin = dependencyProject.getConvention()
					.getPlugin(JavaPluginConvention.class);
			SourceSetOutput test = javaPlugin.getSourceSets().findByName(SourceSet.TEST_SOURCE_SET_NAME).getOutput();
			currentProject.getDependencies().add(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME, test);
		});
	}
}
