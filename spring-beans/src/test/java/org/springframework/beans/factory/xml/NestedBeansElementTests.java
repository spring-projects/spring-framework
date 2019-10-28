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
public class NestedBeansElementTests {
	private final Resource XML =
		new ClassPathResource("NestedBeansElementTests-context.xml", this.getClass());

	@Test
	public void getBean_withoutActiveProfile() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(XML);

		Object foo = bf.getBean("foo");
		assertThat(foo).isInstanceOf(String.class);
	}

	@Test
	public void getBean_withActiveProfile() {
		ConfigurableEnvironment env = new StandardEnvironment();
		env.setActiveProfiles("dev");

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
		reader.setEnvironment(env);
		reader.loadBeanDefinitions(XML);

		bf.getBean("devOnlyBean"); // should not throw NSBDE

		Object foo = bf.getBean("foo");
		assertThat(foo).isInstanceOf(Integer.class);

		bf.getBean("devOnlyBean");
	}

}
