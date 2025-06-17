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

package org.springframework.build.multirelease;

import javax.inject.Inject;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.jvm.toolchain.JavaToolchainService;

/**
 * A plugin which adds support for building multi-release jars
 * with Gradle.
 * @author Cedric Champeau
 * @author Brian Clozel
 * @see <a href="https://github.com/melix/mrjar-gradle-plugin">original project</a>
 */
public class MultiReleaseJarPlugin implements Plugin<Project> {

	@Inject
	protected JavaToolchainService getToolchains() {
		throw new UnsupportedOperationException();
	}

	public void apply(Project project) {
		project.getPlugins().apply(JavaPlugin.class);
		ExtensionContainer extensions = project.getExtensions();
		JavaPluginExtension javaPluginExtension = extensions.getByType(JavaPluginExtension.class);
		ConfigurationContainer configurations = project.getConfigurations();
		TaskContainer tasks = project.getTasks();
		DependencyHandler dependencies = project.getDependencies();
		ObjectFactory objects = project.getObjects();
		extensions.create("multiRelease", MultiReleaseExtension.class,
				javaPluginExtension.getSourceSets(),
				configurations,
				tasks,
				dependencies,
				objects);
	}
}
