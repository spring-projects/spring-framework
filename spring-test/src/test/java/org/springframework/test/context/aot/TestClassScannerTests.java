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

package org.springframework.test.context.aot;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterImportedConfigTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterSharedConfigTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringTestNGTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringVintageTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestClassScanner}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class TestClassScannerTests extends AbstractAotTests {

	@Test
	void scanBasicTestClasses() {
		assertThat(scan("org.springframework.test.context.aot.samples.basic"))
			.containsExactlyInAnyOrder(
				BasicSpringJupiterImportedConfigTests.class,
				BasicSpringJupiterSharedConfigTests.class,
				BasicSpringJupiterTests.class,
				BasicSpringJupiterTests.NestedTests.class,
				BasicSpringVintageTests.class,
				BasicSpringTestNGTests.class
			);
	}

	@Test
	void scanTestSuitesForJupiter() {
		assertThat(scan("org.springframework.test.context.aot.samples.suites.jupiter"))
			.containsExactlyInAnyOrder(BasicSpringJupiterImportedConfigTests.class,
				BasicSpringJupiterSharedConfigTests.class, BasicSpringJupiterTests.class,
				BasicSpringJupiterTests.NestedTests.class);
	}

	@Test
	void scanTestSuitesForVintage() {
		assertThat(scan("org.springframework.test.context.aot.samples.suites.vintage"))
			.containsExactly(BasicSpringVintageTests.class);
	}

	@Test
	void scanTestSuitesForTestNG() {
		assertThat(scan("org.springframework.test.context.aot.samples.suites.testng"))
			.containsExactly(BasicSpringTestNGTests.class);
	}

	@Test
	void scanTestSuitesForAllTestEngines() {
		assertThat(scan("org.springframework.test.context.aot.samples.suites.all"))
			.containsExactlyInAnyOrder(
				BasicSpringJupiterImportedConfigTests.class,
				BasicSpringJupiterSharedConfigTests.class,
				BasicSpringJupiterTests.class,
				BasicSpringJupiterTests.NestedTests.class,
				BasicSpringVintageTests.class,
				BasicSpringTestNGTests.class
			);
	}

	@Test
	void scanTestSuitesWithNestedSuites() {
		assertThat(scan("org.springframework.test.context.aot.samples.suites.nested"))
			.containsExactlyInAnyOrder(
				BasicSpringJupiterImportedConfigTests.class,
				BasicSpringJupiterSharedConfigTests.class,
				BasicSpringJupiterTests.class,
				BasicSpringJupiterTests.NestedTests.class,
				BasicSpringVintageTests.class
			);
	}

	@Test
	void scanEntireSpringTestModule() {
		assertThat(scan()).hasSizeGreaterThan(400);
	}

}
