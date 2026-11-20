/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.build.optional;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSetContainer;

/**
 * A {@code Plugin} that adds support for Maven-style optional dependencies. Creates a new
 * {@code optional} configuration. The {@code optional} configuration is part of the
 * project's compile and runtime classpaths but does not affect the classpath of
 * dependent projects.
 *
 * @author Andy Wilkinson
 */
public class OptionalDependenciesPlugin implements Plugin<Project> {

	/**
	 * Name of the {@code optional} configuration.
	 */
	public static final String OPTIONAL_CONFIGURATION_NAME = "optional";

	@Override
	public void apply(Project project) {
		Configuration optional = project.getConfigurations().create(OPTIONAL_CONFIGURATION_NAME);
		optional.setCanBeConsumed(false);
		optional.setCanBeResolved(false);
		project.getPlugins().withType(JavaBasePlugin.class, (javaBasePlugin) -> {
			SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class)
					.getSourceSets();
			sourceSets.all((sourceSet) -> {
				project.getConfigurations().getByName(sourceSet.getCompileClasspathConfigurationName()).extendsFrom(optional);
				project.getConfigurations().getByName(sourceSet.getRuntimeClasspathConfigurationName()).extendsFrom(optional);
			});
		});
	}

}
