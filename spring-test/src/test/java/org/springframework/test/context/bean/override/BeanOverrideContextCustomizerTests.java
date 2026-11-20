/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
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
					ResolvableType.forClass(Object.class), null, "", BeanOverrideStrategy.REPLACE);
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
