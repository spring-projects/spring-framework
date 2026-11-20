/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
