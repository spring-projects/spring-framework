/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SPR-10668.
 *
 * @author Oliver Gierke
 * @author Phillip Webb
 */
class Spr10668Tests {

	@Test
	void selfInjectHierarchy() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ChildConfig.class);
		assertThat(context.getBean(MyComponent.class)).isNotNull();
		context.close();
	}


	@Configuration
	public static class ParentConfig {

		@Autowired(required = false)
		MyComponent component;
	}


	@Configuration
	public static class ChildConfig extends ParentConfig {

		@Bean
		public MyComponentImpl myComponent() {
			return new MyComponentImpl();
		}
	}


	public interface MyComponent {}

	public static class MyComponentImpl implements MyComponent {}

}
