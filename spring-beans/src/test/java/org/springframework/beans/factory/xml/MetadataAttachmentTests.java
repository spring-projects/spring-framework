/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.xml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 */
class MetadataAttachmentTests {

	private DefaultListableBeanFactory beanFactory;


	@BeforeEach
	void setUp() {
		this.beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
				new ClassPathResource("withMeta.xml", getClass()));
	}

	@Test
	void metadataAttachment() {
		BeanDefinition beanDefinition1 = this.beanFactory.getMergedBeanDefinition("testBean1");
		assertThat(beanDefinition1.getAttribute("foo")).isEqualTo("bar");
	}

	@Test
	void metadataIsInherited() {
		BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("testBean2");
		assertThat(beanDefinition.getAttribute("foo")).as("Metadata not inherited").isEqualTo("bar");
		assertThat(beanDefinition.getAttribute("abc")).as("Child metadata not attached").isEqualTo("123");
	}

	@Test
	void propertyMetadata() {
		BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("testBean3");
		PropertyValue pv = beanDefinition.getPropertyValues().getPropertyValue("name");
		assertThat(pv.getAttribute("surname")).isEqualTo("Harrop");
	}

}
