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
import org.springframework.test.context.bean.override.OverrideMetadata;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockitoSpyBeanOverrideMetadata}.
 *
 * @author Stephane Nicoll
 */
class MockitoSpyBeanOverrideMetadataTests {

	@Test
	void forTestClassSetsNameToNullIfAnnotationNameIsNull() {
		List<OverrideMetadata> list = OverrideMetadata.forTestClass(SampleOneSpy.class);
		assertThat(list).singleElement().satisfies(metadata -> assertThat(metadata.getBeanName()).isNull());
	}

	@Test
	void forTestClassSetsNameToAnnotationName() {
		List<OverrideMetadata> list = OverrideMetadata.forTestClass(SampleOneSpyWithName.class);
		assertThat(list).singleElement().satisfies(metadata -> assertThat(metadata.getBeanName()).isEqualTo("anotherService"));
	}

	@Test
	void isEqualToWithSameInstance() {
		MockitoSpyBeanOverrideMetadata metadata = createMetadata(sampleField("service"));
		assertThat(metadata).isEqualTo(metadata);
		assertThat(metadata).hasSameHashCodeAs(metadata);
	}

	@Test
	void isEqualToWithSameMetadata() {
		MockitoSpyBeanOverrideMetadata metadata = createMetadata(sampleField("service"));
		MockitoSpyBeanOverrideMetadata metadata2 = createMetadata(sampleField("service"));
		assertThat(metadata).isEqualTo(metadata2);
		assertThat(metadata).hasSameHashCodeAs(metadata2);
	}

	@Test
	void isNotEqualToByTypeLookupWithSameMetadataButDifferentField() {
		MockitoSpyBeanOverrideMetadata metadata = createMetadata(sampleField("service"));
		MockitoSpyBeanOverrideMetadata metadata2 = createMetadata(sampleField("service2"));
		assertThat(metadata).isNotEqualTo(metadata2);
	}

	@Test
	void isEqualToByNameLookupWithSameMetadataButDifferentField() {
		MockitoSpyBeanOverrideMetadata metadata = createMetadata(sampleField("service3"));
		MockitoSpyBeanOverrideMetadata metadata2 = createMetadata(sampleField("service4"));
		assertThat(metadata).isEqualTo(metadata2);
		assertThat(metadata).hasSameHashCodeAs(metadata2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentBeanName() {
		MockitoSpyBeanOverrideMetadata metadata = createMetadata(sampleField("service"));
		MockitoSpyBeanOverrideMetadata metadata2 = createMetadata(sampleField("service3"));
		assertThat(metadata).isNotEqualTo(metadata2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentReset() {
		MockitoSpyBeanOverrideMetadata metadata = createMetadata(sampleField("service"));
		MockitoSpyBeanOverrideMetadata metadata2 = createMetadata(sampleField("service5"));
		assertThat(metadata).isNotEqualTo(metadata2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentProxyTargetAwareFlag() {
		MockitoSpyBeanOverrideMetadata metadata = createMetadata(sampleField("service"));
		MockitoSpyBeanOverrideMetadata metadata2 = createMetadata(sampleField("service6"));
		assertThat(metadata).isNotEqualTo(metadata2);
	}


	private Field sampleField(String fieldName) {
		Field field = ReflectionUtils.findField(Sample.class, fieldName);
		assertThat(field).isNotNull();
		return field;
	}

	private MockitoSpyBeanOverrideMetadata createMetadata(Field field) {
		MockitoSpyBean annotation = field.getAnnotation(MockitoSpyBean.class);
		return new MockitoSpyBeanOverrideMetadata(field, ResolvableType.forClass(field.getType()), annotation);
	}


	static class SampleOneSpy {

		@MockitoSpyBean
		String service;

	}

	static class SampleOneSpyWithName {

		@MockitoSpyBean(name = "anotherService")
		String service;

	}

	static class Sample {

		@MockitoSpyBean
		private String service;

		@MockitoSpyBean
		private String service2;

		@MockitoSpyBean(name = "beanToMock")
		private String service3;

		@MockitoSpyBean(name = "beanToMock")
		private String service4;

		@MockitoSpyBean(reset = MockReset.BEFORE)
		private String service5;

		@MockitoSpyBean(proxyTargetAware = false)
		private String service6;

	}

}
