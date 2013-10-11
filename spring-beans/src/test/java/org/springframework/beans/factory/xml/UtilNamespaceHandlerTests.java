/*
 * Copyright 2002-2013 the original author or authors.
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

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.springframework.beans.factory.config.FieldRetrievingFactoryBean;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.parsing.ComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.tests.beans.CollectingReaderEventListener;
import org.springframework.tests.sample.beans.CustomEnum;
import org.springframework.tests.sample.beans.TestBean;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Biju Kunjummen
 */

public class UtilNamespaceHandlerTests {

	private DefaultListableBeanFactory beanFactory;

	private CollectingReaderEventListener listener = new CollectingReaderEventListener();

	@Before
	public void setUp() {
		this.beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.setEventListener(this.listener);
		reader.loadBeanDefinitions(new ClassPathResource("testUtilNamespace.xml", getClass()));
	}

	@Test
	public void testConstant() throws Exception {
		Integer min = (Integer) this.beanFactory.getBean("min");
		assertEquals(Integer.MIN_VALUE, min.intValue());
	}

	@Test
	public void testConstantWithDefaultName() throws Exception {
		Integer max = (Integer) this.beanFactory.getBean("java.lang.Integer.MAX_VALUE");
		assertEquals(Integer.MAX_VALUE, max.intValue());
	}

	@Test
	public void testEvents() throws Exception {
		ComponentDefinition propertiesComponent = this.listener.getComponentDefinition("myProperties");
		assertNotNull("Event for 'myProperties' not sent", propertiesComponent);
		AbstractBeanDefinition propertiesBean = (AbstractBeanDefinition) propertiesComponent.getBeanDefinitions()[0];
		assertEquals("Incorrect BeanDefinition", PropertiesFactoryBean.class, propertiesBean.getBeanClass());

		ComponentDefinition constantComponent = this.listener.getComponentDefinition("min");
		assertNotNull("Event for 'min' not sent", propertiesComponent);
		AbstractBeanDefinition constantBean = (AbstractBeanDefinition) constantComponent.getBeanDefinitions()[0];
		assertEquals("Incorrect BeanDefinition", FieldRetrievingFactoryBean.class, constantBean.getBeanClass());
	}

	@Test
	public void testNestedProperties() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		Properties props = bean.getSomeProperties();
		assertEquals("Incorrect property value", "bar", props.get("foo"));
	}

	@Test
	public void testPropertyPath() throws Exception {
		String name = (String) this.beanFactory.getBean("name");
		assertEquals("Rob Harrop", name);
	}

	@Test
	public void testNestedPropertyPath() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		assertEquals("Rob Harrop", bean.getName());
	}

	@Test
	public void testSimpleMap() throws Exception {
		Map map = (Map) this.beanFactory.getBean("simpleMap");
		assertEquals("bar", map.get("foo"));
		Map map2 = (Map) this.beanFactory.getBean("simpleMap");
		assertTrue(map == map2);
	}

	@Test
	public void testScopedMap() throws Exception {
		Map map = (Map) this.beanFactory.getBean("scopedMap");
		assertEquals("bar", map.get("foo"));
		Map map2 = (Map) this.beanFactory.getBean("scopedMap");
		assertEquals("bar", map2.get("foo"));
		assertTrue(map != map2);
	}

	@Test
	public void testSimpleList() throws Exception {
		List list = (List) this.beanFactory.getBean("simpleList");
		assertEquals("Rob Harrop", list.get(0));
		List list2 = (List) this.beanFactory.getBean("simpleList");
		assertTrue(list == list2);
	}

	@Test
	public void testScopedList() throws Exception {
		List list = (List) this.beanFactory.getBean("scopedList");
		assertEquals("Rob Harrop", list.get(0));
		List list2 = (List) this.beanFactory.getBean("scopedList");
		assertEquals("Rob Harrop", list2.get(0));
		assertTrue(list != list2);
	}

	@Test
	public void testSimpleSet() throws Exception {
		Set set = (Set) this.beanFactory.getBean("simpleSet");
		assertTrue(set.contains("Rob Harrop"));
		Set set2 = (Set) this.beanFactory.getBean("simpleSet");
		assertTrue(set == set2);
	}

	@Test
	public void testScopedSet() throws Exception {
		Set set = (Set) this.beanFactory.getBean("scopedSet");
		assertTrue(set.contains("Rob Harrop"));
		Set set2 = (Set) this.beanFactory.getBean("scopedSet");
		assertTrue(set2.contains("Rob Harrop"));
		assertTrue(set != set2);
	}

	@Test
	public void testMapWithRef() throws Exception {
		Map map = (Map) this.beanFactory.getBean("mapWithRef");
		assertTrue(map instanceof TreeMap);
		assertEquals(this.beanFactory.getBean("testBean"), map.get("bean"));
	}

	@Test
	public void testNestedCollections() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("nestedCollectionsBean");

		List list = bean.getSomeList();
		assertEquals(1, list.size());
		assertEquals("foo", list.get(0));

		Set set = bean.getSomeSet();
		assertEquals(1, set.size());
		assertTrue(set.contains("bar"));

		Map map = bean.getSomeMap();
		assertEquals(1, map.size());
		assertTrue(map.get("foo") instanceof Set);
		Set innerSet = (Set) map.get("foo");
		assertEquals(1, innerSet.size());
		assertTrue(innerSet.contains("bar"));

		TestBean bean2 = (TestBean) this.beanFactory.getBean("nestedCollectionsBean");
		assertEquals(list, bean2.getSomeList());
		assertEquals(set, bean2.getSomeSet());
		assertEquals(map, bean2.getSomeMap());
		assertFalse(list == bean2.getSomeList());
		assertFalse(set == bean2.getSomeSet());
		assertFalse(map == bean2.getSomeMap());
	}

	@Test
	public void testNestedShortcutCollections() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("nestedShortcutCollections");

		assertEquals(1, bean.getStringArray().length);
		assertEquals("fooStr", bean.getStringArray()[0]);

		List list = bean.getSomeList();
		assertEquals(1, list.size());
		assertEquals("foo", list.get(0));

		Set set = bean.getSomeSet();
		assertEquals(1, set.size());
		assertTrue(set.contains("bar"));

		TestBean bean2 = (TestBean) this.beanFactory.getBean("nestedShortcutCollections");
		assertTrue(Arrays.equals(bean.getStringArray(), bean2.getStringArray()));
		assertFalse(bean.getStringArray() == bean2.getStringArray());
		assertEquals(list, bean2.getSomeList());
		assertEquals(set, bean2.getSomeSet());
		assertFalse(list == bean2.getSomeList());
		assertFalse(set == bean2.getSomeSet());
	}

	@Test
	public void testNestedInCollections() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("nestedCustomTagBean");

		List list = bean.getSomeList();
		assertEquals(1, list.size());
		assertEquals(Integer.MIN_VALUE, list.get(0));

		Set set = bean.getSomeSet();
		assertEquals(2, set.size());
		assertTrue(set.contains(Thread.State.NEW));
		assertTrue(set.contains(Thread.State.RUNNABLE));

		Map map = bean.getSomeMap();
		assertEquals(1, map.size());
		assertEquals(CustomEnum.VALUE_1, map.get("min"));

		TestBean bean2 = (TestBean) this.beanFactory.getBean("nestedCustomTagBean");
		assertEquals(list, bean2.getSomeList());
		assertEquals(set, bean2.getSomeSet());
		assertEquals(map, bean2.getSomeMap());
		assertFalse(list == bean2.getSomeList());
		assertFalse(set == bean2.getSomeSet());
		assertFalse(map == bean2.getSomeMap());
	}

	@Test
	public void testCircularCollections() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("circularCollectionsBean");

		List list = bean.getSomeList();
		assertEquals(1, list.size());
		assertEquals(bean, list.get(0));

		Set set = bean.getSomeSet();
		assertEquals(1, set.size());
		assertTrue(set.contains(bean));

		Map map = bean.getSomeMap();
		assertEquals(1, map.size());
		assertEquals(bean, map.get("foo"));
	}

	@Test
	public void testCircularCollectionBeansStartingWithList() throws Exception {
		this.beanFactory.getBean("circularList");
		TestBean bean = (TestBean) this.beanFactory.getBean("circularCollectionBeansBean");

		List list = bean.getSomeList();
		assertTrue(Proxy.isProxyClass(list.getClass()));
		assertEquals(1, list.size());
		assertEquals(bean, list.get(0));

		Set set = bean.getSomeSet();
		assertFalse(Proxy.isProxyClass(set.getClass()));
		assertEquals(1, set.size());
		assertTrue(set.contains(bean));

		Map map = bean.getSomeMap();
		assertFalse(Proxy.isProxyClass(map.getClass()));
		assertEquals(1, map.size());
		assertEquals(bean, map.get("foo"));
	}

	@Test
	public void testCircularCollectionBeansStartingWithSet() throws Exception {
		this.beanFactory.getBean("circularSet");
		TestBean bean = (TestBean) this.beanFactory.getBean("circularCollectionBeansBean");

		List list = bean.getSomeList();
		assertFalse(Proxy.isProxyClass(list.getClass()));
		assertEquals(1, list.size());
		assertEquals(bean, list.get(0));

		Set set = bean.getSomeSet();
		assertTrue(Proxy.isProxyClass(set.getClass()));
		assertEquals(1, set.size());
		assertTrue(set.contains(bean));

		Map map = bean.getSomeMap();
		assertFalse(Proxy.isProxyClass(map.getClass()));
		assertEquals(1, map.size());
		assertEquals(bean, map.get("foo"));
	}

	@Test
	public void testCircularCollectionBeansStartingWithMap() throws Exception {
		this.beanFactory.getBean("circularMap");
		TestBean bean = (TestBean) this.beanFactory.getBean("circularCollectionBeansBean");

		List list = bean.getSomeList();
		assertFalse(Proxy.isProxyClass(list.getClass()));
		assertEquals(1, list.size());
		assertEquals(bean, list.get(0));

		Set set = bean.getSomeSet();
		assertFalse(Proxy.isProxyClass(set.getClass()));
		assertEquals(1, set.size());
		assertTrue(set.contains(bean));

		Map map = bean.getSomeMap();
		assertTrue(Proxy.isProxyClass(map.getClass()));
		assertEquals(1, map.size());
		assertEquals(bean, map.get("foo"));
	}

	@Test
	public void testNestedInConstructor() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("constructedTestBean");
		assertEquals("Rob Harrop", bean.getName());
	}

	@Test
	public void testLoadProperties() throws Exception {
		Properties props = (Properties) this.beanFactory.getBean("myProperties");
		assertEquals("Incorrect property value", "bar", props.get("foo"));
		assertEquals("Incorrect property value", null, props.get("foo2"));
		Properties props2 = (Properties) this.beanFactory.getBean("myProperties");
		assertTrue(props == props2);
	}
	
	@Test
	public void testLoadPropertiesWithMultipleResources() throws Exception {
		Properties props = (Properties) this.beanFactory.getBean("multiLocationProperties");
		assertEquals("Incorrect property value", "bar", props.get("foo"));
		assertEquals("Incorrect property value", "bar2", props.get("foo2"));
		assertEquals("Incorrect property value", null, props.get("foo3"));
	}	

	@Test
	public void testScopedProperties() throws Exception {
		Properties props = (Properties) this.beanFactory.getBean("myScopedProperties");
		assertEquals("Incorrect property value", "bar", props.get("foo"));
		assertEquals("Incorrect property value", null, props.get("foo2"));
		Properties props2 = (Properties) this.beanFactory.getBean("myScopedProperties");
		assertEquals("Incorrect property value", "bar", props.get("foo"));
		assertEquals("Incorrect property value", null, props.get("foo2"));
		assertTrue(props != props2);
	}

	@Test
	public void testLocalProperties() throws Exception {
		Properties props = (Properties) this.beanFactory.getBean("myLocalProperties");
		assertEquals("Incorrect property value", null, props.get("foo"));
		assertEquals("Incorrect property value", "bar2", props.get("foo2"));
	}

	@Test
	public void testMergedProperties() throws Exception {
		Properties props = (Properties) this.beanFactory.getBean("myMergedProperties");
		assertEquals("Incorrect property value", "bar", props.get("foo"));
		assertEquals("Incorrect property value", "bar2", props.get("foo2"));
	}

	@Test
	public void testLocalOverrideDefault() {
		Properties props = (Properties) this.beanFactory.getBean("defaultLocalOverrideProperties");
		assertEquals("Incorrect property value", "bar", props.get("foo"));
		assertEquals("Incorrect property value", "local2", props.get("foo2"));
	}

	@Test
	public void testLocalOverrideFalse() {
		Properties props = (Properties) this.beanFactory.getBean("falseLocalOverrideProperties");
		assertEquals("Incorrect property value", "bar", props.get("foo"));
		assertEquals("Incorrect property value", "local2", props.get("foo2"));
	}

	@Test
	public void testLocalOverrideTrue() {
		Properties props = (Properties) this.beanFactory.getBean("trueLocalOverrideProperties");
		assertEquals("Incorrect property value", "local", props.get("foo"));
		assertEquals("Incorrect property value", "local2", props.get("foo2"));
	}

}
