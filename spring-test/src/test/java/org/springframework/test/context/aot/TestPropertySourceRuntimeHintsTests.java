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

package org.springframework.test.context.aot;

import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.env.YamlTestProperties;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for registering run-time hints for {@code @TestPropertySource}, tested
 * via the {@link TestContextAotGenerator}.
 *
 * @author Sam Brannen
 * @since 6.1
 */
class TestPropertySourceRuntimeHintsTests extends AbstractAotTests {

	private final RuntimeHints runtimeHints = new RuntimeHints();

	private final TestContextAotGenerator generator =
			new TestContextAotGenerator(new InMemoryGeneratedFiles(), this.runtimeHints);


	@Test
	void testPropertySourceWithClassPathStarLocationPattern() {
		Class<?> testClass = ClassPathStarLocationPatternTestCase.class;

		// We can effectively only assert that an exception is not thrown; however,
		// a WARN-level log message similar to the following should be logged.
		//
		// Runtime hint registration is not supported for the 'classpath*:' prefix or
		// wildcards in @TestPropertySource locations. Please manually register a resource
		// hint for each location represented by 'classpath*:**/aot/samples/basic/test?.yaml'.
		assertThatNoException().isThrownBy(() -> this.generator.processAheadOfTime(Stream.of(testClass)));

		// But we can also ensure that a resource hint was not registered.
		assertThat(resource("org/springframework/test/context/aot/samples/basic/test1.yaml")).rejects(runtimeHints);
	}

	@Test
	void testPropertySourceWithWildcardLocationPattern() {
		Class<?> testClass = WildcardLocationPatternTestCase.class;

		// We can effectively only assert that an exception is not thrown; however,
		// a WARN-level log message similar to the following should be logged.
		//
		// Runtime hint registration is not supported for the 'classpath*:' prefix or
		// wildcards in @TestPropertySource locations. Please manually register a resource
		// hint for each location represented by 'classpath:org/springframework/test/context/aot/samples/basic/test?.yaml'.
		assertThatNoException().isThrownBy(() -> this.generator.processAheadOfTime(Stream.of(testClass)));

		// But we can also ensure that a resource hint was not registered.
		assertThat(resource("org/springframework/test/context/aot/samples/basic/test1.yaml")).rejects(runtimeHints);
	}

	private static Predicate<RuntimeHints> resource(String location) {
		return RuntimeHintsPredicates.resource().forResource(location);
	}


	@SpringJUnitConfig(Config.class)
	@YamlTestProperties("classpath*:**/aot/samples/basic/test?.yaml")
	static class ClassPathStarLocationPatternTestCase {
	}

	@SpringJUnitConfig(Config.class)
	@YamlTestProperties("classpath:org/springframework/test/context/aot/samples/basic/test?.yaml")
	static class WildcardLocationPatternTestCase {
	}

	@Configuration
	static class Config {
	}

}
