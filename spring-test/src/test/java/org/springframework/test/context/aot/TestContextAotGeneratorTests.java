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

package org.springframework.test.context.aot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.SpringProperties;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.aot.TestContextAotGenerator.FAIL_ON_ERROR_PROPERTY_NAME;

/**
 * Tests for {@link TestContextAotGenerator}.
 *
 * @author Sam Brannen
 * @since 6.1
 */
class TestContextAotGeneratorTests {

	@BeforeEach
	@AfterEach
	void resetFlag() {
		SpringProperties.setProperty(FAIL_ON_ERROR_PROPERTY_NAME, null);
	}

	@Test
	void failOnErrorEnabledByDefault() {
		assertThat(createGenerator().failOnError).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "true", "  True\t" })
	void failOnErrorEnabledViaSpringProperty(String value) {
		SpringProperties.setProperty(FAIL_ON_ERROR_PROPERTY_NAME, value);
		assertThat(createGenerator().failOnError).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "false", "  False\t", "x" })
	void failOnErrorDisabledViaSpringProperty(String value) {
		SpringProperties.setProperty(FAIL_ON_ERROR_PROPERTY_NAME, value);
		assertThat(createGenerator().failOnError).isFalse();
	}

	@Test  // gh-34841
	void contextIsClosedAfterAotProcessing() {
		DemoTestContextAotGenerator generator = createGenerator();
		generator.processAheadOfTime(Stream.of(TestCase1.class, TestCase2.class));

		assertThat(generator.contexts)
				.allSatisfy(context -> assertThat(context.isClosed()).as("context is closed").isTrue());
	}


	private static DemoTestContextAotGenerator createGenerator() {
		return new DemoTestContextAotGenerator(new InMemoryGeneratedFiles());
	}


	private static class DemoTestContextAotGenerator extends TestContextAotGenerator {

		List<GenericApplicationContext> contexts = new ArrayList<>();

		DemoTestContextAotGenerator(GeneratedFiles generatedFiles) {
			super(generatedFiles);
		}

		@Override
		GenericApplicationContext loadContextForAotProcessing(
				MergedContextConfiguration mergedConfig) throws TestContextAotException {

			GenericApplicationContext context = super.loadContextForAotProcessing(mergedConfig);
			this.contexts.add(context);
			return context;
		}
	}

	@SpringJUnitConfig
	private static class TestCase1 {

		@Configuration(proxyBeanMethods = false)
		static class Config {
			// no beans
		}
	}

	@SpringJUnitConfig
	private static class TestCase2 {

		@Configuration(proxyBeanMethods = false)
		static class Config {
			// no beans
		}
	}

}
