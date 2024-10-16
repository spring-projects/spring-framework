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

import org.springframework.core.ResolvableType;
import org.springframework.test.context.bean.override.BeanOverrideProcessor;

/**
 * {@link BeanOverrideProcessor} implementation that provides support for
 * {@link MockitoBean @MockitoBean} and {@link MockitoSpyBean @MockitoSpyBean}.
 *
 * @author Simon Baslé
 * @since 6.2
 * @see MockitoBean @MockitoBean
 * @see MockitoSpyBean @MockitoSpyBean
 */
class MockitoBeanOverrideProcessor implements BeanOverrideProcessor {

	@Override
	public AbstractMockitoBeanOverrideHandler createHandler(Annotation overrideAnnotation, Class<?> testClass, Field field) {
		if (overrideAnnotation instanceof MockitoBean mockBean) {
			return new MockitoBeanOverrideHandler(field, ResolvableType.forField(field, testClass), mockBean);
		}
		else if (overrideAnnotation instanceof MockitoSpyBean spyBean) {
			return new MockitoSpyBeanOverrideHandler(field, ResolvableType.forField(field, testClass), spyBean);
		}
		throw new IllegalStateException("""
				Invalid annotation passed to MockitoBeanOverrideProcessor: \
				expected either @MockitoBean or @MockitoSpyBean on field %s.%s"""
					.formatted(field.getDeclaringClass().getName(), field.getName()));
	}

}
