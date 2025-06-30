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

package org.springframework.build;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

/**
 * {@link Plugin} that applies conventions for compiling Java sources in Spring Framework.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 * @author Sebastien Deleuze
 */
public class JavaConventions {

	private static final List<String> COMPILER_ARGS;

	private static final List<String> TEST_COMPILER_ARGS;

	/**
	 * The Java version we should use as the JVM baseline for building the project.
	 * <p>NOTE: If you update this value, you should also update the value used in
	 * the {@code javadoc} task in {@code framework-api.gradle}.
	 */
	private static final JavaLanguageVersion DEFAULT_LANGUAGE_VERSION = JavaLanguageVersion.of(24);

	/**
	 * The Java version we should use as the baseline for the compiled bytecode
	 * (the "-release" compiler argument).
	 */
	private static final JavaLanguageVersion DEFAULT_RELEASE_VERSION = JavaLanguageVersion.of(17);

	static {
		List<String> commonCompilerArgs = List.of(
				"-Xlint:serial", "-Xlint:cast", "-Xlint:classfile", "-Xlint:dep-ann",
				"-Xlint:divzero", "-Xlint:empty", "-Xlint:finally", "-Xlint:overrides",
				"-Xlint:path", "-Xlint:processing", "-Xlint:static", "-Xlint:try", "-Xlint:-options",
				"-parameters"
		);
		COMPILER_ARGS = new ArrayList<>();
		COMPILER_ARGS.addAll(commonCompilerArgs);
		COMPILER_ARGS.addAll(List.of(
				"-Xlint:varargs", "-Xlint:fallthrough", "-Xlint:rawtypes", "-Xlint:deprecation",
				"-Xlint:unchecked", "-Werror"
		));
		TEST_COMPILER_ARGS = new ArrayList<>();
		TEST_COMPILER_ARGS.addAll(commonCompilerArgs);
		TEST_COMPILER_ARGS.addAll(List.of("-Xlint:-varargs", "-Xlint:-fallthrough", "-Xlint:-rawtypes",
				"-Xlint:-deprecation", "-Xlint:-unchecked"));
	}

	public void apply(Project project) {
		project.getPlugins().withType(JavaBasePlugin.class, javaPlugin -> {
			applyToolchainConventions(project);
			applyJavaCompileConventions(project);
		});
	}

	/**
	 * Configure the Toolchain support for the project.
	 * @param project the current project
	 */
	private static void applyToolchainConventions(Project project) {
		project.getExtensions().getByType(JavaPluginExtension.class).toolchain(toolchain -> {
			toolchain.getLanguageVersion().set(DEFAULT_LANGUAGE_VERSION);
		});
	}

	/**
	 * Apply the common Java compiler options for main sources, test fixture sources, and
	 * test sources.
	 * @param project the current project
	 */
	private void applyJavaCompileConventions(Project project) {
		project.afterEvaluate(p -> {
			p.getTasks().withType(JavaCompile.class)
					.matching(compileTask -> compileTask.getName().startsWith(JavaPlugin.COMPILE_JAVA_TASK_NAME))
					.forEach(compileTask -> {
						compileTask.getOptions().setCompilerArgs(COMPILER_ARGS);
						compileTask.getOptions().setEncoding("UTF-8");
						setJavaRelease(compileTask);
					});
			p.getTasks().withType(JavaCompile.class)
					.matching(compileTask -> compileTask.getName().startsWith(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME)
							|| compileTask.getName().equals("compileTestFixturesJava"))
					.forEach(compileTask -> {
						compileTask.getOptions().setCompilerArgs(TEST_COMPILER_ARGS);
						compileTask.getOptions().setEncoding("UTF-8");
						setJavaRelease(compileTask);
					});

		});
	}

	/**
	 * We should pick the {@link #DEFAULT_RELEASE_VERSION} for all compiled classes,
	 * unless the current task is compiling multi-release JAR code with a higher version.
	 */
	private void setJavaRelease(JavaCompile task) {
		int defaultVersion = DEFAULT_RELEASE_VERSION.asInt();
		int releaseVersion = defaultVersion;
		int compilerVersion = task.getJavaCompiler().get().getMetadata().getLanguageVersion().asInt();
		for (int version = defaultVersion ; version <= compilerVersion ; version++) {
			if (task.getName().contains("Java" + version)) {
				releaseVersion = version;
				break;
			}
		}
		task.getOptions().getRelease().set(releaseVersion);
	}

}
