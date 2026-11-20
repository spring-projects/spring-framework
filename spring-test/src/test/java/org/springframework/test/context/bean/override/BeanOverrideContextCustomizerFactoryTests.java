/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override;

import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.bean.override.DummyBean.DummyBeanOverrideProcessor.DummyBeanOverrideHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link BeanOverrideContextCustomizerFactory}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
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
		assertThat(customizer.getBeanOverrideHandlers()).singleElement().satisfies(dummyHandler(null, String.class));
	}

	@Test
	void createContextCustomizerWhenNestedTestHasSingleBeanOverrideInParent() {
		BeanOverrideContextCustomizer customizer = createContextCustomizer(Test2.Orange.class);
		assertThat(customizer).isNotNull();
		assertThat(customizer.getBeanOverrideHandlers()).singleElement().satisfies(dummyHandler(null, String.class));
	}

	@Test
	void createContextCustomizerWhenNestedTestHasBeanOverrideAsWellAsTheParent() {
		BeanOverrideContextCustomizer customizer = createContextCustomizer(Test2.Green.class);
		assertThat(customizer).isNotNull();
		assertThat(customizer.getBeanOverrideHandlers())
				.anySatisfy(dummyHandler(null, String.class))
				.anySatisfy(dummyHandler("counterBean", Integer.class))
				.hasSize(2);
	}

	@Test  // gh-34054
	void failsWithDuplicateBeanOverrides() {
		Class<?> testClass = DuplicateOverridesTestCase.class;
		assertThatIllegalStateException()
				.isThrownBy(() -> createContextCustomizer(testClass))
				.withMessageStartingWith("Duplicate BeanOverrideHandler discovered in test class " + testClass.getName())
				.withMessageContaining("DummyBeanOverrideHandler");
	}


	private Consumer<BeanOverrideHandler> dummyHandler(@Nullable String beanName, Class<?> beanType) {
		return dummyHandler(beanName, beanType, BeanOverrideStrategy.REPLACE);
	}

	private Consumer<BeanOverrideHandler> dummyHandler(@Nullable String beanName, Class<?> beanType, BeanOverrideStrategy strategy) {
		return handler -> {
			assertThat(handler).isExactlyInstanceOf(DummyBeanOverrideHandler.class);
			assertThat(handler.getBeanName()).isEqualTo(beanName);
			assertThat(handler.getBeanType().toClass()).isEqualTo(beanType);
			assertThat(handler.getStrategy()).isEqualTo(strategy);
		};
	}

	private @Nullable BeanOverrideContextCustomizer createContextCustomizer(Class<?> testClass) {
		return this.factory.createContextCustomizer(testClass, List.of(new ContextConfigurationAttributes(testClass)));
	}


	static class Test1 {

		@DummyBean
		private String descriptor;
	}

	static class Test2 {

		@DummyBean
		private String name;

		// @Nested
		class Orange {
		}

		// @Nested
		class Green {

			@DummyBean(beanName = "counterBean")
			private Integer counter;
		}
	}

	static class DuplicateOverridesTestCase {

		@DummyBean(beanName = "text")
		String text1;

		@DummyBean(beanName = "text")
		String text2;
	}

}
