/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 */
class SimplePropertyNamespaceHandlerTests {

	@Test
	void simpleBeanConfigured() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(beanFactory).loadBeanDefinitions(
				new ClassPathResource("simplePropertyNamespaceHandlerTests.xml", getClass()));
		ITestBean rob = (TestBean) beanFactory.getBean("rob");
		ITestBean sally = (TestBean) beanFactory.getBean("sally");
		assertThat(rob.getName()).isEqualTo("Rob Harrop");
		assertThat(rob.getAge()).isEqualTo(24);
		assertThat(sally).isEqualTo(rob.getSpouse());
	}

	@Test
	void innerBeanConfigured() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(beanFactory).loadBeanDefinitions(
				new ClassPathResource("simplePropertyNamespaceHandlerTests.xml", getClass()));
		TestBean sally = (TestBean) beanFactory.getBean("sally2");
		ITestBean rob = sally.getSpouse();
		assertThat(rob.getName()).isEqualTo("Rob Harrop");
		assertThat(rob.getAge()).isEqualTo(24);
		assertThat(sally).isEqualTo(rob.getSpouse());
	}

	@Test
	void withPropertyDefinedTwice() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				new XmlBeanDefinitionReader(beanFactory).loadBeanDefinitions(
							new ClassPathResource("simplePropertyNamespaceHandlerTestsWithErrors.xml", getClass())));
	}

	@Test
	void propertyWithNameEndingInRef() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(beanFactory).loadBeanDefinitions(
				new ClassPathResource("simplePropertyNamespaceHandlerTests.xml", getClass()));
		ITestBean sally = (TestBean) beanFactory.getBean("derivedSally");
		assertThat(sally.getSpouse().getName()).isEqualTo("r");
	}

}
