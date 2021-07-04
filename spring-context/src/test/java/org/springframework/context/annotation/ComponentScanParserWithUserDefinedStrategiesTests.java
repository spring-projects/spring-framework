/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 */
public class ComponentScanParserWithUserDefinedStrategiesTests {

	@Test
	public void testCustomBeanNameGenerator() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/context/annotation/customNameGeneratorTests.xml");
		assertThat(context.containsBean("testing.fooServiceImpl")).isTrue();
	}

	@Test
	public void testCustomScopeMetadataResolver() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/context/annotation/customScopeResolverTests.xml");
		BeanDefinition bd = context.getBeanFactory().getBeanDefinition("fooServiceImpl");
		assertThat(bd.getScope()).isEqualTo("myCustomScope");
		assertThat(bd.isSingleton()).isFalse();
	}

	@Test
	public void testInvalidConstructorBeanNameGenerator() {
		assertThatExceptionOfType(BeansException.class).isThrownBy(() ->
			new ClassPathXmlApplicationContext(
					"org/springframework/context/annotation/invalidConstructorNameGeneratorTests.xml"));
	}

	@Test
	public void testInvalidClassNameScopeMetadataResolver() {
		assertThatExceptionOfType(BeansException.class).isThrownBy(() ->
				new ClassPathXmlApplicationContext(
						"org/springframework/context/annotation/invalidClassNameScopeResolverTests.xml"));
	}

}
