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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OverrideMetadata}.
 *
 * @author Simon Baslé
 * @since 6.2
 */
class OverrideMetadataTests {

	@Test
	void implicitConfigurations() throws Exception {
		OverrideMetadata metadata = exampleOverride();
		assertThat(metadata.getBeanName()).as("expectedBeanName").isNull();
	}


	@NonNull
	String annotated = "exampleField";

	private static OverrideMetadata exampleOverride() throws Exception {
		Field field = OverrideMetadataTests.class.getDeclaredField("annotated");
		return new ConcreteOverrideMetadata(field, ResolvableType.forClass(String.class),
				BeanOverrideStrategy.REPLACE_DEFINITION);
	}

	static class ConcreteOverrideMetadata extends OverrideMetadata {

		ConcreteOverrideMetadata(Field field, ResolvableType typeToOverride,
				BeanOverrideStrategy strategy) {

			super(field, typeToOverride, strategy);
		}

		@Override
		protected Object createOverride(String beanName, @Nullable BeanDefinition existingBeanDefinition,
				@Nullable Object existingBeanInstance) {

			return BeanOverrideStrategy.REPLACE_DEFINITION;
		}
	}

}
