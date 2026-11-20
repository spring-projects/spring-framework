/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for new nested beans element support in Spring XML
 *
 * @author Chris Beams
 */
class NestedBeansElementTests {

	private final Resource XML = new ClassPathResource("NestedBeansElementTests-context.xml", getClass());

	@Test
	void getBean_withoutActiveProfile() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(XML);

		Object foo = bf.getBean("foo");
		assertThat(foo).isInstanceOf(String.class);
	}

	@Test
	void getBean_withActiveProfile() {
		ConfigurableEnvironment env = new StandardEnvironment();
		env.setActiveProfiles("dev");

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setAllowBeanDefinitionOverriding(true);
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
		reader.setEnvironment(env);
		reader.loadBeanDefinitions(XML);

		bf.getBean("devOnlyBean"); // should not throw NSBDE

		Object foo = bf.getBean("foo");
		assertThat(foo).isInstanceOf(Integer.class);

		bf.getBean("devOnlyBean");
	}

}
