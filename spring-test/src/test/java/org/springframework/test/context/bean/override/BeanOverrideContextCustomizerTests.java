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

package org.springframework.test.context.bean.override;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BeanOverrideContextCustomizer}.
 *
 * @author Stephane Nicoll
 */
class BeanOverrideContextCustomizerTests {

	@Test
	void customizerIsEqualWithIdenticalMetadata() {
		BeanOverrideContextCustomizer customizer = createCustomizer(new DummyOverrideMetadata("key"));
		BeanOverrideContextCustomizer customizer2 = createCustomizer(new DummyOverrideMetadata("key"));
		assertThat(customizer).isEqualTo(customizer2);
		assertThat(customizer).hasSameHashCodeAs(customizer2);
	}

	@Test
	void customizerIsEqualWithIdenticalMetadataInDifferentOrder() {
		BeanOverrideContextCustomizer customizer = createCustomizer(
				new DummyOverrideMetadata("key1"), new DummyOverrideMetadata("key2"));
		BeanOverrideContextCustomizer customizer2 = createCustomizer(
				new DummyOverrideMetadata("key2"), new DummyOverrideMetadata("key1"));
		assertThat(customizer).isEqualTo(customizer2);
		assertThat(customizer).hasSameHashCodeAs(customizer2);
	}

	@Test
	void customizerIsNotEqualWithDifferentMetadata() {
		BeanOverrideContextCustomizer customizer = createCustomizer(new DummyOverrideMetadata("key"));
		BeanOverrideContextCustomizer customizer2 = createCustomizer(
				new DummyOverrideMetadata("key"), new DummyOverrideMetadata("another"));
		assertThat(customizer).isNotEqualTo(customizer2);
	}

	private BeanOverrideContextCustomizer createCustomizer(OverrideMetadata... metadata) {
		return new BeanOverrideContextCustomizer(new LinkedHashSet<>(Arrays.asList(metadata)));
	}

	private static class DummyOverrideMetadata extends OverrideMetadata {

		private final String key;

		public DummyOverrideMetadata(String key) {
			super(mock(Field.class), ResolvableType.forClass(Object.class), null, BeanOverrideStrategy.REPLACE_DEFINITION);
			this.key = key;
		}

		@Override
		protected Object createOverride(String beanName, BeanDefinition existingBeanDefinition,
				Object existingBeanInstance) {
			return existingBeanInstance;
		}

		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			DummyOverrideMetadata that = (DummyOverrideMetadata) o;
			return Objects.equals(this.key, that.key);
		}

		@Override
		public int hashCode() {
			return this.key.hashCode();
		}
	}

}
