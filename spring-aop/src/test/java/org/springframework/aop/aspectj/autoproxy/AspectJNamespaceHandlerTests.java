/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.aspectj.autoproxy;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
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

import test.parsing.CollectingReaderEventListener;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
public final class AspectJNamespaceHandlerTests {

	private ParserContext parserContext;

	private CollectingReaderEventListener readerEventListener = new CollectingReaderEventListener();

	private BeanDefinitionRegistry registry = new DefaultListableBeanFactory();


	@Before
	public void setUp() throws Exception {
		SourceExtractor sourceExtractor = new PassThroughSourceExtractor();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.registry);
		XmlReaderContext readerContext =
				new XmlReaderContext(null, null, this.readerEventListener, sourceExtractor, reader, null);
		this.parserContext = new ParserContext(readerContext, null);
	}

	@Test
	public void testRegisterAutoProxyCreator() throws Exception {
		AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(this.parserContext, null);
		assertEquals("Incorrect number of definitions registered", 1, registry.getBeanDefinitionCount());

		AopNamespaceUtils.registerAspectJAutoProxyCreatorIfNecessary(this.parserContext, null);
		assertEquals("Incorrect number of definitions registered", 1, registry.getBeanDefinitionCount());
	}

	@Test
	public void testRegisterAspectJAutoProxyCreator() throws Exception {
		AopNamespaceUtils.registerAspectJAutoProxyCreatorIfNecessary(this.parserContext, null);
		assertEquals("Incorrect number of definitions registered", 1, registry.getBeanDefinitionCount());

		AopNamespaceUtils.registerAspectJAutoProxyCreatorIfNecessary(this.parserContext, null);
		assertEquals("Incorrect number of definitions registered", 1, registry.getBeanDefinitionCount());

		BeanDefinition definition = registry.getBeanDefinition(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
		assertEquals("Incorrect APC class",
				AspectJAwareAdvisorAutoProxyCreator.class.getName(), definition.getBeanClassName());
	}

	@Test
	public void testRegisterAspectJAutoProxyCreatorWithExistingAutoProxyCreator() throws Exception {
		AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(this.parserContext, null);
		assertEquals(1, registry.getBeanDefinitionCount());

		AopNamespaceUtils.registerAspectJAutoProxyCreatorIfNecessary(this.parserContext, null);
		assertEquals("Incorrect definition count", 1, registry.getBeanDefinitionCount());

		BeanDefinition definition = registry.getBeanDefinition(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
		assertEquals("APC class not switched",
				AspectJAwareAdvisorAutoProxyCreator.class.getName(), definition.getBeanClassName());
	}

	@Test
	public void testRegisterAutoProxyCreatorWhenAspectJAutoProxyCreatorAlreadyExists() throws Exception {
		AopNamespaceUtils.registerAspectJAutoProxyCreatorIfNecessary(this.parserContext, null);
		assertEquals(1, registry.getBeanDefinitionCount());

		AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(this.parserContext, null);
		assertEquals("Incorrect definition count", 1, registry.getBeanDefinitionCount());

		BeanDefinition definition = registry.getBeanDefinition(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
		assertEquals("Incorrect APC class",
				AspectJAwareAdvisorAutoProxyCreator.class.getName(), definition.getBeanClassName());
	}

}
