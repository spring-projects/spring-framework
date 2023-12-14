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

package org.springframework.test.bean.override;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

class OverrideMetadataTests {

	static class ConcreteOverrideMetadata extends OverrideMetadata {

		ConcreteOverrideMetadata(Field field, Annotation overrideAnnotation, ResolvableType typeToOverride,
				BeanOverrideStrategy strategy) {
			super(field, overrideAnnotation, typeToOverride, strategy);
		}

		@Override
		public String getBeanOverrideDescription() {
			return ConcreteOverrideMetadata.class.getSimpleName();
		}

		@Override
		protected Object createOverride(String beanName, @Nullable BeanDefinition existingBeanDefinition, @Nullable Object existingBeanInstance) {
			return BeanOverrideStrategy.REPLACE_DEFINITION;
		}
	}

	@NonNull
	public String annotated = "exampleField";

	static OverrideMetadata exampleOverride() throws NoSuchFieldException {
		final Field annotated = OverrideMetadataTests.class.getField("annotated");
		return new ConcreteOverrideMetadata(Objects.requireNonNull(annotated), annotated.getAnnotation(NonNull.class),
				ResolvableType.forClass(String.class), BeanOverrideStrategy.REPLACE_DEFINITION);
	}

	@Test
	void implicitConfigurations() throws NoSuchFieldException {
		final OverrideMetadata metadata = exampleOverride();
		assertThat(metadata.getExpectedBeanName()).as("expectedBeanName")
				.isEqualTo(metadata.field().getName());
	}

}
