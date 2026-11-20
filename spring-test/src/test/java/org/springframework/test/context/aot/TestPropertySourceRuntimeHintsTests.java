/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class TestPropertySourceRuntimeHintsTests {

	private final RuntimeHints runtimeHints = new RuntimeHints();

	private final TestContextAotGenerator generator =
			new TestContextAotGenerator(new InMemoryGeneratedFiles(), this.runtimeHints);


	@Test
	void propertySourceWithClassPathStarLocationPattern() {
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
	void propertySourceWithWildcardLocationPattern() {
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

	@Configuration(proxyBeanMethods = false)
	static class Config {
	}

}
