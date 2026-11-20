/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Adrian Colyer
 * @author Chris Beams
 */
class AopNamespaceHandlerArgNamesTests {

	@Test
	void argNamesOK() {
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-ok.xml", getClass());
	}

	@Test
	void argNamesError() {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-error.xml", getClass()))
			.matches(ex -> ex.contains(IllegalArgumentException.class));
	}

}
