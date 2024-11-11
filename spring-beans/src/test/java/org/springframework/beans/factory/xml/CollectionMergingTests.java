/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit and integration tests for the collection merging support.
 *
 * @author Rob Harrop
 * @author Rick Evans
 */
@SuppressWarnings("rawtypes")
class CollectionMergingTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();


	@BeforeEach
	void setUp() {
		BeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.loadBeanDefinitions(new ClassPathResource("collectionMerging.xml", getClass()));
	}

	@Test
	void mergeList() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithList");
		List<?> list = bean.getSomeList();
		assertThat(list).asInstanceOf(InstanceOfAssertFactories.list(String.class))
				.containsExactly("Rob Harrop", "Rod Johnson", "Juergen Hoeller");
	}

	@Test
	void mergeListWithInnerBeanAsListElement() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithListOfRefs");
		List<?> list = bean.getSomeList();
		assertThat(list).isNotNull();
		assertThat(list).hasSize(3);
		assertThat(list.get(2)).isInstanceOf(TestBean.class);
	}

	@Test
	void mergeSet() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithSet");
		Set<?> set = bean.getSomeSet();
		assertThat(set).asInstanceOf(InstanceOfAssertFactories.collection(String.class))
				.containsOnly("Rob Harrop", "Sally Greenwood");
	}

	@Test
	void mergeSetWithInnerBeanAsSetElement() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithSetOfRefs");
		Set<?> set = bean.getSomeSet();
		assertThat(set).isNotNull();
		assertThat(set).hasSize(2);
		Iterator it = set.iterator();
		it.next();
		Object o = it.next();
		assertThat(o).isInstanceOf(TestBean.class);
		assertThat(((TestBean) o).getName()).isEqualTo("Sally");
	}

	@Test
	void mergeMap() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithMap");
		Map<?, ?> map = bean.getSomeMap();
		assertThat(map).asInstanceOf(InstanceOfAssertFactories.map(String.class,String.class))
				.containsOnly(entry("Rob", "Sally"), entry("Rod", "Kerry"), entry("Juergen", "Eva"));
	}

	@Test
	void mergeMapWithInnerBeanAsMapEntryValue() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithMapOfRefs");
		Map<?, ?> map = bean.getSomeMap();
		assertThat(map).isNotNull();
		assertThat(map).hasSize(2);
		assertThat(map.get("Rob")).isNotNull();
		assertThat(map.get("Rob")).isInstanceOf(TestBean.class);
		assertThat(((TestBean) map.get("Rob")).getName()).isEqualTo("Sally");
	}

	@Test
	void mergeProperties() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithProps");
		Properties props = bean.getSomeProperties();
		assertThat(props).as("Incorrect size").hasSize(3);
		assertThat(props.getProperty("Rob")).isEqualTo("Sally");
		assertThat(props.getProperty("Rod")).isEqualTo("Kerry");
		assertThat(props.getProperty("Juergen")).isEqualTo("Eva");
	}

	@Test
	void mergeListInConstructor() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithListInConstructor");
		List<?> list = bean.getSomeList();
		assertThat(list).asInstanceOf(InstanceOfAssertFactories.list(String.class))
				.containsExactly("Rob Harrop", "Rod Johnson", "Juergen Hoeller");
	}

	@Test
	void mergeListWithInnerBeanAsListElementInConstructor() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithListOfRefsInConstructor");
		List<?> list = bean.getSomeList();
		assertThat(list).hasSize(3).element(2).isInstanceOf(TestBean.class);
	}

	@Test
	void mergeSetInConstructor() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithSetInConstructor");
		Set<?> set = bean.getSomeSet();
		assertThat(set).as("Incorrect size").hasSize(2);
		assertThat(set.contains("Rob Harrop")).isTrue();
		assertThat(set.contains("Sally Greenwood")).isTrue();
	}

	@Test
	void mergeSetWithInnerBeanAsSetElementInConstructor() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithSetOfRefsInConstructor");
		Set<?> set = bean.getSomeSet();
		assertThat(set).isNotNull();
		assertThat(set).hasSize(2);
		Iterator it = set.iterator();
		it.next();
		Object o = it.next();
		assertThat(o).isInstanceOf(TestBean.class);
		assertThat(((TestBean) o).getName()).isEqualTo("Sally");
	}

	@Test
	void mergeMapInConstructor() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithMapInConstructor");
		Map<?, ?> map = bean.getSomeMap();
		assertThat(map).as("Incorrect size").hasSize(3);
		assertThat(map.get("Rob")).isEqualTo("Sally");
		assertThat(map.get("Rod")).isEqualTo("Kerry");
		assertThat(map.get("Juergen")).isEqualTo("Eva");
	}

	@Test
	void mergeMapWithInnerBeanAsMapEntryValueInConstructor() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithMapOfRefsInConstructor");
		Map<?, ?> map = bean.getSomeMap();
		assertThat(map).isNotNull();
		assertThat(map).hasSize(2);
		assertThat(map.get("Rob")).isInstanceOf(TestBean.class);
		assertThat(((TestBean) map.get("Rob")).getName()).isEqualTo("Sally");
	}

	@Test
	void mergePropertiesInConstructor() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithPropsInConstructor");
		Properties props = bean.getSomeProperties();
		assertThat(props).as("Incorrect size").hasSize(3);
		assertThat(props.getProperty("Rob")).isEqualTo("Sally");
		assertThat(props.getProperty("Rod")).isEqualTo("Kerry");
		assertThat(props.getProperty("Juergen")).isEqualTo("Eva");
	}

}
