/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.xml;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rob Harrop
 */
class SchemaValidationTests {

	@Test
	void withAutodetection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
		assertThatExceptionOfType(BeansException.class).isThrownBy(() ->
				reader.loadBeanDefinitions(new ClassPathResource("invalidPerSchema.xml", getClass())))
			.withCauseInstanceOf(SAXParseException.class);
	}

	@Test
	void withExplicitValidationMode() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		assertThatExceptionOfType(BeansException.class).isThrownBy(() ->
				reader.loadBeanDefinitions(new ClassPathResource("invalidPerSchema.xml", getClass())))
			.withCauseInstanceOf(SAXParseException.class);
	}

	@Test
	void loadDefinitions() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.loadBeanDefinitions(new ClassPathResource("schemaValidated.xml", getClass()));

		TestBean foo = (TestBean) bf.getBean("fooBean");
		assertThat(foo.getSpouse()).as("Spouse is null").isNotNull();
		assertThat(foo.getFriends().size()).as("Incorrect number of friends").isEqualTo(2);
	}

}
