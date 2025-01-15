/*
 * Copyright 2002-2025 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.bean.override.BeanOverrideHandler;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link MockitoBeanOverrideProcessor}.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
class MockitoBeanOverrideProcessorTests {

	private final MockitoBeanOverrideProcessor processor = new MockitoBeanOverrideProcessor();


	@Nested
	class CreateHandlerTests {

		private final Field field = ReflectionUtils.findField(TestCase.class, "number");


		@Test
		void mockAnnotationCreatesMockitoBeanOverrideHandler() {
			MockitoBean annotation = AnnotationUtils.synthesizeAnnotation(MockitoBean.class);
			BeanOverrideHandler object = processor.createHandler(annotation, TestCase.class, field);

			assertThat(object).isExactlyInstanceOf(MockitoBeanOverrideHandler.class);
		}

		@Test
		void spyAnnotationCreatesMockitoSpyBeanOverrideHandler() {
			MockitoSpyBean annotation = AnnotationUtils.synthesizeAnnotation(MockitoSpyBean.class);
			BeanOverrideHandler object = processor.createHandler(annotation, TestCase.class, field);

			assertThat(object).isExactlyInstanceOf(MockitoSpyBeanOverrideHandler.class);
		}

		@Test
		void otherAnnotationThrows() {
			Annotation annotation = field.getAnnotation(Nullable.class);

			assertThatIllegalStateException()
					.isThrownBy(() -> processor.createHandler(annotation, TestCase.class, field))
					.withMessage("Invalid annotation passed to MockitoBeanOverrideProcessor: expected either " +
							"@MockitoBean or @MockitoSpyBean on field %s.%s", field.getDeclaringClass().getName(),
							field.getName());
		}

		@Test
		void typesNotSupportedAtFieldLevel() {
			Field field = ReflectionUtils.findField(TestCase.class, "typesNotSupported");
			MockitoBean annotation = field.getAnnotation(MockitoBean.class);

			assertThatIllegalStateException()
					.isThrownBy(() -> processor.createHandler(annotation, TestCase.class, field))
					.withMessage("The @MockitoBean 'types' attribute must be omitted when declared on a field");
		}


		static class TestCase {

			@MockitoBean
			@MockitoSpyBean
			public @Nullable Integer number;

			@MockitoBean(types = Integer.class)
			String typesNotSupported;
		}

		@MockitoBean(name = "bogus", types = Integer.class)
		static class NameNotSupportedTestCase {
		}
	}

	@Nested
	class CreateHandlersTests {

		@Test
		void missingTypes() {
			Class<?> testClass = MissingTypesTestCase.class;
			MockitoBean annotation = testClass.getAnnotation(MockitoBean.class);

			assertThatIllegalStateException()
					.isThrownBy(() -> processor.createHandlers(annotation, testClass))
					.withMessage("The @MockitoBean 'types' attribute must not be empty when declared on a class");
		}

		@Test
		void nameNotSupportedWithMultipleTypes() {
			Class<?> testClass = NameNotSupportedWithMultipleTypesTestCase.class;
			MockitoBean annotation = testClass.getAnnotation(MockitoBean.class);

			assertThatIllegalStateException()
					.isThrownBy(() -> processor.createHandlers(annotation, testClass))
					.withMessage("The @MockitoBean 'name' attribute cannot be used when mocking multiple types");
		}

		@Test
		void singleMockByType() {
			Class<?> testClass = SingleMockByTypeTestCase.class;
			MockitoBean annotation = testClass.getAnnotation(MockitoBean.class);
			List<BeanOverrideHandler> handlers = processor.createHandlers(annotation, testClass);

			assertThat(handlers).singleElement().isInstanceOfSatisfying(MockitoBeanOverrideHandler.class, handler -> {
				assertThat(handler.getField()).isNull();
				assertThat(handler.getBeanName()).isNull();
				assertThat(handler.getBeanType().resolve()).isEqualTo(Integer.class);
			});
		}

		@Test
		void singleMockByName() {
			Class<?> testClass = SingleMockByNameTestCase.class;
			MockitoBean annotation = testClass.getAnnotation(MockitoBean.class);
			List<BeanOverrideHandler> handlers = processor.createHandlers(annotation, testClass);

			assertThat(handlers).singleElement().isInstanceOfSatisfying(MockitoBeanOverrideHandler.class, handler -> {
				assertThat(handler.getField()).isNull();
				assertThat(handler.getBeanName()).isEqualTo("enigma");
				assertThat(handler.getBeanType().resolve()).isEqualTo(Integer.class);
			});
		}

		@Test
		void multipleMocks() {
			Class<?> testClass = MultipleMocksTestCase.class;
			MockitoBean annotation = testClass.getAnnotation(MockitoBean.class);
			List<BeanOverrideHandler> handlers = processor.createHandlers(annotation, testClass);

			assertThat(handlers).satisfiesExactly(
					handler1 -> {
						assertThat(handler1.getField()).isNull();
						assertThat(handler1.getBeanName()).isNull();
						assertThat(handler1.getBeanType().resolve()).isEqualTo(Integer.class);
					},
					handler2 -> {
						assertThat(handler2.getField()).isNull();
						assertThat(handler2.getBeanName()).isNull();
						assertThat(handler2.getBeanType().resolve()).isEqualTo(Float.class);
					}
				);
		}


		@MockitoBean
		static class MissingTypesTestCase {
		}

		@MockitoBean(name = "bogus", types = { Integer.class, Float.class })
		static class NameNotSupportedWithMultipleTypesTestCase {
		}

		@MockitoBean(types = Integer.class)
		static class SingleMockByTypeTestCase {
		}

		@MockitoBean(name = "enigma", types = Integer.class)
		static class SingleMockByNameTestCase {
		}

		@MockitoBean(types = { Integer.class, Float.class })
		static class MultipleMocksTestCase {
		}
	}

}
