/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * JUnit Platform based test suite for tests that involve the Spring TestContext
 * Framework and dynamic properties.
 *
 * <p><strong>This suite is only intended to be used manually within an IDE.</strong>
 *
 * <h3>Logging Configuration</h3>
 *
 * <p>In order for our log4j2 configuration to be used in an IDE, you must
 * set the following system property before running any tests &mdash; for
 * example, in <em>Run Configurations</em> in Eclipse.
 *
 * <pre style="code">
 * -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager
 * </pre>
 *
 * @author Sam Brannen
 * @since 6.2
 */
@Suite
@SelectClasses(
	value = {
		DynamicPropertyRegistryIntegrationTests.class,
		DynamicPropertySourceIntegrationTests.class
	},
	names = {
		"org.springframework.test.context.junit.jupiter.nested.DynamicPropertySourceNestedTests",
		"org.springframework.test.context.support.DefaultTestPropertySourcesIntegrationTests",
		"org.springframework.test.context.support.DynamicPropertiesContextCustomizerFactoryTests",
		"org.springframework.test.context.support.DynamicValuesPropertySourceTests"
	}
)
class DynamicPropertiesTestSuite {
}
