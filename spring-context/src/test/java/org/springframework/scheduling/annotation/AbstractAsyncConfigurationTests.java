package org.springframework.scheduling.annotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("try")
class AbstractAsyncConfigurationTests {

	@DisplayName("Context should fail if 2 async configurers are defined")
	@Test
	void testTwoAsyncConfigurers() {
		assertThrows(BeansException.class, () -> {
			try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(TwoAsyncConfigurers.class)) {

			}
		});
	}

	@DisplayName("Context should pass if 1 async configurer is defined")
	@Test
	void testOneAsyncConfigurer() {
		assertDoesNotThrow(() -> {
			try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(OneAsyncConfigurer.class)) {

			}
		});
	}

	@DisplayName("Context should pass if no async configurer is defined")
	@Test
	void testNoAsyncConfigurer() {
		assertDoesNotThrow(() -> {
			try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(NoAsyncConfigurer.class)) {

			}
		});
	}

	@DisplayName("Context should pass if primary async configurer win others")
	@Test
	void testPrimaryAsyncConfigurer() {
		assertDoesNotThrow(() -> {
			try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(PrimaryAsyncConfigurer.class)) {

			}
		});
	}

	@Configuration
	static class TwoAsyncConfigurers extends AbstractAsyncConfiguration {

		@Bean
		AsyncConfigurer asyncConfigurer1() {
			return new AsyncConfigurer() {
			};
		}

		@Bean
		AsyncConfigurer asyncConfigurer2() {
			return new AsyncConfigurer() {
			};
		}
	}

	@Configuration
	static class OneAsyncConfigurer extends AbstractAsyncConfiguration {

		@Bean
		AsyncConfigurer asyncConfigurer() {
			return new AsyncConfigurer() {
			};
		}

	}

	@Configuration
	static class PrimaryAsyncConfigurer extends AbstractAsyncConfiguration {

		@Primary
		@Bean
		AsyncConfigurer asyncConfigurer1() {
			return new AsyncConfigurer() {
			};
		}

		@Bean
		AsyncConfigurer asyncConfigurer2() {
			return new AsyncConfigurer() {
			};
		}

	}

	@Configuration
	static class NoAsyncConfigurer extends AbstractAsyncConfiguration {
	}
}
