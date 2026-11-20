/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.build;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin;

import org.springframework.build.architecture.ArchitecturePlugin;

/**
 * Plugin to apply conventions to projects that are part of Spring Framework's build.
 * Conventions are applied in response to various plugins being applied.
 *
 * <p>When the {@link JavaBasePlugin} is applied, the conventions in {@link CheckstyleConventions},
 * {@link TestConventions} and {@link JavaConventions} are applied.
 * The {@link ArchitecturePlugin} plugin is also applied.
 * When the {@link KotlinBasePlugin} is applied, the conventions in {@link KotlinConventions}
 * are applied.
 *
 * @author Brian Clozel
 */
public class ConventionsPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		project.getExtensions().create("springFramework", SpringFrameworkExtension.class);
		new ArchitecturePlugin().apply(project);
		new CheckstyleConventions().apply(project);
		new JavaConventions().apply(project);
		new KotlinConventions().apply(project);
		new TestConventions().apply(project);
	}

}
