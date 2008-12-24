/*
 * Copyright 2002-2007 the original author or authors.
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

import junit.framework.TestCase;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;

import test.beans.TestBean;

/**
 * Unit and integration tests for the collection merging support.
 *
 * @author Rob Harrop
 * @author Rick Evans
 */
public class CollectionMergingTests extends TestCase {

	private DefaultListableBeanFactory beanFactory;

	protected void setUp() throws Exception {
		this.beanFactory = new DefaultListableBeanFactory();
		BeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.loadBeanDefinitions(new ClassPathResource("collectionMerging.xml", getClass()));
	}

	public void testMergeList() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithList");
		List list = bean.getSomeList();
		assertEquals("Incorrect size", 3, list.size());
		assertEquals(list.get(0), "Rob Harrop");
		assertEquals(list.get(1), "Rod Johnson");
		assertEquals(list.get(2), "Juergen Hoeller");
	}

	public void testMergeListWithInnerBeanAsListElement() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithListOfRefs");
		List list = bean.getSomeList();
		assertNotNull(list);
		assertEquals(3, list.size());
		assertNotNull(list.get(2));
		assertTrue(list.get(2) instanceof TestBean);
	}

	public void testMergeSet() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithSet");
		Set set = bean.getSomeSet();
		assertEquals("Incorrect size", 2, set.size());
		assertTrue(set.contains("Rob Harrop"));
		assertTrue(set.contains("Sally Greenwood"));
	}

	public void testMergeSetWithInnerBeanAsSetElement() throws Exception {
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

	public void testMergeMap() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithMap");
		Map map = bean.getSomeMap();
		assertEquals("Incorrect size", 3, map.size());
		assertEquals(map.get("Rob"), "Sally");
		assertEquals(map.get("Rod"), "Kerry");
		assertEquals(map.get("Juergen"), "Eva");
	}

	public void testMergeMapWithInnerBeanAsMapEntryValue() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithMapOfRefs");
		Map map = bean.getSomeMap();
		assertNotNull(map);
		assertEquals(2, map.size());
		assertNotNull(map.get("Rob"));
		assertTrue(map.get("Rob") instanceof TestBean);
		assertEquals("Sally", ((TestBean) map.get("Rob")).getName());
	}

	public void testMergeProperties() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithProps");
		Properties props = bean.getSomeProperties();
		assertEquals("Incorrect size", 3, props.size());
		assertEquals(props.getProperty("Rob"), "Sally");
		assertEquals(props.getProperty("Rod"), "Kerry");
		assertEquals(props.getProperty("Juergen"), "Eva");
	}


	public void testMergeListInConstructor() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithListInConstructor");
		List list = bean.getSomeList();
		assertEquals("Incorrect size", 3, list.size());
		assertEquals(list.get(0), "Rob Harrop");
		assertEquals(list.get(1), "Rod Johnson");
		assertEquals(list.get(2), "Juergen Hoeller");
	}

	public void testMergeListWithInnerBeanAsListElementInConstructor() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithListOfRefsInConstructor");
		List list = bean.getSomeList();
		assertNotNull(list);
		assertEquals(3, list.size());
		assertNotNull(list.get(2));
		assertTrue(list.get(2) instanceof TestBean);
	}

	public void testMergeSetInConstructor() {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithSetInConstructor");
		Set set = bean.getSomeSet();
		assertEquals("Incorrect size", 2, set.size());
		assertTrue(set.contains("Rob Harrop"));
		assertTrue(set.contains("Sally Greenwood"));
	}

	public void testMergeSetWithInnerBeanAsSetElementInConstructor() throws Exception {
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

	public void testMergeMapInConstructor() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithMapInConstructor");
		Map map = bean.getSomeMap();
		assertEquals("Incorrect size", 3, map.size());
		assertEquals(map.get("Rob"), "Sally");
		assertEquals(map.get("Rod"), "Kerry");
		assertEquals(map.get("Juergen"), "Eva");
	}

	public void testMergeMapWithInnerBeanAsMapEntryValueInConstructor() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithMapOfRefsInConstructor");
		Map map = bean.getSomeMap();
		assertNotNull(map);
		assertEquals(2, map.size());
		assertNotNull(map.get("Rob"));
		assertTrue(map.get("Rob") instanceof TestBean);
		assertEquals("Sally", ((TestBean) map.get("Rob")).getName());
	}

	public void testMergePropertiesInConstructor() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("childWithPropsInConstructor");
		Properties props = bean.getSomeProperties();
		assertEquals("Incorrect size", 3, props.size());
		assertEquals(props.getProperty("Rob"), "Sally");
		assertEquals(props.getProperty("Rod"), "Kerry");
		assertEquals(props.getProperty("Juergen"), "Eva");
	}

}
