/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void setUp() {
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
			new ClassPathResource("component-config.xml", ComponentBeanDefinitionParserTests.class));
	}

	@AfterAll
	void tearDown() {
		bf.destroySingletons();
	}

	@Test
	void bionicBasic() {
		Component cp = getBionicFamily();
		assertThat(cp.getName()).isEqualTo("Bionic-1");
	}

	@Test
	void bionicFirstLevelChildren() {
		Component cp = getBionicFamily();
		List<Component> components = cp.getComponents();
		assertThat(components).hasSize(2);
		assertThat(components.get(0).getName()).isEqualTo("Mother-1");
		assertThat(components.get(1).getName()).isEqualTo("Rock-1");
	}

	@Test
	void bionicSecondLevelChildren() {
		Component cp = getBionicFamily();
		List<Component> components = cp.getComponents().get(0).getComponents();
		assertThat(components).hasSize(2);
		assertThat(components.get(0).getName()).isEqualTo("Karate-1");
		assertThat(components.get(1).getName()).isEqualTo("Sport-1");
	}

	private Component getBionicFamily() {
		return bf.getBean("bionic-family", Component.class);
	}

}

