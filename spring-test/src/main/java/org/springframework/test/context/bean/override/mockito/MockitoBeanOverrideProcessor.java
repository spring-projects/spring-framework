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
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.ResolvableType;
import org.springframework.test.context.bean.override.BeanOverrideHandler;
import org.springframework.test.context.bean.override.BeanOverrideProcessor;
import org.springframework.util.Assert;

/**
 * {@link BeanOverrideProcessor} implementation that provides support for
 * {@link MockitoBean @MockitoBean} and {@link MockitoSpyBean @MockitoSpyBean}.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 * @see MockitoBean @MockitoBean
 * @see MockitoSpyBean @MockitoSpyBean
 */
class MockitoBeanOverrideProcessor implements BeanOverrideProcessor {

	@Override
	public AbstractMockitoBeanOverrideHandler createHandler(Annotation overrideAnnotation, Class<?> testClass, Field field) {
		if (overrideAnnotation instanceof MockitoBean mockitoBean) {
			Assert.state(mockitoBean.types().length == 0,
					"The @MockitoBean 'types' attribute must be omitted when declared on a field");
			return new MockitoBeanOverrideHandler(field, ResolvableType.forField(field, testClass), mockitoBean);
		}
		else if (overrideAnnotation instanceof MockitoSpyBean spyBean) {
			return new MockitoSpyBeanOverrideHandler(field, ResolvableType.forField(field, testClass), spyBean);
		}
		throw new IllegalStateException("""
				Invalid annotation passed to MockitoBeanOverrideProcessor: \
				expected either @MockitoBean or @MockitoSpyBean on field %s.%s"""
					.formatted(field.getDeclaringClass().getName(), field.getName()));
	}

	@Override
	public List<BeanOverrideHandler> createHandlers(Annotation overrideAnnotation, Class<?> testClass) {
		if (!(overrideAnnotation instanceof MockitoBean mockitoBean)) {
			throw new IllegalStateException("""
					Invalid annotation passed to MockitoBeanOverrideProcessor: \
					expected @MockitoBean on test class """ + testClass.getName());
		}
		Class<?>[] types = mockitoBean.types();
		Assert.state(types.length > 0,
				"The @MockitoBean 'types' attribute must not be empty when declared on a class");
		Assert.state(mockitoBean.name().isEmpty() || types.length == 1,
				"The @MockitoBean 'name' attribute cannot be used when mocking multiple types");
		List<BeanOverrideHandler> handlers = new ArrayList<>();
		for (Class<?> type : types) {
			handlers.add(new MockitoBeanOverrideHandler(ResolvableType.forClass(type), mockitoBean));
		}
		return handlers;
	}

}
