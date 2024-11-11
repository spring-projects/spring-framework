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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanOverrideContextCustomizer}.
 *
 * @author Stephane Nicoll
 */
class BeanOverrideContextCustomizerTests {

	@Test
	void customizerIsEqualWithIdenticalMetadata() {
		BeanOverrideContextCustomizer customizer = createCustomizer(new DummyBeanOverrideHandler("key"));
		BeanOverrideContextCustomizer customizer2 = createCustomizer(new DummyBeanOverrideHandler("key"));
		assertThat(customizer).isEqualTo(customizer2);
		assertThat(customizer).hasSameHashCodeAs(customizer2);
	}

	@Test
	void customizerIsEqualWithIdenticalMetadataInDifferentOrder() {
		BeanOverrideContextCustomizer customizer = createCustomizer(
				new DummyBeanOverrideHandler("key1"), new DummyBeanOverrideHandler("key2"));
		BeanOverrideContextCustomizer customizer2 = createCustomizer(
				new DummyBeanOverrideHandler("key2"), new DummyBeanOverrideHandler("key1"));
		assertThat(customizer).isEqualTo(customizer2);
		assertThat(customizer).hasSameHashCodeAs(customizer2);
	}

	@Test
	void customizerIsNotEqualWithDifferentMetadata() {
		BeanOverrideContextCustomizer customizer = createCustomizer(new DummyBeanOverrideHandler("key"));
		BeanOverrideContextCustomizer customizer2 = createCustomizer(
				new DummyBeanOverrideHandler("key"), new DummyBeanOverrideHandler("another"));
		assertThat(customizer).isNotEqualTo(customizer2);
	}

	private BeanOverrideContextCustomizer createCustomizer(BeanOverrideHandler... handlers) {
		return new BeanOverrideContextCustomizer(new LinkedHashSet<>(Arrays.asList(handlers)));
	}

	private static class DummyBeanOverrideHandler extends BeanOverrideHandler {

		private final String key;

		public DummyBeanOverrideHandler(String key) {
			super(ReflectionUtils.findField(DummyBeanOverrideHandler.class, "key"),
					ResolvableType.forClass(Object.class), null, BeanOverrideStrategy.REPLACE);
			this.key = key;
		}

		@Override
		protected Object createOverrideInstance(String beanName, BeanDefinition existingBeanDefinition,
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
			DummyBeanOverrideHandler that = (DummyBeanOverrideHandler) o;
			return Objects.equals(this.key, that.key);
		}

		@Override
		public int hashCode() {
			return this.key.hashCode();
		}
	}

}
