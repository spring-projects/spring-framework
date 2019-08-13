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

package org.springframework.beans.factory.xml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class BeanNameGenerationTests {

	private DefaultListableBeanFactory beanFactory;


	@BeforeEach
	public void setUp() {
		this.beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(new ClassPathResource("beanNameGeneration.xml", getClass()));
	}

	@Test
	public void naming() {
		String className = GeneratedNameBean.class.getName();

		String targetName = className + BeanDefinitionReaderUtils.GENERATED_BEAN_NAME_SEPARATOR + "0";
		GeneratedNameBean topLevel1 = (GeneratedNameBean) beanFactory.getBean(targetName);
		assertThat(topLevel1).isNotNull();

		targetName = className + BeanDefinitionReaderUtils.GENERATED_BEAN_NAME_SEPARATOR + "1";
		GeneratedNameBean topLevel2 = (GeneratedNameBean) beanFactory.getBean(targetName);
		assertThat(topLevel2).isNotNull();

		GeneratedNameBean child1 = topLevel1.getChild();
		assertThat(child1.getBeanName()).isNotNull();
		assertThat(child1.getBeanName().startsWith(className)).isTrue();

		GeneratedNameBean child2 = topLevel2.getChild();
		assertThat(child2.getBeanName()).isNotNull();
		assertThat(child2.getBeanName().startsWith(className)).isTrue();

		assertThat(child1.getBeanName().equals(child2.getBeanName())).isFalse();
	}

}
