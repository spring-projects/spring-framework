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

package org.springframework.scheduling.annotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

@SuppressWarnings("try")
class AbstractAsyncConfigurationTests {

	@DisplayName("Context should fail if 2 async configurers are defined")
	@Test
	void testTwoAsyncConfigurers() {
		assertThatExceptionOfType(BeansException.class).isThrownBy(
			() -> new AnnotationConfigApplicationContext(TwoAsyncConfigurers.class));
	}

	@DisplayName("Context should pass if 1 async configurer is defined")
	@Test
	void testOneAsyncConfigurer() {
		assertThatNoException().isThrownBy(() -> {
			try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(OneAsyncConfigurer.class)) {
				ctx.getId();
			}
		});
	}

	@DisplayName("Context should pass if no async configurer is defined")
	@Test
	void testNoAsyncConfigurer() {
		assertThatNoException().isThrownBy(() -> {
			try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(NoAsyncConfigurer.class)) {
				ctx.getId();
			}
		});
	}

	@DisplayName("Context should pass if primary async configurer win others")
	@Test
	void testPrimaryAsyncConfigurer() {
		assertThatNoException().isThrownBy(() -> {
			try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(PrimaryAsyncConfigurer.class)) {
				ctx.getId();
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
