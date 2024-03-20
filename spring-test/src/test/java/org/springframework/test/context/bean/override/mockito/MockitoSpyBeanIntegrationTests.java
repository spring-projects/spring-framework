/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.mockito.Mockito;

import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@SpringJUnitConfig(MockitoBeanIntegrationTests.Config.class)
public class MockitoSpyBeanIntegrationTests {

	@MockitoSpyBean
	ExampleService field;

	@MockitoSpyBean
	ExampleService nestedField;

	@MockitoSpyBean(name = "field")
	ExampleService renamed1;

	@MockitoSpyBean(name = "nestedField")
	ExampleService renamed2;

	@Test
	void fieldHasOverride(ApplicationContext ctx) {
		assertThat(ctx.getBean("field"))
				.isInstanceOf(ExampleService.class)
				.satisfies(o -> assertThat(Mockito.mockingDetails(o).isSpy())
						.as("isSpy").isTrue())
				.isSameAs(this.field);

		assertThat(this.field.greeting()).as("spied greeting")
				.isEqualTo("Hello Field");

	}

	@Test
	void fieldWithBeanNameHasOverride(ApplicationContext ctx) {
		assertThat(ctx.getBean("field"))
				.isInstanceOf(ExampleService.class)
				.satisfies(o -> assertThat(Mockito.mockingDetails(o).isSpy())
						.as("isSpy").isTrue())
				.isSameAs(this.renamed1);

		assertThat(this.field.greeting()).as("spied greeting")
				.isEqualTo("Hello Field");
	}

	@Test
	void failWhenBeanNotPresentFieldName() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(Failure1.class))//
				.execute();

		assertThat(results.allEvents().failed().stream()).hasSize(1).first()
				.satisfies(e -> assertThat(e.getRequiredPayload(TestExecutionResult.class)
						.getThrowable()).get(THROWABLE)
						.cause()
						.isInstanceOf(IllegalStateException.class)
						.hasMessage("Unable to override bean 'notPresent' by wrapping," +
										" no existing bean instance by this name of type %s",
								ExampleService.class.getName()));
	}

	@Test
	void failWhenBeanNotPresentExplicitName() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(Failure2.class))//
				.execute();

		assertThat(results.allEvents().failed().stream()).hasSize(1).first()
				.satisfies(e -> assertThat(e.getRequiredPayload(TestExecutionResult.class)
						.getThrowable()).get(THROWABLE)
						.cause()
						.isInstanceOf(IllegalStateException.class)
						.hasMessage("Unable to override bean 'notPresentAtAll' by wrapping," +
								" no existing bean instance by this name of type %s",
								ExampleService.class.getName()));
	}

	@Nested
	@DisplayName("With @MockitoSpyBean on enclosing class")
	class MockitoBeanNested {

		@Test
		void fieldHasOverride(ApplicationContext ctx) {
			assertThat(ctx.getBean("nestedField"))
					.isInstanceOf(ExampleService.class)
					.satisfies(o -> assertThat(Mockito.mockingDetails(o).isSpy())
							.as("isSpy").isTrue())
					.isSameAs(MockitoSpyBeanIntegrationTests.this.nestedField);

			assertThat(MockitoSpyBeanIntegrationTests.this.nestedField.greeting())
					.as("spied greeting")
					.isEqualTo("Hello Nested Field");
		}

		@Test
		void fieldWithBeanNameHasOverride(ApplicationContext ctx) {
			assertThat(ctx.getBean("nestedField"))
					.isInstanceOf(ExampleService.class)
					.satisfies(o -> assertThat(Mockito.mockingDetails(o).isSpy())
							.as("isSpy").isTrue())
					.isSameAs(MockitoSpyBeanIntegrationTests.this.renamed2);


			assertThat(MockitoSpyBeanIntegrationTests.this.renamed2.greeting())
					.as("spied greeting")
					.isEqualTo("Hello Nested Field");
		}
	}


	@SpringJUnitConfig
	static class Failure1 {

		@MockitoSpyBean
		ExampleService notPresent;

		@Test
		void ignored() { }

	}

	@SpringJUnitConfig
	static class Failure2 {

		@MockitoSpyBean(name = "notPresentAtAll")
		ExampleService field;

		@Test
		void ignored() { }
	}
}
