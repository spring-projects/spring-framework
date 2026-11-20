/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.aot.samples.basic.AbstractSpringJupiterParameterizedClassTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterImportedConfigTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterParameterizedClassTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterSharedConfigTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringTestNGTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringVintageTests;
import org.springframework.test.context.aot.samples.basic.DisabledInAotProcessingTests;
import org.springframework.test.context.aot.samples.basic.DisabledInAotRuntimeClassLevelTests;
import org.springframework.test.context.aot.samples.basic.DisabledInAotRuntimeMethodLevelTests;

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
				BasicSpringJupiterParameterizedClassTests.class,
				AbstractSpringJupiterParameterizedClassTests.InheritedNestedTests.class,
				AbstractSpringJupiterParameterizedClassTests.InheritedNestedTests.InheritedDoublyNestedTests.class,
				BasicSpringJupiterParameterizedClassTests.NestedTests.class,
				BasicSpringJupiterParameterizedClassTests.NestedTests.DoublyNestedTests.class,
				BasicSpringTestNGTests.class,
				BasicSpringVintageTests.class,
				DisabledInAotProcessingTests.class,
				DisabledInAotRuntimeClassLevelTests.class,
				DisabledInAotRuntimeMethodLevelTests.class
			);
	}

	@Test
	void scanTestSuitesForJupiter() {
		assertThat(scan("org.springframework.test.context.aot.samples.suites.jupiter"))
			.containsExactlyInAnyOrder(
				BasicSpringJupiterImportedConfigTests.class,
				BasicSpringJupiterSharedConfigTests.class,
				BasicSpringJupiterTests.class,
				BasicSpringJupiterTests.NestedTests.class,
				BasicSpringJupiterParameterizedClassTests.class,
				AbstractSpringJupiterParameterizedClassTests.InheritedNestedTests.class,
				AbstractSpringJupiterParameterizedClassTests.InheritedNestedTests.InheritedDoublyNestedTests.class,
				BasicSpringJupiterParameterizedClassTests.NestedTests.class,
				BasicSpringJupiterParameterizedClassTests.NestedTests.DoublyNestedTests.class,
				DisabledInAotProcessingTests.class,
				DisabledInAotRuntimeClassLevelTests.class,
				DisabledInAotRuntimeMethodLevelTests.class
			);
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
				BasicSpringJupiterParameterizedClassTests.class,
				AbstractSpringJupiterParameterizedClassTests.InheritedNestedTests.class,
				AbstractSpringJupiterParameterizedClassTests.InheritedNestedTests.InheritedDoublyNestedTests.class,
				BasicSpringJupiterParameterizedClassTests.NestedTests.class,
				BasicSpringJupiterParameterizedClassTests.NestedTests.DoublyNestedTests.class,
				BasicSpringVintageTests.class,
				BasicSpringTestNGTests.class,
				DisabledInAotProcessingTests.class,
				DisabledInAotRuntimeClassLevelTests.class,
				DisabledInAotRuntimeMethodLevelTests.class
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
				BasicSpringJupiterParameterizedClassTests.class,
				AbstractSpringJupiterParameterizedClassTests.InheritedNestedTests.class,
				AbstractSpringJupiterParameterizedClassTests.InheritedNestedTests.InheritedDoublyNestedTests.class,
				BasicSpringJupiterParameterizedClassTests.NestedTests.class,
				BasicSpringJupiterParameterizedClassTests.NestedTests.DoublyNestedTests.class,
				BasicSpringVintageTests.class,
				DisabledInAotProcessingTests.class,
				DisabledInAotRuntimeClassLevelTests.class,
				DisabledInAotRuntimeMethodLevelTests.class
			);
	}

	@Test
	void scanEntireSpringTestModule() {
		assertThat(scan()).hasSizeGreaterThan(400);
	}

}
