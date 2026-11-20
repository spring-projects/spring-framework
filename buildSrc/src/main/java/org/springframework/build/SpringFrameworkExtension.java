/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.build;

import java.util.Collections;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.process.CommandLineArgumentProvider;

public class SpringFrameworkExtension {

	private final Property<Boolean> enableJavaPreviewFeatures;

	public SpringFrameworkExtension(Project project) {
		this.enableJavaPreviewFeatures = project.getObjects().property(Boolean.class);
		project.getTasks().withType(JavaCompile.class).configureEach(javaCompile ->
				javaCompile.getOptions().getCompilerArgumentProviders().add(asArgumentProvider()));
		project.getTasks().withType(Test.class).configureEach(test ->
				test.getJvmArgumentProviders().add(asArgumentProvider()));

	}

	public Property<Boolean> getEnableJavaPreviewFeatures() {
		return this.enableJavaPreviewFeatures;
	}

	private CommandLineArgumentProvider asArgumentProvider() {
		return () -> {
			if (getEnableJavaPreviewFeatures().getOrElse(false)) {
				return List.of("--enable-preview");
			}
			return Collections.emptyList();
		};
	}
}
