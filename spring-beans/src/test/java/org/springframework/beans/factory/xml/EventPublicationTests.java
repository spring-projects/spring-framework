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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.parsing.AliasDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.ComponentDefinition;
import org.springframework.beans.factory.parsing.ImportDefinition;
import org.springframework.beans.factory.parsing.PassThroughSourceExtractor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.tests.beans.CollectingReaderEventListener;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
@SuppressWarnings("rawtypes")
public class EventPublicationTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final CollectingReaderEventListener eventListener = new CollectingReaderEventListener();



	@BeforeEach
	public void setUp() throws Exception {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.setEventListener(this.eventListener);
		reader.setSourceExtractor(new PassThroughSourceExtractor());
		reader.loadBeanDefinitions(new ClassPathResource("beanEvents.xml", getClass()));
	}

	@Test
	public void defaultsEventReceived() throws Exception {
		List defaultsList = this.eventListener.getDefaults();
		boolean condition2 = !defaultsList.isEmpty();
		assertThat(condition2).isTrue();
		boolean condition1 = defaultsList.get(0) instanceof DocumentDefaultsDefinition;
		assertThat(condition1).isTrue();
		DocumentDefaultsDefinition defaults = (DocumentDefaultsDefinition) defaultsList.get(0);
		assertThat(defaults.getLazyInit()).isEqualTo("true");
		assertThat(defaults.getAutowire()).isEqualTo("constructor");
		assertThat(defaults.getInitMethod()).isEqualTo("myInit");
		assertThat(defaults.getDestroyMethod()).isEqualTo("myDestroy");
		assertThat(defaults.getMerge()).isEqualTo("true");
		boolean condition = defaults.getSource() instanceof Element;
		assertThat(condition).isTrue();
	}

	@Test
	public void beanEventReceived() throws Exception {
		ComponentDefinition componentDefinition1 = this.eventListener.getComponentDefinition("testBean");
		boolean condition3 = componentDefinition1 instanceof BeanComponentDefinition;
		assertThat(condition3).isTrue();
		assertThat(componentDefinition1.getBeanDefinitions().length).isEqualTo(1);
		BeanDefinition beanDefinition1 = componentDefinition1.getBeanDefinitions()[0];
		assertThat(beanDefinition1.getConstructorArgumentValues().getGenericArgumentValue(String.class).getValue()).isEqualTo(new TypedStringValue("Rob Harrop"));
		assertThat(componentDefinition1.getBeanReferences().length).isEqualTo(1);
		assertThat(componentDefinition1.getBeanReferences()[0].getBeanName()).isEqualTo("testBean2");
		assertThat(componentDefinition1.getInnerBeanDefinitions().length).isEqualTo(1);
		BeanDefinition innerBd1 = componentDefinition1.getInnerBeanDefinitions()[0];
		assertThat(innerBd1.getConstructorArgumentValues().getGenericArgumentValue(String.class).getValue()).isEqualTo(new TypedStringValue("ACME"));
		boolean condition2 = componentDefinition1.getSource() instanceof Element;
		assertThat(condition2).isTrue();

		ComponentDefinition componentDefinition2 = this.eventListener.getComponentDefinition("testBean2");
		boolean condition1 = componentDefinition2 instanceof BeanComponentDefinition;
		assertThat(condition1).isTrue();
		assertThat(componentDefinition1.getBeanDefinitions().length).isEqualTo(1);
		BeanDefinition beanDefinition2 = componentDefinition2.getBeanDefinitions()[0];
		assertThat(beanDefinition2.getPropertyValues().getPropertyValue("name").getValue()).isEqualTo(new TypedStringValue("Juergen Hoeller"));
		assertThat(componentDefinition2.getBeanReferences().length).isEqualTo(0);
		assertThat(componentDefinition2.getInnerBeanDefinitions().length).isEqualTo(1);
		BeanDefinition innerBd2 = componentDefinition2.getInnerBeanDefinitions()[0];
		assertThat(innerBd2.getPropertyValues().getPropertyValue("name").getValue()).isEqualTo(new TypedStringValue("Eva Schallmeiner"));
		boolean condition = componentDefinition2.getSource() instanceof Element;
		assertThat(condition).isTrue();
	}

	@Test
	public void aliasEventReceived() throws Exception {
		List aliases = this.eventListener.getAliases("testBean");
		assertThat(aliases.size()).isEqualTo(2);
		AliasDefinition aliasDefinition1 = (AliasDefinition) aliases.get(0);
		assertThat(aliasDefinition1.getAlias()).isEqualTo("testBeanAlias1");
		boolean condition1 = aliasDefinition1.getSource() instanceof Element;
		assertThat(condition1).isTrue();
		AliasDefinition aliasDefinition2 = (AliasDefinition) aliases.get(1);
		assertThat(aliasDefinition2.getAlias()).isEqualTo("testBeanAlias2");
		boolean condition = aliasDefinition2.getSource() instanceof Element;
		assertThat(condition).isTrue();
	}

	@Test
	public void importEventReceived() throws Exception {
		List imports = this.eventListener.getImports();
		assertThat(imports.size()).isEqualTo(1);
		ImportDefinition importDefinition = (ImportDefinition) imports.get(0);
		assertThat(importDefinition.getImportedResource()).isEqualTo("beanEventsImported.xml");
		boolean condition = importDefinition.getSource() instanceof Element;
		assertThat(condition).isTrue();
	}

}
