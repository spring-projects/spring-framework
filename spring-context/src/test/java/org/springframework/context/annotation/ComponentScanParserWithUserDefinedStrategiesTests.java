/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 */
class ComponentScanParserWithUserDefinedStrategiesTests {

	@Test
	void customBeanNameGenerator() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/context/annotation/customNameGeneratorTests.xml");
		assertThat(context.containsBean("testing.fooServiceImpl")).isTrue();
		context.close();
	}

	@Test
	void customScopeMetadataResolver() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/context/annotation/customScopeResolverTests.xml");
		BeanDefinition bd = context.getBeanFactory().getBeanDefinition("fooServiceImpl");
		assertThat(bd.getScope()).isEqualTo("myCustomScope");
		assertThat(bd.isSingleton()).isFalse();
		context.close();
	}

	@Test
	void invalidConstructorBeanNameGenerator() {
		assertThatExceptionOfType(BeansException.class).isThrownBy(() ->
			new ClassPathXmlApplicationContext(
					"org/springframework/context/annotation/invalidConstructorNameGeneratorTests.xml"));
	}

	@Test
	void invalidClassNameScopeMetadataResolver() {
		assertThatExceptionOfType(BeansException.class).isThrownBy(() ->
				new ClassPathXmlApplicationContext(
						"org/springframework/context/annotation/invalidClassNameScopeResolverTests.xml"));
	}

}
