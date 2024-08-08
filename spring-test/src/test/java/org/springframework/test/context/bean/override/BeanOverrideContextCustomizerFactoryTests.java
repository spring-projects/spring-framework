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

import java.util.Collections;
import java.util.function.Consumer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.BeanOverrideContextCustomizerFactoryTests.Test2.Green;
import org.springframework.test.context.bean.override.BeanOverrideContextCustomizerFactoryTests.Test2.Orange;
import org.springframework.test.context.bean.override.DummyBean.DummyBeanOverrideProcessor.DummyOverrideMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanOverrideContextCustomizerFactory}.
 *
 * @author Stephane Nicoll
 */
class BeanOverrideContextCustomizerFactoryTests {

	private final BeanOverrideContextCustomizerFactory factory = new BeanOverrideContextCustomizerFactory();

	@Test
	void createContextCustomizerWhenTestHasNoBeanOverride() {
		assertThat(createContextCustomizer(String.class)).isNull();
	}

	@Test
	void createContextCustomizerWhenTestHasSingleBeanOverride() {
		BeanOverrideContextCustomizer customizer = createContextCustomizer(Test1.class);
		assertThat(customizer).isNotNull();
		assertThat(customizer.getMetadata()).singleElement().satisfies(dummyMetadata(null, String.class));
	}

	@Test
	void createContextCustomizerWhenNestedTestHasSingleBeanOverrideInParent() {
		BeanOverrideContextCustomizer customizer = createContextCustomizer(Orange.class);
		assertThat(customizer).isNotNull();
		assertThat(customizer.getMetadata()).singleElement().satisfies(dummyMetadata(null, String.class));
	}

	@Test
	void createContextCustomizerWhenNestedTestHasBeanOverrideAsWellAsTheParent() {
		BeanOverrideContextCustomizer customizer = createContextCustomizer(Green.class);
		assertThat(customizer).isNotNull();
		assertThat(customizer.getMetadata())
				.anySatisfy(dummyMetadata(null, String.class))
				.anySatisfy(dummyMetadata("counterBean", Integer.class))
				.hasSize(2);
	}


	private Consumer<OverrideMetadata> dummyMetadata(@Nullable String beanName, Class<?> beanType) {
		return dummyMetadata(beanName, beanType, BeanOverrideStrategy.REPLACE_DEFINITION);
	}

	private Consumer<OverrideMetadata> dummyMetadata(@Nullable String beanName, Class<?> beanType, BeanOverrideStrategy strategy) {
		return metadata -> {
			assertThat(metadata).isExactlyInstanceOf(DummyOverrideMetadata.class);
			assertThat(metadata.getBeanName()).isEqualTo(beanName);
			assertThat(metadata.getBeanType().toClass()).isEqualTo(beanType);
			assertThat(metadata.getStrategy()).isEqualTo(strategy);
		};
	}

	@Nullable
	BeanOverrideContextCustomizer createContextCustomizer(Class<?> testClass) {
		return this.factory.createContextCustomizer(testClass, Collections.emptyList());
	}

	static class Test1 {

		@DummyBean
		private String descriptor;

	}

	static class Test2 {

		@DummyBean
		private String name;

		@Nested
		class Orange {

		}

		@Nested
		class Green {

			@DummyBean(beanName = "counterBean")
			private Integer counter;

		}
	}

}
