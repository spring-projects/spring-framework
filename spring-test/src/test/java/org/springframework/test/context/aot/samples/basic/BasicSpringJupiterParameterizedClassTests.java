/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.basic;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.aot.samples.common.MessageService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ParameterizedClass @ParameterizedClass} variant of {@link BasicSpringJupiterTests}.
 *
 * @author Sam Brannen
 * @since 7.0
 */
@ParameterizedClass
@ValueSource(strings = {"foo", "bar"})
public class BasicSpringJupiterParameterizedClassTests extends AbstractSpringJupiterParameterizedClassTests {

	private final String parameterizedString;

	@Resource
	Integer magicNumber;


	BasicSpringJupiterParameterizedClassTests(String parameterizedString) {
		this.parameterizedString = parameterizedString;
	}


	@Test
	void test(@Autowired ApplicationContext context, @Autowired MessageService messageService,
			@Value("${test.engine}") String testEngine) {
		assertThat("foo".equals(parameterizedString) || "bar".equals(parameterizedString)).isTrue();
		assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
		assertThat(testEngine).isEqualTo("jupiter");
		assertThat(magicNumber).isEqualTo(42);
		BasicSpringJupiterTests.assertEnvProperties(context);
	}

	@Nested
	@TestPropertySource(properties = "foo=bar")
	@ActiveProfiles(resolver = SpanishActiveProfilesResolver.class)
	public class NestedTests {

		@Test
		void test(@Autowired ApplicationContext context, @Autowired MessageService messageService,
				@Value("${test.engine}") String testEngine, @Value("${foo}") String foo) {
			assertThat("foo".equals(parameterizedString) || "bar".equals(parameterizedString)).isTrue();
			assertThat(messageService.generateMessage()).isEqualTo("¡Hola, AOT!");
			assertThat(foo).isEqualTo("bar");
			assertThat(testEngine).isEqualTo("jupiter");
			BasicSpringJupiterTests.assertEnvProperties(context);
		}

		@Nested
		@TestPropertySource(properties = "foo=quux")
		public class DoublyNestedTests {

			@Test
			void test(@Autowired ApplicationContext context, @Autowired MessageService messageService,
					@Value("${test.engine}") String testEngine, @Value("${foo}") String foo) {
				assertThat("foo".equals(parameterizedString) || "bar".equals(parameterizedString)).isTrue();
				assertThat(messageService.generateMessage()).isEqualTo("¡Hola, AOT!");
				assertThat(foo).isEqualTo("quux");
				assertThat(testEngine).isEqualTo("jupiter");
				BasicSpringJupiterTests.assertEnvProperties(context);
			}
		}
	}

	// This is here to ensure that an inner class is only considered a nested test
	// class if it's annotated with @Nested.
	public class NotReallyNestedTests {
	}

}
