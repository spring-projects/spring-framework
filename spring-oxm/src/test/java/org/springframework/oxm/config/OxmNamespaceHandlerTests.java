/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.oxm.config;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link OxmNamespaceHandler} class.
 *
 * @author Arjen Poustma
 * @author Jakub Narloch
 * @author Sam Brannen
 */
class OxmNamespaceHandlerTests {

	private final ApplicationContext applicationContext =
			new ClassPathXmlApplicationContext("oxmNamespaceHandlerTest.xml", getClass());


	@Test
	void jaxb2ContextPathMarshaller() {
		Jaxb2Marshaller jaxb2Marshaller = applicationContext.getBean("jaxb2ContextPathMarshaller", Jaxb2Marshaller.class);
		assertThat(jaxb2Marshaller).isNotNull();
	}

	@Test
	void jaxb2ClassesToBeBoundMarshaller() {
		Jaxb2Marshaller jaxb2Marshaller = applicationContext.getBean("jaxb2ClassesMarshaller", Jaxb2Marshaller.class);
		assertThat(jaxb2Marshaller).isNotNull();
	}

}
