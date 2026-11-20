/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.xml;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
class CollectionsWithDefaultTypesTests {

	private final DefaultListableBeanFactory beanFactory;

	public CollectionsWithDefaultTypesTests() {
		this.beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
				new ClassPathResource("collectionsWithDefaultTypes.xml", getClass()));
	}

	@Test
	void listHasDefaultType() {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		for (Object o : bean.getSomeList()) {
			assertThat(o.getClass()).as("Value type is incorrect").isEqualTo(Integer.class);
		}
	}

	@Test
	void setHasDefaultType() {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		for (Object o : bean.getSomeSet()) {
			assertThat(o.getClass()).as("Value type is incorrect").isEqualTo(Integer.class);
		}
	}

	@Test
	void mapHasDefaultKeyAndValueType() {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		assertMap(bean.getSomeMap());
	}

	@Test
	void mapWithNestedElementsHasDefaultKeyAndValueType() {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean2");
		assertMap(bean.getSomeMap());
	}

	@SuppressWarnings("rawtypes")
	private void assertMap(Map<?,?> map) {
		for (Map.Entry entry : map.entrySet()) {
			assertThat(entry.getKey().getClass()).as("Key type is incorrect").isEqualTo(Integer.class);
			assertThat(entry.getValue().getClass()).as("Value type is incorrect").isEqualTo(Boolean.class);
		}
	}

	@Test
	@SuppressWarnings("rawtypes")
	void buildCollectionFromMixtureOfReferencesAndValues() {
		MixedCollectionBean jumble = (MixedCollectionBean) this.beanFactory.getBean("jumble");
		assertThat(jumble.getJumble()).as("Expected 3 elements, not " + jumble.getJumble().size()).hasSize(3);
		List l = (List) jumble.getJumble();
		assertThat(l.get(0)).isEqualTo("literal");
		Integer[] array1 = (Integer[]) l.get(1);
		assertThat(array1[0]).isEqualTo(2);
		assertThat(array1[1]).isEqualTo(4);
		int[] array2 = (int[]) l.get(2);
		assertThat(array2[0]).isEqualTo(3);
		assertThat(array2[1]).isEqualTo(5);
	}

}
