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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Costin Leau
 */
@TestInstance(Lifecycle.PER_CLASS)
class ComponentBeanDefinitionParserTests {

	private final DefaultListableBeanFactory bf = new DefaultListableBeanFactory();


	@BeforeAll
	void setUp() throws Exception {
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
			new ClassPathResource("component-config.xml", ComponentBeanDefinitionParserTests.class));
	}

	@AfterAll
	void tearDown() {
		bf.destroySingletons();
	}

	@Test
	void testBionicBasic() {
		Component cp = getBionicFamily();
		assertThat("Bionic-1").isEqualTo(cp.getName());
	}

	@Test
	void testBionicFirstLevelChildren() {
		Component cp = getBionicFamily();
		List<Component> components = cp.getComponents();
		assertThat(2).isEqualTo(components.size());
		assertThat("Mother-1").isEqualTo(components.get(0).getName());
		assertThat("Rock-1").isEqualTo(components.get(1).getName());
	}

	@Test
	void testBionicSecondLevelChildren() {
		Component cp = getBionicFamily();
		List<Component> components = cp.getComponents().get(0).getComponents();
		assertThat(2).isEqualTo(components.size());
		assertThat("Karate-1").isEqualTo(components.get(0).getName());
		assertThat("Sport-1").isEqualTo(components.get(1).getName());
	}

	private Component getBionicFamily() {
		return bf.getBean("bionic-family", Component.class);
	}

}

