/*
 * Copyright 2002-2022 the original author or authors.
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
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.testing.Test;
import org.gradle.testretry.TestRetryPlugin;
import org.gradle.testretry.TestRetryTaskExtension;

/**
 * Conventions that are applied in the presence of the {@link JavaBasePlugin}. When the
 * plugin is applied:
 * <ul>
 * <li>The {@link TestRetryPlugin Test Retry} plugins is applied so that flaky tests
 * are retried 3 times when running on the CI.
 * </ul>
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 */
class TestConventions {

	void apply(Project project) {
		project.getPlugins().withType(JavaBasePlugin.class, (java) -> configureTestConventions(project));
	}

	private void configureTestConventions(Project project) {
		project.getPlugins().apply(TestRetryPlugin.class);
		project.getTasks().withType(Test.class,
				(test) -> project.getPlugins().withType(TestRetryPlugin.class, (testRetryPlugin) -> {
					TestRetryTaskExtension testRetry = test.getExtensions().getByType(TestRetryTaskExtension.class);
					testRetry.getFailOnPassedAfterRetry().set(true);
					testRetry.getMaxRetries().set(isCi() ? 3 : 0);
				}));
	}

	private boolean isCi() {
		return Boolean.parseBoolean(System.getenv("CI"));
	}

}
