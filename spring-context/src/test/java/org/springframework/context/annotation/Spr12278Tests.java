/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Stephane Nicoll
 */
class Spr12278Tests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void close() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	void componentSingleConstructor() {
		this.context = new AnnotationConfigApplicationContext(BaseConfiguration.class,
				SingleConstructorComponent.class);
		assertThat(this.context.getBean(SingleConstructorComponent.class).autowiredName).isEqualTo("foo");
	}

	@Test
	void componentTwoConstructorsNoHint() {
		this.context = new AnnotationConfigApplicationContext(BaseConfiguration.class,
				TwoConstructorsComponent.class);
		assertThat(this.context.getBean(TwoConstructorsComponent.class).name).isEqualTo("fallback");
	}

	@Test
	void componentTwoSpecificConstructorsNoHint() {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				new AnnotationConfigApplicationContext(BaseConfiguration.class, TwoSpecificConstructorsComponent.class))
			.withMessageContaining("No default constructor found");
	}


	@Configuration
	static class BaseConfiguration {

		@Bean
		public String autowiredName() {
			return "foo";
		}
	}

	private static class SingleConstructorComponent {

		private final String autowiredName;

		// No @Autowired - implicit wiring
		@SuppressWarnings("unused")
		public SingleConstructorComponent(String autowiredName) {
			this.autowiredName = autowiredName;
		}

	}

	private static class TwoConstructorsComponent {

		private final String name;

		public TwoConstructorsComponent(String name) {
			this.name = name;
		}

		@SuppressWarnings("unused")
		public TwoConstructorsComponent() {
			this("fallback");
		}
	}

	@SuppressWarnings("unused")
	private static class TwoSpecificConstructorsComponent {

		private final Integer counter;

		public TwoSpecificConstructorsComponent(Integer counter) {
			this.counter = counter;
		}

		public TwoSpecificConstructorsComponent(String name) {
			this(Integer.valueOf(name));
		}
	}

}
