/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.context.junit.jupiter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class demonstrates how to have a JUnit Jupiter extension managed as a
 * Spring bean in order to have dependencies injected into an extension from
 * a Spring {@code ApplicationContext}.
 *
 * @author Sam Brannen
 * @since 5.1
 */
@SpringJUnitConfig
@TestInstance(Lifecycle.PER_CLASS)
class SpringManagedJupiterExtensionTests {

	@Autowired
	@RegisterExtension
	TestTemplateInvocationContextProvider provider;


	@TestTemplate
	void testTemplate(String parameter) {
		assertTrue("foo".equals(parameter) || "bar".equals(parameter));
	}


	@Configuration
	static class Config {

		@Bean
		String foo() {
			return "foo";
		}

		@Bean
		String bar() {
			return "bar";
		}

		@Bean
		TestTemplateInvocationContextProvider provider(List<String> parameters) {
			return new StringInvocationContextProvider(parameters);
		}
	}

	private static class StringInvocationContextProvider implements TestTemplateInvocationContextProvider {

		private final List<String> parameters;


		StringInvocationContextProvider(List<String> parameters) {
			this.parameters = parameters;
		}

		@Override
		public boolean supportsTestTemplate(ExtensionContext context) {
			return true;
		}

		@Override
		public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
			return this.parameters.stream().map(this::invocationContext);
		}

		private TestTemplateInvocationContext invocationContext(String parameter) {
			return new TestTemplateInvocationContext() {

				@Override
				public String getDisplayName(int invocationIndex) {
					return parameter;
				}

				@Override
				public List<Extension> getAdditionalExtensions() {
					return Collections.singletonList(new ParameterResolver() {

						@Override
						public boolean supportsParameter(ParameterContext parameterContext,
								ExtensionContext extensionContext) {
							return parameterContext.getParameter().getType() == String.class;
						}

						@Override
						public Object resolveParameter(ParameterContext parameterContext,
								ExtensionContext extensionContext) {
							return parameter;
						}
					});
				}
			};
		}
	}

}
