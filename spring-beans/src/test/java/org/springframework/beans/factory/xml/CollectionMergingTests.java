/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * Unit and integration tests for the collection merging support.
 *
 * @author Rob Harrop
 * @author Rick Evans
 */
@SuppressWarnings("rawtypes")
public class CollectionMergingTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();


	@Before
	public void setUp() throws Exception {
		BeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.loadBeanDefinitions(new ClassPathResource("collectionMerging.xml", getClass()));
	}

	@Test
	public void mergeList() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithList");
		List list = bean.getSomeList();
		assertEquals("Incorrect size", 3, list.size());
		assertEquals("Rob Harrop", list.get(0));
		assertEquals("Rod Johnson", list.get(1));
		assertEquals("Juergen Hoeller", list.get(2));
	}

	@Test
	public void mergeListWithInnerBeanAsListElement() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithListOfRefs");
		List list = bean.getSomeList();
		assertNotNull(list);
		assertEquals(3, list.size());
		assertNotNull(list.get(2));
		assertTrue(list.get(2) instanceof TestBean);
	}

	@Test
	public void mergeSet() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithSet");
		Set set = bean.getSomeSet();
		assertEquals("Incorrect size", 2, set.size());
		assertTrue(set.contains("Rob Harrop"));
		assertTrue(set.contains("Sally Greenwood"));
	}

	@Test
	public void mergeSetWithInnerBeanAsSetElement() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithSetOfRefs");
		Set set = bean.getSomeSet();
		assertNotNull(set);
		assertEquals(2, set.size());
		Iterator it = set.iterator();
		it.next();
		Object o = it.next();
		assertNotNull(o);
		assertTrue(o instanceof TestBean);
		assertEquals("Sally", ((TestBean) o).getName());
	}

	@Test
	public void mergeMap() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithMap");
		Map map = bean.getSomeMap();
		assertEquals("Incorrect size", 3, map.size());
		assertEquals("Sally", map.get("Rob"));
		assertEquals("Kerry", map.get("Rod"));
		assertEquals("Eva", map.get("Juergen"));
	}

	@Test
	public void mergeMapWithInnerBeanAsMapEntryValue() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithMapOfRefs");
		Map map = bean.getSomeMap();
		assertNotNull(map);
		assertEquals(2, map.size());
		assertNotNull(map.get("Rob"));
		assertTrue(map.get("Rob") instanceof TestBean);
		assertEquals("Sally", ((TestBean) map.get("Rob")).getName());
	}

	@Test
	public void mergeProperties() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithProps");
		Properties props = bean.getSomeProperties();
		assertEquals("Incorrect size", 3, props.size());
		assertEquals("Sally", props.getProperty("Rob"));
		assertEquals("Kerry",props.getProperty("Rod"));
		assertEquals("Eva", props.getProperty("Juergen"));
	}

	@Test
	public void mergeListInConstructor() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithListInConstructor");
		List list = bean.getSomeList();
		assertEquals("Incorrect size", 3, list.size());
		assertEquals("Rob Harrop", list.get(0));
		assertEquals("Rod Johnson", list.get(1));
		assertEquals("Juergen Hoeller", list.get(2));
	}

	@Test
	public void mergeListWithInnerBeanAsListElementInConstructor() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithListOfRefsInConstructor");
		List list = bean.getSomeList();
		assertNotNull(list);
		assertEquals(3, list.size());
		assertNotNull(list.get(2));
		assertTrue(list.get(2) instanceof TestBean);
	}

	@Test
	public void mergeSetInConstructor() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithSetInConstructor");
		Set set = bean.getSomeSet();
		assertEquals("Incorrect size", 2, set.size());
		assertTrue(set.contains("Rob Harrop"));
		assertTrue(set.contains("Sally Greenwood"));
	}

	@Test
	public void mergeSetWithInnerBeanAsSetElementInConstructor() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithSetOfRefsInConstructor");
		Set set = bean.getSomeSet();
		assertNotNull(set);
		assertEquals(2, set.size());
		Iterator it = set.iterator();
		it.next();
		Object o = it.next();
		assertNotNull(o);
		assertTrue(o instanceof TestBean);
		assertEquals("Sally", ((TestBean) o).getName());
	}

	@Test
	public void mergeMapInConstructor() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithMapInConstructor");
		Map map = bean.getSomeMap();
		assertEquals("Incorrect size", 3, map.size());
		assertEquals("Sally", map.get("Rob"));
		assertEquals("Kerry", map.get("Rod"));
		assertEquals("Eva", map.get("Juergen"));
	}

	@Test
	public void mergeMapWithInnerBeanAsMapEntryValueInConstructor() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithMapOfRefsInConstructor");
		Map map = bean.getSomeMap();
		assertNotNull(map);
		assertEquals(2, map.size());
		assertNotNull(map.get("Rob"));
		assertTrue(map.get("Rob") instanceof TestBean);
		assertEquals("Sally", ((TestBean) map.get("Rob")).getName());
	}

	@Test
	public void mergePropertiesInConstructor() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithPropsInConstructor");
		Properties props = bean.getSomeProperties();
		assertEquals("Incorrect size", 3, props.size());
		assertEquals("Sally", props.getProperty("Rob"));
		assertEquals("Kerry", props.getProperty("Rod"));
		assertEquals("Eva", props.getProperty("Juergen"));
	}

}
