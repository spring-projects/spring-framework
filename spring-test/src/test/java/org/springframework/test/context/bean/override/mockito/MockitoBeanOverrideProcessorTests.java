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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.OverrideMetadata;
import org.springframework.test.context.bean.override.example.ExampleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link MockitoBeanOverrideProcessorTests}
 */
public class MockitoBeanOverrideProcessorTests {

	private final MockitoBeanOverrideProcessor processor = new MockitoBeanOverrideProcessor();

	@Test
	void mockAnnotationCreatesMockMetadata() throws NoSuchFieldException {
		MockitoBean annotation = AnnotationUtils.synthesizeAnnotation(MockitoBean.class);
		Class<?> clazz = MockitoConf.class;
		Field field = clazz.getField("a");
		OverrideMetadata object = this.processor.createMetadata(annotation, clazz, field);

		assertThat(object).isExactlyInstanceOf(MockitoBeanOverrideMetadata.class);
	}

	@Test
	void spyAnnotationCreatesSpyMetadata() throws NoSuchFieldException {
		MockitoSpyBean annotation = AnnotationUtils.synthesizeAnnotation(MockitoSpyBean.class);
		Class<?> clazz = MockitoConf.class;
		Field field = clazz.getField("a");
		OverrideMetadata object = this.processor.createMetadata(annotation, clazz, field);

		assertThat(object).isExactlyInstanceOf(MockitoSpyBeanOverrideMetadata.class);
	}

	@Test
	void otherAnnotationThrows() throws NoSuchFieldException {
		Class<?> clazz = MockitoConf.class;
		Field field = clazz.getField("a");
		Annotation annotation = field.getAnnotation(Nullable.class);

		assertThatIllegalStateException()
				.isThrownBy(() -> this.processor.createMetadata(annotation, clazz, field))
				.withMessage("Invalid annotation passed to MockitoBeanOverrideProcessor: expected " +
						"@MockitoBean/@MockitoSpyBean on field %s.%s", field.getDeclaringClass().getName(),
						field.getName());
	}

	static class MockitoConf {
		@Nullable
		@MockitoBean
		@MockitoSpyBean
		public ExampleService a;
	}

}
