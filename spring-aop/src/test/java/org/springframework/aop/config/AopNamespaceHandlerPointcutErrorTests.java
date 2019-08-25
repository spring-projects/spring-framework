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

package org.springframework.aop.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.tests.TestResourceUtils.qualifiedResource;

/**
 * @author Mark Fisher
 * @author Chris Beams
 */
public class AopNamespaceHandlerPointcutErrorTests {

	@Test
	public void testDuplicatePointcutConfig() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
						qualifiedResource(getClass(), "pointcutDuplication.xml")))
			.satisfies(ex -> ex.contains(BeanDefinitionParsingException.class));
	}

	@Test
	public void testMissingPointcutConfig() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
						qualifiedResource(getClass(), "pointcutMissing.xml")))
			.satisfies(ex -> ex.contains(BeanDefinitionParsingException.class));
	}

}
