/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.basic;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.aot.samples.common.MessageService;
import org.springframework.test.context.aot.samples.management.ManagementConfiguration;
import org.springframework.test.context.env.YamlTestProperties;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base test class which declares {@link Nested @Nested} test classes
 * that will be inherited by concrete subclasses.
 *
 * @author Sam Brannen
 * @since 7.0
 */
@SpringJUnitConfig({BasicTestConfiguration.class, ManagementConfiguration.class})
@TestPropertySource(properties = "test.engine = jupiter")
@YamlTestProperties({
	"classpath:org/springframework/test/context/aot/samples/basic/test1.yaml",
	"classpath:org/springframework/test/context/aot/samples/basic/test2.yaml"
})
public abstract class AbstractSpringJupiterParameterizedClassTests {

	@Nested
	public class InheritedNestedTests {

		@Test
		void test(@Autowired ApplicationContext context, @Autowired MessageService messageService,
				@Value("${test.engine}") String testEngine, @Value("${foo}") String foo) {

			assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
			assertThat(foo).isEqualTo("${foo}");
			assertThat(testEngine).isEqualTo("jupiter");
			BasicSpringJupiterTests.assertEnvProperties(context);
		}

		@Nested
		@TestPropertySource(properties = "foo=quux")
		public class InheritedDoublyNestedTests {

			@Test
			void test(@Autowired ApplicationContext context, @Autowired MessageService messageService,
					@Value("${test.engine}") String testEngine, @Value("${foo}") String foo) {

				assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
				assertThat(foo).isEqualTo("quux");
				assertThat(testEngine).isEqualTo("jupiter");
				BasicSpringJupiterTests.assertEnvProperties(context);
			}
		}
	}

	// This is here to ensure that an inner class is only considered a nested test
	// class if it's annotated with @Nested.
	public class InheritedNotReallyNestedTests {
	}

}
