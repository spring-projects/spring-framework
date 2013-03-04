/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.build.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ProjectDependency;


/**
 * Gradle plugin that automatically updates testCompile dependencies to include
 * the test source sets of project dependencies.
 *
 * @author Phillip Webb
 */
class TestSourceSetDependenciesPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		project.afterEvaluate {
			Set<ProjectDependency> projectDependencies = new LinkedHashSet<ProjectDependency>()
			collectProjectDependencies(projectDependencies, project)
			projectDependencies.each {
				project.dependencies.add("testCompile", it.dependencyProject.sourceSets.test.output)
			}
		}
	}

	private void collectProjectDependencies(Set<ProjectDependency> projectDependencies,
			Project project) {
		for(def configurationName in ["compile", "optional", "provided", "testCompile"]) {
			Configuration configuration = project.getConfigurations().findByName(configurationName)
			if(configuration) {
				configuration.dependencies.findAll { it instanceof ProjectDependency }.each {
					projectDependencies.add(it)
					collectProjectDependencies(projectDependencies, it.dependencyProject)
				}
			}
		}
	}

}
