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

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.TestFrameworkOptions;
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions;
import org.gradle.testretry.TestRetryPlugin;
import org.gradle.testretry.TestRetryTaskExtension;
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest;

import java.util.Map;

/**
 * Conventions that are applied in the presence of the {@link JavaBasePlugin}. When the
 * plugin is applied:
 * <ul>
 * <li>The {@link TestRetryPlugin Test Retry} plugin is applied so that flaky tests
 * are retried 3 times when running on the CI server.
 * <li>Common test properties are configured
 * <li>The Mockito Java agent is set on test tasks.
 * </ul>
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 * @author Sam Brannen
 */
class TestConventions {

	void apply(Project project) {
		project.getPlugins().withType(JavaBasePlugin.class, (java) -> configureTestConventions(project));
	}

	private void configureTestConventions(Project project) {
		configureMockitoAgent(project);
		project.getTasks().withType(Test.class,
				test -> {
					configureTests(project, test);
					configureTestRetryPlugin(project, test);
				});
	}

	private void configureTests(Project project, Test test) {
		TestFrameworkOptions existingOptions = test.getOptions();
		test.useJUnitPlatform(options -> {
			if (existingOptions instanceof JUnitPlatformOptions junitPlatformOptions) {
				options.copyFrom(junitPlatformOptions);
			}
		});
		test.include("**/*Tests.class", "**/*Test.class");
		test.setSystemProperties(Map.of(
				"java.awt.headless", "true",
				"io.netty.leakDetection.level", "paranoid",
				"junit.platform.discovery.issue.severity.critical", "INFO"
		));
		if (project.hasProperty("testGroups")) {
			test.systemProperty("testGroups", project.getProperties().get("testGroups"));
		}
		test.jvmArgs(
				"--add-opens=java.base/java.lang=ALL-UNNAMED",
				"--add-opens=java.base/java.util=ALL-UNNAMED",
				"-Xshare:off"
		);
	}

	private void configureMockitoAgent(Project project) {
		if (project.hasProperty("mockitoVersion")) {
			String mockitoVersion = (String) project.getProperties().get("mockitoVersion");
			Configuration mockitoAgentConfig = project.getConfigurations().create("mockitoAgent");
			mockitoAgentConfig.setTransitive(false);
			Dependency mockitoCore = project.getDependencies().create("org.mockito:mockito-core:" + mockitoVersion);
			mockitoAgentConfig.getDependencies().add(mockitoCore);
			project.afterEvaluate(p -> {
				p.getTasks().withType(Test.class, test -> test.jvmArgs("-javaagent:" + mockitoAgentConfig.getAsPath()));
				project.getPlugins().withId("org.jetbrains.kotlin.jvm", plugin -> {
					project.getTasks().withType(KotlinJvmTest.class, kotlinTest -> {
						kotlinTest.jvmArgs("-javaagent:" + mockitoAgentConfig.getAsPath());
					});
				});
			});
		}
	}

	private void configureTestRetryPlugin(Project project, Test test) {
		project.getPlugins().withType(TestRetryPlugin.class, testRetryPlugin -> {
			TestRetryTaskExtension testRetry = test.getExtensions().getByType(TestRetryTaskExtension.class);
			testRetry.getFailOnPassedAfterRetry().set(true);
			testRetry.getMaxRetries().set(isCi() ? 3 : 0);
		});
	}

	private boolean isCi() {
		return Boolean.parseBoolean(System.getenv("CI"));
	}

}
