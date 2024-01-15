/*
 * Copyright 2002-2024 the original author or authors.
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
import org.springframework.beans.factory.parsing.DefaultsDefinition;
import org.springframework.beans.factory.parsing.ImportDefinition;
import org.springframework.beans.factory.parsing.PassThroughSourceExtractor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.CollectingReaderEventListener;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
class EventPublicationTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final CollectingReaderEventListener eventListener = new CollectingReaderEventListener();



	@BeforeEach
	void setUp() {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.setEventListener(this.eventListener);
		reader.setSourceExtractor(new PassThroughSourceExtractor());
		reader.loadBeanDefinitions(new ClassPathResource("beanEvents.xml", getClass()));
	}

	@Test
	void defaultsEventReceived() {
		List<DefaultsDefinition> defaultsList = this.eventListener.getDefaults();
		assertThat(defaultsList).element(0).isInstanceOf(DocumentDefaultsDefinition.class);
		DocumentDefaultsDefinition defaults = (DocumentDefaultsDefinition) defaultsList.get(0);
		assertThat(defaults.getLazyInit()).isEqualTo("true");
		assertThat(defaults.getAutowire()).isEqualTo("constructor");
		assertThat(defaults.getInitMethod()).isEqualTo("myInit");
		assertThat(defaults.getDestroyMethod()).isEqualTo("myDestroy");
		assertThat(defaults.getMerge()).isEqualTo("true");
		assertThat(defaults.getSource()).isInstanceOf(Element.class);
	}

	@Test
	void beanEventReceived() {
		ComponentDefinition componentDefinition1 = this.eventListener.getComponentDefinition("testBean");
		assertThat(componentDefinition1).isInstanceOf(BeanComponentDefinition.class);
		assertThat(componentDefinition1.getBeanDefinitions()).hasSize(1);
		BeanDefinition beanDefinition1 = componentDefinition1.getBeanDefinitions()[0];
		assertThat(beanDefinition1.getConstructorArgumentValues().getGenericArgumentValue(String.class).getValue()).isEqualTo(new TypedStringValue("Rob Harrop"));
		assertThat(componentDefinition1.getBeanReferences()).hasSize(1);
		assertThat(componentDefinition1.getBeanReferences()[0].getBeanName()).isEqualTo("testBean2");
		assertThat(componentDefinition1.getInnerBeanDefinitions()).hasSize(1);
		BeanDefinition innerBd1 = componentDefinition1.getInnerBeanDefinitions()[0];
		assertThat(innerBd1.getConstructorArgumentValues().getGenericArgumentValue(String.class).getValue()).isEqualTo(new TypedStringValue("ACME"));
		assertThat(componentDefinition1.getSource()).isInstanceOf(Element.class);

		ComponentDefinition componentDefinition2 = this.eventListener.getComponentDefinition("testBean2");
		assertThat(componentDefinition2).isInstanceOf(BeanComponentDefinition.class);
		assertThat(componentDefinition1.getBeanDefinitions()).hasSize(1);
		BeanDefinition beanDefinition2 = componentDefinition2.getBeanDefinitions()[0];
		assertThat(beanDefinition2.getPropertyValues().getPropertyValue("name").getValue()).isEqualTo(new TypedStringValue("Juergen Hoeller"));
		assertThat(componentDefinition2.getBeanReferences()).isEmpty();
		assertThat(componentDefinition2.getInnerBeanDefinitions()).hasSize(1);
		BeanDefinition innerBd2 = componentDefinition2.getInnerBeanDefinitions()[0];
		assertThat(innerBd2.getPropertyValues().getPropertyValue("name").getValue()).isEqualTo(new TypedStringValue("Eva Schallmeiner"));
		assertThat(componentDefinition2.getSource()).isInstanceOf(Element.class);
	}

	@Test
	void aliasEventReceived() {
		List<AliasDefinition> aliases = this.eventListener.getAliases("testBean");
		assertThat(aliases).hasSize(2);
		AliasDefinition aliasDefinition1 = aliases.get(0);
		assertThat(aliasDefinition1.getAlias()).isEqualTo("testBeanAlias1");
		assertThat(aliasDefinition1.getSource()).isInstanceOf(Element.class);
		AliasDefinition aliasDefinition2 = aliases.get(1);
		assertThat(aliasDefinition2.getAlias()).isEqualTo("testBeanAlias2");
		assertThat(aliasDefinition2.getSource()).isInstanceOf(Element.class);
	}

	@Test
	void importEventReceived() {
		List<ImportDefinition> imports = this.eventListener.getImports();
		assertThat(imports).hasSize(1);
		ImportDefinition importDefinition = imports.get(0);
		assertThat(importDefinition.getImportedResource()).isEqualTo("beanEventsImported.xml");
		assertThat(importDefinition.getSource()).isInstanceOf(Element.class);
	}

}
