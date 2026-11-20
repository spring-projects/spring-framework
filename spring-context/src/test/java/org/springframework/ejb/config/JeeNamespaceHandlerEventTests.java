/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.ejb.config;

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
 * @author Chris Beams
 */
class JeeNamespaceHandlerEventTests {

	private final CollectingReaderEventListener eventListener = new CollectingReaderEventListener();

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private XmlBeanDefinitionReader reader;


	@BeforeEach
	void setup() {
		this.reader = new XmlBeanDefinitionReader(this.beanFactory);
		this.reader.setEventListener(this.eventListener);
		this.reader.loadBeanDefinitions(new ClassPathResource("jeeNamespaceHandlerTests.xml", getClass()));
	}


	@Test
	void jndiLookupComponentEventReceived() {
		ComponentDefinition component = this.eventListener.getComponentDefinition("simple");
		assertThat(component).isInstanceOf(BeanComponentDefinition.class);
	}

	@Test
	void localSlsbComponentEventReceived() {
		ComponentDefinition component = this.eventListener.getComponentDefinition("simpleLocalEjb");
		assertThat(component).isInstanceOf(BeanComponentDefinition.class);
	}

	@Test
	void remoteSlsbComponentEventReceived() {
		ComponentDefinition component = this.eventListener.getComponentDefinition("simpleRemoteEjb");
		assertThat(component).isInstanceOf(BeanComponentDefinition.class);
	}

}
