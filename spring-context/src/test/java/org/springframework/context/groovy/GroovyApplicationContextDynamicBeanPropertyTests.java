/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.groovy;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.GenericGroovyApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Jeff Brown
 * @author Sam Brannen
 * @author Dave Syer
 */
class GroovyApplicationContextDynamicBeanPropertyTests {

	@Test
	void accessDynamicBeanProperties() {
		GenericGroovyApplicationContext ctx = new GenericGroovyApplicationContext("org/springframework/context/groovy/applicationContext.groovy");

		Object framework = ctx.getBean("framework");
		assertThat(framework).as("could not find framework bean").isNotNull();
		assertThat(framework).isEqualTo("Grails");

		ctx.close();
	}

	@Test
	void accessNonExistentBeanViaDynamicProperty() {
		GenericGroovyApplicationContext ctx = new GenericGroovyApplicationContext("org/springframework/context/groovy/applicationContext.groovy");

		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> ctx.getBean("someNonExistentBean"))
			.withMessage("No bean named 'someNonExistentBean' available");

		ctx.close();
	}

}
