/*
 * Copyright 2002-2023 the original author or authors.
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
