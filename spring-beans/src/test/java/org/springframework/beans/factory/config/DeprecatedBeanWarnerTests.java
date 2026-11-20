/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class DeprecatedBeanWarnerTests {

	private String beanName;

	private BeanDefinition beanDefinition;


	@Test
	@SuppressWarnings("deprecation")
	void postProcess() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition def = new RootBeanDefinition(MyDeprecatedBean.class);
		String beanName = "deprecated";
		beanFactory.registerBeanDefinition(beanName, def);

		DeprecatedBeanWarner warner = new MyDeprecatedBeanWarner();
		warner.postProcessBeanFactory(beanFactory);
		assertThat(this.beanName).isEqualTo(beanName);
		assertThat(this.beanDefinition).isEqualTo(def);
	}


	private class MyDeprecatedBeanWarner extends DeprecatedBeanWarner {

		@Override
		protected void logDeprecatedBean(String beanName, Class<?> beanType, BeanDefinition beanDefinition) {
			DeprecatedBeanWarnerTests.this.beanName = beanName;
			DeprecatedBeanWarnerTests.this.beanDefinition = beanDefinition;
		}
	}

}
