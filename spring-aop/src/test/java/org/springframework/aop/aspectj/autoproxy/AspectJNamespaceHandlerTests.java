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

package org.springframework.aop.aspectj.autoproxy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.config.AopConfigUtils;
import org.springframework.aop.config.AopNamespaceUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.PassThroughSourceExtractor;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.beans.testfixture.beans.CollectingReaderEventListener;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
class AspectJNamespaceHandlerTests {

	private ParserContext parserContext;

	private CollectingReaderEventListener readerEventListener = new CollectingReaderEventListener();

	private BeanDefinitionRegistry registry = new DefaultListableBeanFactory();


	@BeforeEach
	public void setUp() {
		SourceExtractor sourceExtractor = new PassThroughSourceExtractor();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.registry);
		XmlReaderContext readerContext =
				new XmlReaderContext(null, null, this.readerEventListener, sourceExtractor, reader, null);
		this.parserContext = new ParserContext(readerContext, null);
	}

	@Test
	void testRegisterAutoProxyCreator() {
		AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(this.parserContext, null);
		assertThat(registry.getBeanDefinitionCount()).as("Incorrect number of definitions registered").isEqualTo(1);

		AopNamespaceUtils.registerAspectJAutoProxyCreatorIfNecessary(this.parserContext, null);
		assertThat(registry.getBeanDefinitionCount()).as("Incorrect number of definitions registered").isEqualTo(1);
	}

	@Test
	void testRegisterAspectJAutoProxyCreator() {
		AopNamespaceUtils.registerAspectJAutoProxyCreatorIfNecessary(this.parserContext, null);
		assertThat(registry.getBeanDefinitionCount()).as("Incorrect number of definitions registered").isEqualTo(1);

		AopNamespaceUtils.registerAspectJAutoProxyCreatorIfNecessary(this.parserContext, null);
		assertThat(registry.getBeanDefinitionCount()).as("Incorrect number of definitions registered").isEqualTo(1);

		BeanDefinition definition = registry.getBeanDefinition(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
		assertThat(definition.getBeanClassName()).as("Incorrect APC class").isEqualTo(AspectJAwareAdvisorAutoProxyCreator.class.getName());
	}

	@Test
	void testRegisterAspectJAutoProxyCreatorWithExistingAutoProxyCreator() {
		AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(this.parserContext, null);
		assertThat(registry.getBeanDefinitionCount()).isEqualTo(1);

		AopNamespaceUtils.registerAspectJAutoProxyCreatorIfNecessary(this.parserContext, null);
		assertThat(registry.getBeanDefinitionCount()).as("Incorrect definition count").isEqualTo(1);

		BeanDefinition definition = registry.getBeanDefinition(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
		assertThat(definition.getBeanClassName()).as("APC class not switched").isEqualTo(AspectJAwareAdvisorAutoProxyCreator.class.getName());
	}

	@Test
	void testRegisterAutoProxyCreatorWhenAspectJAutoProxyCreatorAlreadyExists() {
		AopNamespaceUtils.registerAspectJAutoProxyCreatorIfNecessary(this.parserContext, null);
		assertThat(registry.getBeanDefinitionCount()).isEqualTo(1);

		AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(this.parserContext, null);
		assertThat(registry.getBeanDefinitionCount()).as("Incorrect definition count").isEqualTo(1);

		BeanDefinition definition = registry.getBeanDefinition(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
		assertThat(definition.getBeanClassName()).as("Incorrect APC class").isEqualTo(AspectJAwareAdvisorAutoProxyCreator.class.getName());
	}

}
