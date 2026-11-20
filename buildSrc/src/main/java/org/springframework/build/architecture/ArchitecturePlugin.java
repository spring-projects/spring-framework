/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
