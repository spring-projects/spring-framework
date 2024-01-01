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
