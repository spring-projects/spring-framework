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

package org.springframework.beans.factory.xml;

import java.util.List;

import junit.framework.TestCase;
import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.parsing.AliasDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.ComponentDefinition;
import org.springframework.beans.factory.parsing.DefaultsDefinition;
import org.springframework.beans.factory.parsing.ImportDefinition;
import org.springframework.beans.factory.parsing.PassThroughSourceExtractor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class EventPublicationTests extends TestCase {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final CollectingReaderEventListener eventListener = new CollectingReaderEventListener();


	protected void setUp() throws Exception {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.setEventListener(this.eventListener);
		reader.setSourceExtractor(new PassThroughSourceExtractor());
		reader.loadBeanDefinitions(new ClassPathResource("beanEvents.xml", getClass()));
	}

	public void testDefaultsEventReceived() throws Exception {
		List<DefaultsDefinition> defaultsList = this.eventListener.getDefaults();
		assertTrue(!defaultsList.isEmpty());
		assertTrue(defaultsList.get(0) instanceof DocumentDefaultsDefinition);
		DocumentDefaultsDefinition defaults = (DocumentDefaultsDefinition) defaultsList.get(0);
		assertEquals("true", defaults.getLazyInit());
		assertEquals("constructor", defaults.getAutowire());
		assertEquals("objects", defaults.getDependencyCheck());
		assertEquals("myInit", defaults.getInitMethod());
		assertEquals("myDestroy", defaults.getDestroyMethod());
		assertEquals("true", defaults.getMerge());
		assertTrue(defaults.getSource() instanceof Element);
	}

	public void testBeanEventReceived() throws Exception {
		ComponentDefinition componentDefinition1 = this.eventListener.getComponentDefinition("testBean");
		assertTrue(componentDefinition1 instanceof BeanComponentDefinition);
		assertEquals(1, componentDefinition1.getBeanDefinitions().length);
		BeanDefinition beanDefinition1 = componentDefinition1.getBeanDefinitions()[0];
		assertEquals(new TypedStringValue("Rob Harrop"),
				beanDefinition1.getConstructorArgumentValues().getGenericArgumentValue(String.class).getValue());
		assertEquals(1, componentDefinition1.getBeanReferences().length);
		assertEquals("testBean2", componentDefinition1.getBeanReferences()[0].getBeanName());
		assertEquals(1, componentDefinition1.getInnerBeanDefinitions().length);
		BeanDefinition innerBd1 = componentDefinition1.getInnerBeanDefinitions()[0];
		assertEquals(new TypedStringValue("ACME"),
				innerBd1.getConstructorArgumentValues().getGenericArgumentValue(String.class).getValue());
		assertTrue(componentDefinition1.getSource() instanceof Element);

		ComponentDefinition componentDefinition2 = this.eventListener.getComponentDefinition("testBean2");
		assertTrue(componentDefinition2 instanceof BeanComponentDefinition);
		assertEquals(1, componentDefinition1.getBeanDefinitions().length);
		BeanDefinition beanDefinition2 = componentDefinition2.getBeanDefinitions()[0];
		assertEquals(new TypedStringValue("Juergen Hoeller"),
				beanDefinition2.getPropertyValues().getPropertyValue("name").getValue());
		assertEquals(0, componentDefinition2.getBeanReferences().length);
		assertEquals(1, componentDefinition2.getInnerBeanDefinitions().length);
		BeanDefinition innerBd2 = componentDefinition2.getInnerBeanDefinitions()[0];
		assertEquals(new TypedStringValue("Eva Schallmeiner"),
				innerBd2.getPropertyValues().getPropertyValue("name").getValue());
		assertTrue(componentDefinition2.getSource() instanceof Element);
	}

	public void testAliasEventReceived() throws Exception {
		List<AliasDefinition> aliases = this.eventListener.getAliases("testBean");
		assertEquals(2, aliases.size());
		AliasDefinition aliasDefinition1 = aliases.get(0);
		assertEquals("testBeanAlias1", aliasDefinition1.getAlias());
		assertTrue(aliasDefinition1.getSource() instanceof Element);
		AliasDefinition aliasDefinition2 = aliases.get(1);
		assertEquals("testBeanAlias2", aliasDefinition2.getAlias());
		assertTrue(aliasDefinition2.getSource() instanceof Element);
	}

	public void testImportEventReceived() throws Exception {
		List<ImportDefinition> imports = this.eventListener.getImports();
		assertEquals(1, imports.size());
		ImportDefinition importDefinition = imports.get(0);
		assertEquals("beanEventsImported.xml", importDefinition.getImportedResource());
		assertTrue(importDefinition.getSource() instanceof Element);
	}

}
