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

package org.springframework.context.nested_config;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests ensuring that parent conditions take precedence over nested conditions
 * @author Edoardo Patti
 * since 6.1
 */
public class ParentConditionsTests {

	@Test
	void registeredContextLoads() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(NestedConfiguration.class);
		assertThat(ctx.getBeansOfType(NestedConfiguration.Client.class).size()).isEqualTo(1);
		assertThat(ctx.getBean(NestedConfiguration.Client.class)).isInstanceOf(NestedConfiguration.ThirdClient.class);
	}

	@Test
	void scannedContextLoads() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext("org.springframework.context.nested_config");
		assertThat(ctx.getBeansOfType(NestedConfiguration.Client.class).size()).isEqualTo(1);
		assertThat(ctx.getBean(NestedConfiguration.Client.class)).isInstanceOf(NestedConfiguration.ThirdClient.class);
	}

	@Configuration
	static class NestedConfiguration {

		@Configuration
		@Conditional(Disabled.class)
		static class DisabledConfiguration {

			@Configuration
			@Conditional(Enabled.class)
			static class FirstClientConfiguration {

				@Bean
				public Client firstClient() {
					return new FirstClient();
				}
			}

			@Configuration
			@Conditional(Disabled.class)
			static class SecondClientConfiguration {

				@Bean
				public Client secondClient() {
					return new SecondClient();
				}
			}
		}

		@Configuration
		@Conditional(Enabled.class)
		static class ThirdClientConfiguration {

			@Bean
			public Client thirdClient() {
				return new ThirdClient();
			}
		}

		static class Enabled implements Condition {

			@Override
			public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
				return true;
			}
		}

		static class Disabled implements Condition {

			@Override
			public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
				return false;
			}
		}

		public static interface Client { }

		public static class FirstClient implements Client { }

		public static class SecondClient implements Client { }

		public static class ThirdClient implements Client { }
	}
}