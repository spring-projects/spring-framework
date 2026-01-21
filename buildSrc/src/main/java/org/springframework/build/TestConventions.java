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

import java.util.Map;

/**
 * Conventions that are applied in the presence of the {@link JavaBasePlugin}. When the
 * plugin is applied:
 * <ul>
 * <li>The {@link TestRetryPlugin Test Retry} plugin is applied so that flaky tests
 * are retried 3 times when running on the CI server.
 * <li>Common test properties are configured
 * <li>The ByteBuddy Java agent is configured on test tasks.
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
		configureByteBuddyAgent(project);
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

	private void configureByteBuddyAgent(Project project) {
		if (project.hasProperty("byteBuddyVersion")) {
			String byteBuddyVersion = (String) project.getProperties().get("byteBuddyVersion");
			Configuration byteBuddyAgentConfig = project.getConfigurations().create("byteBuddyAgentConfig");
			byteBuddyAgentConfig.setTransitive(false);
			Dependency byteBuddyAgent = project.getDependencies().create("net.bytebuddy:byte-buddy-agent:" + byteBuddyVersion);
			byteBuddyAgentConfig.getDependencies().add(byteBuddyAgent);
			project.afterEvaluate(p -> {
				p.getTasks().withType(Test.class, test -> test
						.jvmArgs("-javaagent:" + byteBuddyAgentConfig.getAsPath()));
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
