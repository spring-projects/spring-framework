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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit and integration tests for the collection merging support.
 *
 * @author Rob Harrop
 * @author Rick Evans
 */
@SuppressWarnings("rawtypes")
public class CollectionMergingTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();


	@BeforeEach
	public void setUp() throws Exception {
		BeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.loadBeanDefinitions(new ClassPathResource("collectionMerging.xml", getClass()));
	}

	@Test
	public void mergeList() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithList");
		List list = bean.getSomeList();
		assertThat(list.size()).as("Incorrect size").isEqualTo(3);
		assertThat(list.get(0)).isEqualTo("Rob Harrop");
		assertThat(list.get(1)).isEqualTo("Rod Johnson");
		assertThat(list.get(2)).isEqualTo("Juergen Hoeller");
	}

	@Test
	public void mergeListWithInnerBeanAsListElement() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithListOfRefs");
		List<?> list = bean.getSomeList();
		assertThat(list).isNotNull();
		assertThat(list.size()).isEqualTo(3);
		assertThat(list.get(2)).isNotNull();
		boolean condition = list.get(2) instanceof TestBean;
		assertThat(condition).isTrue();
	}

	@Test
	public void mergeSet() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithSet");
		Set set = bean.getSomeSet();
		assertThat(set.size()).as("Incorrect size").isEqualTo(2);
		assertThat(set.contains("Rob Harrop")).isTrue();
		assertThat(set.contains("Sally Greenwood")).isTrue();
	}

	@Test
	public void mergeSetWithInnerBeanAsSetElement() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithSetOfRefs");
		Set<?> set = bean.getSomeSet();
		assertThat(set).isNotNull();
		assertThat(set.size()).isEqualTo(2);
		Iterator it = set.iterator();
		it.next();
		Object o = it.next();
		assertThat(o).isNotNull();
		boolean condition = o instanceof TestBean;
		assertThat(condition).isTrue();
		assertThat(((TestBean) o).getName()).isEqualTo("Sally");
	}

	@Test
	public void mergeMap() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithMap");
		Map map = bean.getSomeMap();
		assertThat(map.size()).as("Incorrect size").isEqualTo(3);
		assertThat(map.get("Rob")).isEqualTo("Sally");
		assertThat(map.get("Rod")).isEqualTo("Kerry");
		assertThat(map.get("Juergen")).isEqualTo("Eva");
	}

	@Test
	public void mergeMapWithInnerBeanAsMapEntryValue() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithMapOfRefs");
		Map<?, ?> map = bean.getSomeMap();
		assertThat(map).isNotNull();
		assertThat(map.size()).isEqualTo(2);
		assertThat(map.get("Rob")).isNotNull();
		boolean condition = map.get("Rob") instanceof TestBean;
		assertThat(condition).isTrue();
		assertThat(((TestBean) map.get("Rob")).getName()).isEqualTo("Sally");
	}

	@Test
	public void mergeProperties() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithProps");
		Properties props = bean.getSomeProperties();
		assertThat(props.size()).as("Incorrect size").isEqualTo(3);
		assertThat(props.getProperty("Rob")).isEqualTo("Sally");
		assertThat(props.getProperty("Rod")).isEqualTo("Kerry");
		assertThat(props.getProperty("Juergen")).isEqualTo("Eva");
	}

	@Test
	public void mergeListInConstructor() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithListInConstructor");
		List list = bean.getSomeList();
		assertThat(list.size()).as("Incorrect size").isEqualTo(3);
		assertThat(list.get(0)).isEqualTo("Rob Harrop");
		assertThat(list.get(1)).isEqualTo("Rod Johnson");
		assertThat(list.get(2)).isEqualTo("Juergen Hoeller");
	}

	@Test
	public void mergeListWithInnerBeanAsListElementInConstructor() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithListOfRefsInConstructor");
		List<?> list = bean.getSomeList();
		assertThat(list).isNotNull();
		assertThat(list.size()).isEqualTo(3);
		assertThat(list.get(2)).isNotNull();
		boolean condition = list.get(2) instanceof TestBean;
		assertThat(condition).isTrue();
	}

	@Test
	public void mergeSetInConstructor() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithSetInConstructor");
		Set set = bean.getSomeSet();
		assertThat(set.size()).as("Incorrect size").isEqualTo(2);
		assertThat(set.contains("Rob Harrop")).isTrue();
		assertThat(set.contains("Sally Greenwood")).isTrue();
	}

	@Test
	public void mergeSetWithInnerBeanAsSetElementInConstructor() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithSetOfRefsInConstructor");
		Set<?> set = bean.getSomeSet();
		assertThat(set).isNotNull();
		assertThat(set.size()).isEqualTo(2);
		Iterator it = set.iterator();
		it.next();
		Object o = it.next();
		assertThat(o).isNotNull();
		boolean condition = o instanceof TestBean;
		assertThat(condition).isTrue();
		assertThat(((TestBean) o).getName()).isEqualTo("Sally");
	}

	@Test
	public void mergeMapInConstructor() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithMapInConstructor");
		Map map = bean.getSomeMap();
		assertThat(map.size()).as("Incorrect size").isEqualTo(3);
		assertThat(map.get("Rob")).isEqualTo("Sally");
		assertThat(map.get("Rod")).isEqualTo("Kerry");
		assertThat(map.get("Juergen")).isEqualTo("Eva");
	}

	@Test
	public void mergeMapWithInnerBeanAsMapEntryValueInConstructor() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithMapOfRefsInConstructor");
		Map<?, ?> map = bean.getSomeMap();
		assertThat(map).isNotNull();
		assertThat(map.size()).isEqualTo(2);
		assertThat(map.get("Rob")).isNotNull();
		boolean condition = map.get("Rob") instanceof TestBean;
		assertThat(condition).isTrue();
		assertThat(((TestBean) map.get("Rob")).getName()).isEqualTo("Sally");
	}

	@Test
	public void mergePropertiesInConstructor() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithPropsInConstructor");
		Properties props = bean.getSomeProperties();
		assertThat(props.size()).as("Incorrect size").isEqualTo(3);
		assertThat(props.getProperty("Rob")).isEqualTo("Sally");
		assertThat(props.getProperty("Rod")).isEqualTo("Kerry");
		assertThat(props.getProperty("Juergen")).isEqualTo("Eva");
	}

}
