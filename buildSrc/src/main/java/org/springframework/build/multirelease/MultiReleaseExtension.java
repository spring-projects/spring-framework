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

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * @author Cedric Champeau
 * @author Brian Clozel
 */
public abstract class MultiReleaseExtension {
	private final TaskContainer tasks;
	private final SourceSetContainer sourceSets;
	private final DependencyHandler dependencies;
	private final ObjectFactory objects;
	private final ConfigurationContainer configurations;

	@Inject
	public MultiReleaseExtension(SourceSetContainer sourceSets,
								 ConfigurationContainer configurations,
								 TaskContainer tasks,
								 DependencyHandler dependencies,
								 ObjectFactory objectFactory) {
		this.sourceSets = sourceSets;
		this.configurations = configurations;
		this.tasks = tasks;
		this.dependencies = dependencies;
		this.objects = objectFactory;
	}

	public void releaseVersions(int... javaVersions) {
		releaseVersions("src/main/", "src/test/", javaVersions);
	}

	private void releaseVersions(String mainSourceDirectory, String testSourceDirectory, int... javaVersions) {
		for (int javaVersion : javaVersions) {
			addLanguageVersion(javaVersion, mainSourceDirectory, testSourceDirectory);
		}
	}

	private void addLanguageVersion(int javaVersion, String mainSourceDirectory, String testSourceDirectory) {
		String javaN = "java" + javaVersion;

		SourceSet langSourceSet = sourceSets.create(javaN, srcSet -> srcSet.getJava().srcDir(mainSourceDirectory + javaN));
		SourceSet testSourceSet = sourceSets.create(javaN + "Test", srcSet -> srcSet.getJava().srcDir(testSourceDirectory + javaN));
		SourceSet sharedSourceSet = sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME);
		SourceSet sharedTestSourceSet = sourceSets.findByName(SourceSet.TEST_SOURCE_SET_NAME);

		FileCollection mainClasses = objects.fileCollection().from(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput().getClassesDirs());
		dependencies.add(javaN + "Implementation", mainClasses);

		tasks.named(langSourceSet.getCompileJavaTaskName(), JavaCompile.class, task ->
				task.getOptions().getRelease().set(javaVersion)
		);
		tasks.named(testSourceSet.getCompileJavaTaskName(), JavaCompile.class, task ->
				task.getOptions().getRelease().set(javaVersion)
		);

		TaskProvider<Test> testTask = createTestTask(javaVersion, testSourceSet, sharedTestSourceSet, langSourceSet, sharedSourceSet);
		tasks.named("check", task -> task.dependsOn(testTask));

		configureMultiReleaseJar(javaVersion, langSourceSet);
	}

	private TaskProvider<Test> createTestTask(int javaVersion, SourceSet testSourceSet, SourceSet sharedTestSourceSet, SourceSet langSourceSet, SourceSet sharedSourceSet) {
		Configuration testImplementation = configurations.getByName(testSourceSet.getImplementationConfigurationName());
		testImplementation.extendsFrom(configurations.getByName(sharedTestSourceSet.getImplementationConfigurationName()));
		Configuration testCompileOnly = configurations.getByName(testSourceSet.getCompileOnlyConfigurationName());
		testCompileOnly.extendsFrom(configurations.getByName(sharedTestSourceSet.getCompileOnlyConfigurationName()));
		testCompileOnly.getDependencies().add(dependencies.create(langSourceSet.getOutput().getClassesDirs()));
		testCompileOnly.getDependencies().add(dependencies.create(sharedSourceSet.getOutput().getClassesDirs()));

		Configuration testRuntimeClasspath = configurations.getByName(testSourceSet.getRuntimeClasspathConfigurationName());
		// so here's the deal. MRjars are JARs! Which means that to execute tests, we need
		// the JAR on classpath, not just classes + resources as Gradle usually does
		testRuntimeClasspath.getAttributes()
				.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));

		TaskProvider<Test> testTask = tasks.register("java" + javaVersion + "Test", Test.class, test -> {
			test.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);

			ConfigurableFileCollection testClassesDirs = objects.fileCollection();
			testClassesDirs.from(testSourceSet.getOutput());
			testClassesDirs.from(sharedTestSourceSet.getOutput());
			test.setTestClassesDirs(testClassesDirs);
			ConfigurableFileCollection classpath = objects.fileCollection();
			// must put the MRJar first on classpath
			classpath.from(tasks.named("jar"));
			// then we put the specific test sourceset tests, so that we can override
			// the shared versions
			classpath.from(testSourceSet.getOutput());

			// then we add the shared tests
			classpath.from(sharedTestSourceSet.getRuntimeClasspath());
			test.setClasspath(classpath);
		});
		return testTask;
	}

	private void configureMultiReleaseJar(int version, SourceSet languageSourceSet) {
		tasks.named("jar", Jar.class, jar -> {
			jar.into("META-INF/versions/" + version, s -> s.from(languageSourceSet.getOutput()));
			Attributes attributes = jar.getManifest().getAttributes();
			attributes.put("Multi-Release", "true");
		});
	}

}
