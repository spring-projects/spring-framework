/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.support;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 */
@SuppressWarnings("deprecation")
class PropertiesBeanDefinitionReaderTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final PropertiesBeanDefinitionReader reader = new PropertiesBeanDefinitionReader(this.beanFactory);


	@Test
	void withSimpleConstructorArg() {
		this.reader.loadBeanDefinitions(new ClassPathResource("simpleConstructorArg.properties", getClass()));
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		assertThat(bean.getName()).isEqualTo("Rob Harrop");
	}

	@Test
	void withConstructorArgRef() {
		this.reader.loadBeanDefinitions(new ClassPathResource("refConstructorArg.properties", getClass()));
		TestBean rob = (TestBean) this.beanFactory.getBean("rob");
		TestBean sally = (TestBean) this.beanFactory.getBean("sally");
		assertThat(rob.getSpouse()).isEqualTo(sally);
	}

	@Test
	void withMultipleConstructorsArgs() {
		this.reader.loadBeanDefinitions(new ClassPathResource("multiConstructorArgs.properties", getClass()));
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		assertThat(bean.getName()).isEqualTo("Rob Harrop");
		assertThat(bean.getAge()).isEqualTo(23);
	}

}
