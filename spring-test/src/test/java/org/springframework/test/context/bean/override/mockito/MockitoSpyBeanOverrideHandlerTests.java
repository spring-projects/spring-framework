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

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.bean.override.BeanOverrideHandler;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockitoSpyBeanOverrideHandler}.
 *
 * @author Stephane Nicoll
 */
class MockitoSpyBeanOverrideHandlerTests {

	@Test
	void forTestClassSetsNameToNullIfAnnotationNameIsNull() {
		List<BeanOverrideHandler> list = BeanOverrideHandler.forTestClass(SampleOneSpy.class);
		assertThat(list).singleElement().satisfies(handler -> assertThat(handler.getBeanName()).isNull());
	}

	@Test
	void forTestClassSetsNameToAnnotationName() {
		List<BeanOverrideHandler> list = BeanOverrideHandler.forTestClass(SampleOneSpyWithName.class);
		assertThat(list).singleElement().satisfies(handler -> assertThat(handler.getBeanName()).isEqualTo("anotherService"));
	}

	@Test
	void isEqualToWithSameInstance() {
		MockitoSpyBeanOverrideHandler handler = createBeanOverrideHandler(sampleField("service"));
		assertThat(handler).isEqualTo(handler);
		assertThat(handler).hasSameHashCodeAs(handler);
	}

	@Test
	void isEqualToWithSameMetadata() {
		MockitoSpyBeanOverrideHandler handler1 = createBeanOverrideHandler(sampleField("service"));
		MockitoSpyBeanOverrideHandler handler2 = createBeanOverrideHandler(sampleField("service"));
		assertThat(handler1).isEqualTo(handler2);
		assertThat(handler1).hasSameHashCodeAs(handler2);
	}

	@Test
	void isNotEqualToByTypeLookupWithSameMetadataButDifferentField() {
		MockitoSpyBeanOverrideHandler handler1 = createBeanOverrideHandler(sampleField("service"));
		MockitoSpyBeanOverrideHandler handler2 = createBeanOverrideHandler(sampleField("service2"));
		assertThat(handler1).isNotEqualTo(handler2);
	}

	@Test
	void isEqualToByNameLookupWithSameMetadataButDifferentField() {
		MockitoSpyBeanOverrideHandler handler1 = createBeanOverrideHandler(sampleField("service3"));
		MockitoSpyBeanOverrideHandler handler2 = createBeanOverrideHandler(sampleField("service4"));
		assertThat(handler1).isEqualTo(handler2);
		assertThat(handler1).hasSameHashCodeAs(handler2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentBeanName() {
		MockitoSpyBeanOverrideHandler handler1 = createBeanOverrideHandler(sampleField("service"));
		MockitoSpyBeanOverrideHandler handler2 = createBeanOverrideHandler(sampleField("service3"));
		assertThat(handler1).isNotEqualTo(handler2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentReset() {
		MockitoSpyBeanOverrideHandler handler1 = createBeanOverrideHandler(sampleField("service"));
		MockitoSpyBeanOverrideHandler handler2 = createBeanOverrideHandler(sampleField("service5"));
		assertThat(handler1).isNotEqualTo(handler2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentProxyTargetAwareFlag() {
		MockitoSpyBeanOverrideHandler handler1 = createBeanOverrideHandler(sampleField("service"));
		MockitoSpyBeanOverrideHandler handler2 = createBeanOverrideHandler(sampleField("service6"));
		assertThat(handler1).isNotEqualTo(handler2);
	}


	private Field sampleField(String fieldName) {
		Field field = ReflectionUtils.findField(Sample.class, fieldName);
		assertThat(field).isNotNull();
		return field;
	}

	private MockitoSpyBeanOverrideHandler createBeanOverrideHandler(Field field) {
		MockitoSpyBean annotation = AnnotatedElementUtils.getMergedAnnotation(field, MockitoSpyBean.class);
		return new MockitoSpyBeanOverrideHandler(field, ResolvableType.forClass(field.getType()), annotation);
	}


	static class SampleOneSpy {

		@MockitoSpyBean
		String service;

	}

	static class SampleOneSpyWithName {

		@MockitoSpyBean("anotherService")
		String service;

	}

	static class Sample {

		@MockitoSpyBean
		private String service;

		@MockitoSpyBean
		private String service2;

		@MockitoSpyBean(name = "beanToSpy")
		private String service3;

		@MockitoSpyBean(value = "beanToSpy")
		private String service4;

		@MockitoSpyBean(reset = MockReset.BEFORE)
		private String service5;

		@MockitoSpyBean(proxyTargetAware = false)
		private String service6;

	}

}
