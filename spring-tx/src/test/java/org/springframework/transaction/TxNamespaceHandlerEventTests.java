/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.ComponentDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.CollectingReaderEventListener;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Torsten Juergeleit
 * @author Juergen Hoeller
 */
class TxNamespaceHandlerEventTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final CollectingReaderEventListener eventListener = new CollectingReaderEventListener();


	@BeforeEach
	void setUp() {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.setEventListener(this.eventListener);
		reader.loadBeanDefinitions(new ClassPathResource("txNamespaceHandlerTests.xml", getClass()));
	}

	@Test
	void componentEventReceived() {
		ComponentDefinition component = this.eventListener.getComponentDefinition("txAdvice");
		assertThat(component).isInstanceOf(BeanComponentDefinition.class);
	}

}
