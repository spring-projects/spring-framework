/*
 * Copyright 2002-2023 the original author or authors.
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
public class CollectionsWithDefaultTypesTests {

	private final DefaultListableBeanFactory beanFactory;

	public CollectionsWithDefaultTypesTests() {
		this.beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
				new ClassPathResource("collectionsWithDefaultTypes.xml", getClass()));
	}

	@Test
	public void testListHasDefaultType() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		for (Object o : bean.getSomeList()) {
			assertThat(o.getClass()).as("Value type is incorrect").isEqualTo(Integer.class);
		}
	}

	@Test
	public void testSetHasDefaultType() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		for (Object o : bean.getSomeSet()) {
			assertThat(o.getClass()).as("Value type is incorrect").isEqualTo(Integer.class);
		}
	}

	@Test
	public void testMapHasDefaultKeyAndValueType() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		assertMap(bean.getSomeMap());
	}

	@Test
	public void testMapWithNestedElementsHasDefaultKeyAndValueType() throws Exception {
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
	public void testBuildCollectionFromMixtureOfReferencesAndValues() throws Exception {
		MixedCollectionBean jumble = (MixedCollectionBean) this.beanFactory.getBean("jumble");
		assertThat(jumble.getJumble()).as("Expected 3 elements, not " + jumble.getJumble().size()).hasSize(3);
		List l = (List) jumble.getJumble();
		assertThat(l.get(0).equals("literal")).isTrue();
		Integer[] array1 = (Integer[]) l.get(1);
		assertThat(array1[0].equals(2)).isTrue();
		assertThat(array1[1].equals(4)).isTrue();
		int[] array2 = (int[]) l.get(2);
		assertThat(array2[0]).isEqualTo(3);
		assertThat(array2[1]).isEqualTo(5);
	}

}
