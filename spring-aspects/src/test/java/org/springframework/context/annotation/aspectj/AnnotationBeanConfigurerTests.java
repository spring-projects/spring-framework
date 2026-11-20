/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.aspectj;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.aspectj.ShouldBeConfiguredBySpring;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that @EnableSpringConfigured properly registers an
 * {@link org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect},
 * just as does {@code <context:spring-configured>}.
 *
 * @author Chris Beams
 * @since 3.1
 */
class AnnotationBeanConfigurerTests {

	@Test
	void injection() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class)) {
			ShouldBeConfiguredBySpring myObject = new ShouldBeConfiguredBySpring();
			assertThat(myObject.getName()).isEqualTo("Rod");
		}
	}


	@Configuration
	@ImportResource("org/springframework/beans/factory/aspectj/beanConfigurerTests-beans.xml")
	@EnableSpringConfigured
	static class Config {
	}

}
