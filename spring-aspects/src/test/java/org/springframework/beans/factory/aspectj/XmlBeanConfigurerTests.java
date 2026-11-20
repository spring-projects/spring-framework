/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.aspectj;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Chris Beams
 */
class XmlBeanConfigurerTests {

	@Test
	void injection() {
		try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/beans/factory/aspectj/beanConfigurerTests.xml")) {

			ShouldBeConfiguredBySpring myObject = new ShouldBeConfiguredBySpring();
			assertThat(myObject.getName()).isEqualTo("Rod");
		}
	}

}
