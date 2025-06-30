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

package org.springframework.build.architecture;

import java.util.ArrayList;
import java.util.List;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * {@link Plugin} for verifying a project's architecture.
 *
 * @author Andy Wilkinson
 */
public class ArchitecturePlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> registerTasks(project));
	}

	private void registerTasks(Project project) {
		JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
		List<TaskProvider<ArchitectureCheck>> architectureChecks = new ArrayList<>();
		for (SourceSet sourceSet : javaPluginExtension.getSourceSets()) {
			if (sourceSet.getName().contains("test")) {
				// skip test source sets.
				continue;
			}
			TaskProvider<ArchitectureCheck> checkArchitecture = project.getTasks()
					.register(taskName(sourceSet), ArchitectureCheck.class,
							(task) -> {
								task.setClasses(sourceSet.getOutput().getClassesDirs());
								task.getResourcesDirectory().set(sourceSet.getOutput().getResourcesDir());
								task.dependsOn(sourceSet.getProcessResourcesTaskName());
								task.setDescription("Checks the architecture of the classes of the " + sourceSet.getName()
										+ " source set.");
								task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
							});
			architectureChecks.add(checkArchitecture);
		}
		if (!architectureChecks.isEmpty()) {
			TaskProvider<Task> checkTask = project.getTasks().named(LifecycleBasePlugin.CHECK_TASK_NAME);
			checkTask.configure((check) -> check.dependsOn(architectureChecks));
		}
	}

	private static String taskName(SourceSet sourceSet) {
		return "checkArchitecture"
				+ sourceSet.getName().substring(0, 1).toUpperCase()
				+ sourceSet.getName().substring(1);
	}

}
