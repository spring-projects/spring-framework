/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.build.hint;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.testing.Test;
import org.gradle.process.CommandLineArgumentProvider;

import java.util.Collections;

/**
 * {@link Plugin} that configures the {@code RuntimeHints} Java agent to test tasks.
 *
 * @author Brian Clozel
 * @author Sebastien Deleuze
 */
public class RuntimeHintsAgentPlugin implements Plugin<Project> {

	public static final String RUNTIMEHINTS_TEST_TASK = "runtimeHintsTest";
	private static final String EXTENSION_NAME = "runtimeHintsAgent";


	@Override
	public void apply(Project project) {

		project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
			RuntimeHintsAgentExtension agentExtension = project.getExtensions().create(EXTENSION_NAME,
					RuntimeHintsAgentExtension.class, project.getObjects());
			Test agentTest = project.getTasks().create(RUNTIMEHINTS_TEST_TASK, Test.class, test -> {
				test.useJUnitPlatform(options -> {
					options.includeTags("RuntimeHintsTests");
				});
				test.include("**/*Tests.class", "**/*Test.class");
				test.systemProperty("java.awt.headless", "true");
				test.systemProperty("org.graalvm.nativeimage.imagecode", "runtime");
			});
			project.afterEvaluate(p -> {
				agentTest.getJvmArgumentProviders().add(createRuntimeHintsAgentArgumentProvider(project, agentExtension));
			});
			project.getTasks().getByName("check", task -> task.dependsOn(agentTest));
		});
	}

	private static RuntimeHintsAgentArgumentProvider createRuntimeHintsAgentArgumentProvider(Project project, RuntimeHintsAgentExtension agentExtension) {
		Jar jar = project.getRootProject().project("spring-core-test").getTasks().withType(Jar.class).named("jar").get();
		RuntimeHintsAgentArgumentProvider agentArgumentProvider = project.getObjects().newInstance(RuntimeHintsAgentArgumentProvider.class);
		agentArgumentProvider.getAgentJar().from(jar.getArchiveFile());
		agentArgumentProvider.getJavaAgentArgument().set(agentExtension.asJavaAgentArgument());
		return agentArgumentProvider;
	}

	interface RuntimeHintsAgentArgumentProvider extends CommandLineArgumentProvider {

		@Classpath
		ConfigurableFileCollection getAgentJar();

		@Input
		Property<String> getJavaAgentArgument();

		@Override
		default Iterable<String> asArguments() {
			return Collections.singleton("-javaagent:" + getAgentJar().getSingleFile() + "=" + getJavaAgentArgument().get());
		}
	}
}
