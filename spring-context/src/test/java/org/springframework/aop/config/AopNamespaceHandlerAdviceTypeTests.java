/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.config;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Adrian Colyer
 * @author Chris Beams
 */
class AopNamespaceHandlerAdviceTypeTests {

	@Test
	void parsingOfAdviceTypes() {
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-ok.xml", getClass());
	}

	@Test
	void parsingOfAdviceTypesWithError() {
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-error.xml", getClass()))
			.matches(ex -> ex.contains(SAXParseException.class));
	}

}
