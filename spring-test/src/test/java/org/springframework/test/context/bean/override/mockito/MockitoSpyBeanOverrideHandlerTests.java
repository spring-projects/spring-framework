/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.bean.override.BeanOverrideHandler;
import org.springframework.test.context.bean.override.BeanOverrideTestUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockitoSpyBeanOverrideHandler}.
 *
 * @author Stephane Nicoll
 */
class MockitoSpyBeanOverrideHandlerTests {

	@Test
	void beanNameIsSetToNullIfAnnotationNameIsEmpty() {
		List<BeanOverrideHandler> list = BeanOverrideTestUtils.findHandlers(SampleOneSpy.class);
		assertThat(list).singleElement().satisfies(handler -> assertThat(handler.getBeanName()).isNull());
	}

	@Test
	void beanNameIsSetToAnnotationName() {
		List<BeanOverrideHandler> list = BeanOverrideTestUtils.findHandlers(SampleOneSpyWithName.class);
		assertThat(list).singleElement().satisfies(handler -> assertThat(handler.getBeanName()).isEqualTo("anotherService"));
	}

	@Test
	void isEqualToWithSameInstance() {
		MockitoSpyBeanOverrideHandler handler = handlerFor("service");
		assertThat(handler).isEqualTo(handler);
		assertThat(handler).hasSameHashCodeAs(handler);
	}

	@Test
	void isEqualToWithSameMetadata() {
		MockitoSpyBeanOverrideHandler handler1 = handlerFor("service");
		MockitoSpyBeanOverrideHandler handler2 = handlerFor("service");
		assertThat(handler1).isEqualTo(handler2);
		assertThat(handler1).hasSameHashCodeAs(handler2);
	}

	@Test
	void isNotEqualToByTypeLookupWithSameMetadataButDifferentField() {
		assertThat(handlerFor("service")).isNotEqualTo(handlerFor("service2"));
	}

	@Test
	void isEqualToByNameLookupWithSameMetadataButDifferentField() {
		MockitoSpyBeanOverrideHandler handler1 = handlerFor("service3");
		MockitoSpyBeanOverrideHandler handler2 = handlerFor("service4");
		assertThat(handler1).isEqualTo(handler2);
		assertThat(handler1).hasSameHashCodeAs(handler2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentBeanName() {
		assertThat(handlerFor("service")).isNotEqualTo(handlerFor("service3"));
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentReset() {
		assertThat(handlerFor("service")).isNotEqualTo(handlerFor("service5"));
	}


	private static MockitoSpyBeanOverrideHandler handlerFor(String fieldName) {
		Field field = ReflectionUtils.findField(Sample.class, fieldName);
		assertThat(field).isNotNull();
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

	}

}
