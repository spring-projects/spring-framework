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

import org.jspecify.annotations.Nullable;
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

	static class TestCase {

		@MockitoBean
		@MockitoSpyBean
		public @Nullable Integer number;
	}

}
