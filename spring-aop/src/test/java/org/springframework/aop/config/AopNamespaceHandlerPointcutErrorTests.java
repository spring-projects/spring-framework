/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * @author Mark Fisher
 * @author Chris Beams
 * @author Sam Brannen
 */
class AopNamespaceHandlerPointcutErrorTests {

	@Test
	void duplicatePointcutConfig() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
		ClassPathResource resource = qualifiedResource(getClass(), "pointcutDuplication.xml");

		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> reader.loadBeanDefinitions(resource));
	}

	@Test
	void missingPointcutConfig() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
		ClassPathResource resource = qualifiedResource(getClass(), "pointcutMissing.xml");

		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> reader.loadBeanDefinitions(resource));
	}

}
