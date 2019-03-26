/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.core.OverridingClassLoader;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.UrlResource;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.tests.sample.beans.GenericBean;
import org.springframework.tests.sample.beans.GenericIntegerBean;
import org.springframework.tests.sample.beans.GenericSetOfIntegerBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 20.01.2006
 */
public class BeanFactoryGenericsTests {

	@Test
	public void testGenericSetProperty() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

		Set<String> input = new HashSet<>();
		input.add("4");
		input.add("5");
		rbd.getPropertyValues().add("integerSet", input);

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertTrue(gb.getIntegerSet().contains(new Integer(4)));
		assertTrue(gb.getIntegerSet().contains(new Integer(5)));
	}

	@Test
	public void testGenericListProperty() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

		List<String> input = new ArrayList<>();
		input.add("http://localhost:8080");
		input.add("http://localhost:9090");
		rbd.getPropertyValues().add("resourceList", input);

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertEquals(new UrlResource("http://localhost:8080"), gb.getResourceList().get(0));
		assertEquals(new UrlResource("http://localhost:9090"), gb.getResourceList().get(1));
	}

	@Test
	public void testGenericListPropertyWithAutowiring() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("resource1", new UrlResource("http://localhost:8080"));
		bf.registerSingleton("resource2", new UrlResource("http://localhost:9090"));

		RootBeanDefinition rbd = new RootBeanDefinition(GenericIntegerBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		bf.registerBeanDefinition("genericBean", rbd);
		GenericIntegerBean gb = (GenericIntegerBean) bf.getBean("genericBean");

		assertEquals(new UrlResource("http://localhost:8080"), gb.getResourceList().get(0));
		assertEquals(new UrlResource("http://localhost:9090"), gb.getResourceList().get(1));
	}

	@Test
	public void testGenericListPropertyWithInvalidElementType() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericIntegerBean.class);

		List<Integer> input = new ArrayList<>();
		input.add(1);
		rbd.getPropertyValues().add("testBeanList", input);

		bf.registerBeanDefinition("genericBean", rbd);
		try {
			bf.getBean("genericBean");
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.getMessage().contains("genericBean") && ex.getMessage().contains("testBeanList[0]"));
			assertTrue(ex.getMessage().contains(TestBean.class.getName()) && ex.getMessage().contains("Integer"));
		}
	}

	@Test
	public void testGenericListPropertyWithOptionalAutowiring() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertNull(gb.getResourceList());
	}

	@Test
	public void testGenericMapProperty() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

		Map<String, String> input = new HashMap<>();
		input.put("4", "5");
		input.put("6", "7");
		rbd.getPropertyValues().add("shortMap", input);

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertEquals(new Integer(5), gb.getShortMap().get(new Short("4")));
		assertEquals(new Integer(7), gb.getShortMap().get(new Short("6")));
	}

	@Test
	public void testGenericListOfArraysProperty() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("listOfArrays");

		assertEquals(1, gb.getListOfArrays().size());
		String[] array = gb.getListOfArrays().get(0);
		assertEquals(2, array.length);
		assertEquals("value1", array[0]);
		assertEquals("value2", array[1]);
	}


	@Test
	public void testGenericSetConstructor() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

		Set<String> input = new HashSet<>();
		input.add("4");
		input.add("5");
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input);

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertTrue(gb.getIntegerSet().contains(new Integer(4)));
		assertTrue(gb.getIntegerSet().contains(new Integer(5)));
	}

	@Test
	public void testGenericSetConstructorWithAutowiring() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("integer1", new Integer(4));
		bf.registerSingleton("integer2", new Integer(5));

		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertTrue(gb.getIntegerSet().contains(new Integer(4)));
		assertTrue(gb.getIntegerSet().contains(new Integer(5)));
	}

	@Test
	public void testGenericSetConstructorWithOptionalAutowiring() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertNull(gb.getIntegerSet());
	}

	@Test
	public void testGenericSetListConstructor() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

		Set<String> input = new HashSet<>();
		input.add("4");
		input.add("5");
		List<String> input2 = new ArrayList<>();
		input2.add("http://localhost:8080");
		input2.add("http://localhost:9090");
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input2);

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertTrue(gb.getIntegerSet().contains(new Integer(4)));
		assertTrue(gb.getIntegerSet().contains(new Integer(5)));
		assertEquals(new UrlResource("http://localhost:8080"), gb.getResourceList().get(0));
		assertEquals(new UrlResource("http://localhost:9090"), gb.getResourceList().get(1));
	}

	@Test
	public void testGenericSetListConstructorWithAutowiring() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("integer1", new Integer(4));
		bf.registerSingleton("integer2", new Integer(5));
		bf.registerSingleton("resource1", new UrlResource("http://localhost:8080"));
		bf.registerSingleton("resource2", new UrlResource("http://localhost:9090"));

		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertTrue(gb.getIntegerSet().contains(new Integer(4)));
		assertTrue(gb.getIntegerSet().contains(new Integer(5)));
		assertEquals(new UrlResource("http://localhost:8080"), gb.getResourceList().get(0));
		assertEquals(new UrlResource("http://localhost:9090"), gb.getResourceList().get(1));
	}

	@Test
	public void testGenericSetListConstructorWithOptionalAutowiring() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("resource1", new UrlResource("http://localhost:8080"));
		bf.registerSingleton("resource2", new UrlResource("http://localhost:9090"));

		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertNull(gb.getIntegerSet());
		assertNull(gb.getResourceList());
	}

	@Test
	public void testGenericSetMapConstructor() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

		Set<String> input = new HashSet<>();
		input.add("4");
		input.add("5");
		Map<String, String> input2 = new HashMap<>();
		input2.put("4", "5");
		input2.put("6", "7");
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input2);

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertTrue(gb.getIntegerSet().contains(new Integer(4)));
		assertTrue(gb.getIntegerSet().contains(new Integer(5)));
		assertEquals(new Integer(5), gb.getShortMap().get(new Short("4")));
		assertEquals(new Integer(7), gb.getShortMap().get(new Short("6")));
	}

	@Test
	public void testGenericMapResourceConstructor() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

		Map<String, String> input = new HashMap<>();
		input.put("4", "5");
		input.put("6", "7");
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
		rbd.getConstructorArgumentValues().addGenericArgumentValue("http://localhost:8080");

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertEquals(new Integer(5), gb.getShortMap().get(new Short("4")));
		assertEquals(new Integer(7), gb.getShortMap().get(new Short("6")));
		assertEquals(new UrlResource("http://localhost:8080"), gb.getResourceList().get(0));
	}

	@Test
	public void testGenericMapMapConstructor() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

		Map<String, String> input = new HashMap<>();
		input.put("1", "0");
		input.put("2", "3");
		Map<String, String> input2 = new HashMap<>();
		input2.put("4", "5");
		input2.put("6", "7");
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input2);

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertNotSame(gb.getPlainMap(), gb.getShortMap());
		assertEquals(2, gb.getPlainMap().size());
		assertEquals("0", gb.getPlainMap().get("1"));
		assertEquals("3", gb.getPlainMap().get("2"));
		assertEquals(2, gb.getShortMap().size());
		assertEquals(new Integer(5), gb.getShortMap().get(new Short("4")));
		assertEquals(new Integer(7), gb.getShortMap().get(new Short("6")));
	}

	@Test
	public void testGenericMapMapConstructorWithSameRefAndConversion() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

		Map<String, String> input = new HashMap<>();
		input.put("1", "0");
		input.put("2", "3");
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input);

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertNotSame(gb.getPlainMap(), gb.getShortMap());
		assertEquals(2, gb.getPlainMap().size());
		assertEquals("0", gb.getPlainMap().get("1"));
		assertEquals("3", gb.getPlainMap().get("2"));
		assertEquals(2, gb.getShortMap().size());
		assertEquals(new Integer(0), gb.getShortMap().get(new Short("1")));
		assertEquals(new Integer(3), gb.getShortMap().get(new Short("2")));
	}

	@Test
	public void testGenericMapMapConstructorWithSameRefAndNoConversion() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

		Map<Short, Integer> input = new HashMap<>();
		input.put(new Short((short) 1), new Integer(0));
		input.put(new Short((short) 2), new Integer(3));
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input);

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertSame(gb.getPlainMap(), gb.getShortMap());
		assertEquals(2, gb.getShortMap().size());
		assertEquals(new Integer(0), gb.getShortMap().get(new Short("1")));
		assertEquals(new Integer(3), gb.getShortMap().get(new Short("2")));
	}

	@Test
	public void testGenericMapWithKeyTypeConstructor() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

		Map<String, String> input = new HashMap<>();
		input.put("4", "5");
		input.put("6", "7");
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input);

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertEquals("5", gb.getLongMap().get(new Long("4")));
		assertEquals("7", gb.getLongMap().get(new Long("6")));
	}

	@Test
	public void testGenericMapWithCollectionValueConstructor() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.addPropertyEditorRegistrar(new PropertyEditorRegistrar() {
			@Override
			public void registerCustomEditors(PropertyEditorRegistry registry) {
				registry.registerCustomEditor(Number.class, new CustomNumberEditor(Integer.class, false));
			}
		});
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

		Map<String, AbstractCollection<?>> input = new HashMap<>();
		HashSet<Integer> value1 = new HashSet<>();
		value1.add(new Integer(1));
		input.put("1", value1);
		ArrayList<Boolean> value2 = new ArrayList<>();
		value2.add(Boolean.TRUE);
		input.put("2", value2);
		rbd.getConstructorArgumentValues().addGenericArgumentValue(Boolean.TRUE);
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input);

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertTrue(gb.getCollectionMap().get(new Integer(1)) instanceof HashSet);
		assertTrue(gb.getCollectionMap().get(new Integer(2)) instanceof ArrayList);
	}


	@Test
	public void testGenericSetFactoryMethod() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
		rbd.setFactoryMethodName("createInstance");

		Set<String> input = new HashSet<>();
		input.add("4");
		input.add("5");
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input);

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertTrue(gb.getIntegerSet().contains(new Integer(4)));
		assertTrue(gb.getIntegerSet().contains(new Integer(5)));
	}

	@Test
	public void testGenericSetListFactoryMethod() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
		rbd.setFactoryMethodName("createInstance");

		Set<String> input = new HashSet<>();
		input.add("4");
		input.add("5");
		List<String> input2 = new ArrayList<>();
		input2.add("http://localhost:8080");
		input2.add("http://localhost:9090");
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input2);

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertTrue(gb.getIntegerSet().contains(new Integer(4)));
		assertTrue(gb.getIntegerSet().contains(new Integer(5)));
		assertEquals(new UrlResource("http://localhost:8080"), gb.getResourceList().get(0));
		assertEquals(new UrlResource("http://localhost:9090"), gb.getResourceList().get(1));
	}

	@Test
	public void testGenericSetMapFactoryMethod() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
		rbd.setFactoryMethodName("createInstance");

		Set<String> input = new HashSet<>();
		input.add("4");
		input.add("5");
		Map<String, String> input2 = new HashMap<>();
		input2.put("4", "5");
		input2.put("6", "7");
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input2);

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertTrue(gb.getIntegerSet().contains(new Integer(4)));
		assertTrue(gb.getIntegerSet().contains(new Integer(5)));
		assertEquals(new Integer(5), gb.getShortMap().get(new Short("4")));
		assertEquals(new Integer(7), gb.getShortMap().get(new Short("6")));
	}

	@Test
	public void testGenericMapResourceFactoryMethod() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
		rbd.setFactoryMethodName("createInstance");

		Map<String, String> input = new HashMap<>();
		input.put("4", "5");
		input.put("6", "7");
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
		rbd.getConstructorArgumentValues().addGenericArgumentValue("http://localhost:8080");

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertEquals(new Integer(5), gb.getShortMap().get(new Short("4")));
		assertEquals(new Integer(7), gb.getShortMap().get(new Short("6")));
		assertEquals(new UrlResource("http://localhost:8080"), gb.getResourceList().get(0));
	}

	@Test
	public void testGenericMapMapFactoryMethod() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
		rbd.setFactoryMethodName("createInstance");

		Map<String, String> input = new HashMap<>();
		input.put("1", "0");
		input.put("2", "3");
		Map<String, String> input2 = new HashMap<>();
		input2.put("4", "5");
		input2.put("6", "7");
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input2);

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertEquals("0", gb.getPlainMap().get("1"));
		assertEquals("3", gb.getPlainMap().get("2"));
		assertEquals(new Integer(5), gb.getShortMap().get(new Short("4")));
		assertEquals(new Integer(7), gb.getShortMap().get(new Short("6")));
	}

	@Test
	public void testGenericMapWithKeyTypeFactoryMethod() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
		rbd.setFactoryMethodName("createInstance");

		Map<String, String> input = new HashMap<>();
		input.put("4", "5");
		input.put("6", "7");
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input);

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertEquals("5", gb.getLongMap().get(new Long("4")));
		assertEquals("7", gb.getLongMap().get(new Long("6")));
	}

	@Test
	public void testGenericMapWithCollectionValueFactoryMethod() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.addPropertyEditorRegistrar(new PropertyEditorRegistrar() {
			@Override
			public void registerCustomEditors(PropertyEditorRegistry registry) {
				registry.registerCustomEditor(Number.class, new CustomNumberEditor(Integer.class, false));
			}
		});
		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
		rbd.setFactoryMethodName("createInstance");

		Map<String, AbstractCollection<?>> input = new HashMap<>();
		HashSet<Integer> value1 = new HashSet<>();
		value1.add(new Integer(1));
		input.put("1", value1);
		ArrayList<Boolean> value2 = new ArrayList<>();
		value2.add(Boolean.TRUE);
		input.put("2", value2);
		rbd.getConstructorArgumentValues().addGenericArgumentValue(Boolean.TRUE);
		rbd.getConstructorArgumentValues().addGenericArgumentValue(input);

		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertTrue(gb.getCollectionMap().get(new Integer(1)) instanceof HashSet);
		assertTrue(gb.getCollectionMap().get(new Integer(2)) instanceof ArrayList);
	}

	@Test
	public void testGenericListBean() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));
		List<?> list = (List<?>) bf.getBean("list");
		assertEquals(1, list.size());
		assertEquals(new URL("http://localhost:8080"), list.get(0));
	}

	@Test
	public void testGenericSetBean() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));
		Set<?> set = (Set<?>) bf.getBean("set");
		assertEquals(1, set.size());
		assertEquals(new URL("http://localhost:8080"), set.iterator().next());
	}

	@Test
	public void testGenericMapBean() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));
		Map<?, ?> map = (Map<?, ?>) bf.getBean("map");
		assertEquals(1, map.size());
		assertEquals(new Integer(10), map.keySet().iterator().next());
		assertEquals(new URL("http://localhost:8080"), map.values().iterator().next());
	}

	@Test
	public void testGenericallyTypedIntegerBean() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));
		GenericIntegerBean gb = (GenericIntegerBean) bf.getBean("integerBean");
		assertEquals(new Integer(10), gb.getGenericProperty());
		assertEquals(new Integer(20), gb.getGenericListProperty().get(0));
		assertEquals(new Integer(30), gb.getGenericListProperty().get(1));
	}

	@Test
	public void testGenericallyTypedSetOfIntegerBean() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));
		GenericSetOfIntegerBean gb = (GenericSetOfIntegerBean) bf.getBean("setOfIntegerBean");
		assertEquals(new Integer(10), gb.getGenericProperty().iterator().next());
		assertEquals(new Integer(20), gb.getGenericListProperty().get(0).iterator().next());
		assertEquals(new Integer(30), gb.getGenericListProperty().get(1).iterator().next());
	}

	@Test
	public void testSetBean() throws Exception {
		Assume.group(TestGroup.LONG_RUNNING);

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));
		UrlSet us = (UrlSet) bf.getBean("setBean");
		assertEquals(1, us.size());
		assertEquals(new URL("https://www.springframework.org"), us.iterator().next());
	}

	/**
	 * Tests support for parameterized static {@code factory-method} declarations such as
	 * Mockito's {@code mock()} method which has the following signature.
	 * <pre>
	 * {@code
	 * public static <T> T mock(Class<T> classToMock)
	 * }
	 * </pre>
	 * <p>See SPR-9493
	 */
	@Test
	public void parameterizedStaticFactoryMethod() {
		RootBeanDefinition rbd = new RootBeanDefinition(Mockito.class);
		rbd.setFactoryMethodName("mock");
		rbd.getConstructorArgumentValues().addGenericArgumentValue(Runnable.class);

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("mock", rbd);

		assertEquals(Runnable.class, bf.getType("mock"));
		assertEquals(Runnable.class, bf.getType("mock"));
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertEquals(1, beans.size());
	}

	/**
	 * Tests support for parameterized instance {@code factory-method} declarations such
	 * as EasyMock's {@code IMocksControl.createMock()} method which has the following
	 * signature.
	 * <pre>
	 * {@code
	 * public <T> T createMock(Class<T> toMock)
	 * }
	 * </pre>
	 * <p>See SPR-10411
	 */
	@Test
	public void parameterizedInstanceFactoryMethod() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition rbd = new RootBeanDefinition(MocksControl.class);
		bf.registerBeanDefinition("mocksControl", rbd);

		rbd = new RootBeanDefinition();
		rbd.setFactoryBeanName("mocksControl");
		rbd.setFactoryMethodName("createMock");
		rbd.getConstructorArgumentValues().addGenericArgumentValue(Runnable.class);
		bf.registerBeanDefinition("mock", rbd);

		assertTrue(bf.isTypeMatch("mock", Runnable.class));
		assertTrue(bf.isTypeMatch("mock", Runnable.class));
		assertEquals(Runnable.class, bf.getType("mock"));
		assertEquals(Runnable.class, bf.getType("mock"));
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertEquals(1, beans.size());
	}

	@Test
	public void parameterizedInstanceFactoryMethodWithNonResolvedClassName() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition rbd = new RootBeanDefinition(MocksControl.class);
		bf.registerBeanDefinition("mocksControl", rbd);

		rbd = new RootBeanDefinition();
		rbd.setFactoryBeanName("mocksControl");
		rbd.setFactoryMethodName("createMock");
		rbd.getConstructorArgumentValues().addGenericArgumentValue(Runnable.class.getName());
		bf.registerBeanDefinition("mock", rbd);

		assertTrue(bf.isTypeMatch("mock", Runnable.class));
		assertTrue(bf.isTypeMatch("mock", Runnable.class));
		assertEquals(Runnable.class, bf.getType("mock"));
		assertEquals(Runnable.class, bf.getType("mock"));
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertEquals(1, beans.size());
	}

	@Test
	public void parameterizedInstanceFactoryMethodWithWrappedClassName() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition rbd = new RootBeanDefinition();
		rbd.setBeanClassName(Mockito.class.getName());
		rbd.setFactoryMethodName("mock");
		// TypedStringValue used to be equivalent to an XML-defined argument String
		rbd.getConstructorArgumentValues().addGenericArgumentValue(new TypedStringValue(Runnable.class.getName()));
		bf.registerBeanDefinition("mock", rbd);

		assertTrue(bf.isTypeMatch("mock", Runnable.class));
		assertTrue(bf.isTypeMatch("mock", Runnable.class));
		assertEquals(Runnable.class, bf.getType("mock"));
		assertEquals(Runnable.class, bf.getType("mock"));
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertEquals(1, beans.size());
	}

	@Test
	public void parameterizedInstanceFactoryMethodWithInvalidClassName() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition rbd = new RootBeanDefinition(MocksControl.class);
		bf.registerBeanDefinition("mocksControl", rbd);

		rbd = new RootBeanDefinition();
		rbd.setFactoryBeanName("mocksControl");
		rbd.setFactoryMethodName("createMock");
		rbd.getConstructorArgumentValues().addGenericArgumentValue("x");
		bf.registerBeanDefinition("mock", rbd);

		assertFalse(bf.isTypeMatch("mock", Runnable.class));
		assertFalse(bf.isTypeMatch("mock", Runnable.class));
		assertNull(bf.getType("mock"));
		assertNull(bf.getType("mock"));
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertEquals(0, beans.size());
	}

	@Test
	public void parameterizedInstanceFactoryMethodWithIndexedArgument() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition rbd = new RootBeanDefinition(MocksControl.class);
		bf.registerBeanDefinition("mocksControl", rbd);

		rbd = new RootBeanDefinition();
		rbd.setFactoryBeanName("mocksControl");
		rbd.setFactoryMethodName("createMock");
		rbd.getConstructorArgumentValues().addIndexedArgumentValue(0, Runnable.class);
		bf.registerBeanDefinition("mock", rbd);

		assertTrue(bf.isTypeMatch("mock", Runnable.class));
		assertTrue(bf.isTypeMatch("mock", Runnable.class));
		assertEquals(Runnable.class, bf.getType("mock"));
		assertEquals(Runnable.class, bf.getType("mock"));
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertEquals(1, beans.size());
	}

	@Test  // SPR-16720
	public void parameterizedInstanceFactoryMethodWithTempClassLoader() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setTempClassLoader(new OverridingClassLoader(getClass().getClassLoader()));

		RootBeanDefinition rbd = new RootBeanDefinition(MocksControl.class);
		bf.registerBeanDefinition("mocksControl", rbd);

		rbd = new RootBeanDefinition();
		rbd.setFactoryBeanName("mocksControl");
		rbd.setFactoryMethodName("createMock");
		rbd.getConstructorArgumentValues().addGenericArgumentValue(Runnable.class);
		bf.registerBeanDefinition("mock", rbd);

		assertTrue(bf.isTypeMatch("mock", Runnable.class));
		assertTrue(bf.isTypeMatch("mock", Runnable.class));
		assertEquals(Runnable.class, bf.getType("mock"));
		assertEquals(Runnable.class, bf.getType("mock"));
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertEquals(1, beans.size());
	}

	@Test
	public void testGenericMatchingWithBeanNameDifferentiation() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setAutowireCandidateResolver(new GenericTypeAwareAutowireCandidateResolver());

		bf.registerBeanDefinition("doubleStore", new RootBeanDefinition(NumberStore.class));
		bf.registerBeanDefinition("floatStore", new RootBeanDefinition(NumberStore.class));
		bf.registerBeanDefinition("numberBean",
				new RootBeanDefinition(NumberBean.class, RootBeanDefinition.AUTOWIRE_CONSTRUCTOR, false));

		NumberBean nb = bf.getBean(NumberBean.class);
		assertSame(bf.getBean("doubleStore"), nb.getDoubleStore());
		assertSame(bf.getBean("floatStore"), nb.getFloatStore());

		String[] numberStoreNames = bf.getBeanNamesForType(ResolvableType.forClass(NumberStore.class));
		String[] doubleStoreNames = bf.getBeanNamesForType(ResolvableType.forClassWithGenerics(NumberStore.class, Double.class));
		String[] floatStoreNames = bf.getBeanNamesForType(ResolvableType.forClassWithGenerics(NumberStore.class, Float.class));
		assertEquals(2, numberStoreNames.length);
		assertEquals("doubleStore", numberStoreNames[0]);
		assertEquals("floatStore", numberStoreNames[1]);
		assertEquals(0, doubleStoreNames.length);
		assertEquals(0, floatStoreNames.length);
	}

	@Test
	public void testGenericMatchingWithFullTypeDifferentiation() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		bf.setAutowireCandidateResolver(new GenericTypeAwareAutowireCandidateResolver());

		RootBeanDefinition bd1 = new RootBeanDefinition(NumberStoreFactory.class);
		bd1.setFactoryMethodName("newDoubleStore");
		bf.registerBeanDefinition("store1", bd1);
		RootBeanDefinition bd2 = new RootBeanDefinition(NumberStoreFactory.class);
		bd2.setFactoryMethodName("newFloatStore");
		bf.registerBeanDefinition("store2", bd2);
		bf.registerBeanDefinition("numberBean",
				new RootBeanDefinition(NumberBean.class, RootBeanDefinition.AUTOWIRE_CONSTRUCTOR, false));

		NumberBean nb = bf.getBean(NumberBean.class);
		assertSame(bf.getBean("store1"), nb.getDoubleStore());
		assertSame(bf.getBean("store2"), nb.getFloatStore());

		String[] numberStoreNames = bf.getBeanNamesForType(ResolvableType.forClass(NumberStore.class));
		String[] doubleStoreNames = bf.getBeanNamesForType(ResolvableType.forClassWithGenerics(NumberStore.class, Double.class));
		String[] floatStoreNames = bf.getBeanNamesForType(ResolvableType.forClassWithGenerics(NumberStore.class, Float.class));
		assertEquals(2, numberStoreNames.length);
		assertEquals("store1", numberStoreNames[0]);
		assertEquals("store2", numberStoreNames[1]);
		assertEquals(1, doubleStoreNames.length);
		assertEquals("store1", doubleStoreNames[0]);
		assertEquals(1, floatStoreNames.length);
		assertEquals("store2", floatStoreNames[0]);

		ObjectProvider<NumberStore<?>> numberStoreProvider = bf.getBeanProvider(ResolvableType.forClass(NumberStore.class));
		ObjectProvider<NumberStore<Double>> doubleStoreProvider = bf.getBeanProvider(ResolvableType.forClassWithGenerics(NumberStore.class, Double.class));
		ObjectProvider<NumberStore<Float>> floatStoreProvider = bf.getBeanProvider(ResolvableType.forClassWithGenerics(NumberStore.class, Float.class));
		try {
			numberStoreProvider.getObject();
			fail("Should have thrown NoUniqueBeanDefinitionException");
		}
		catch (NoUniqueBeanDefinitionException ex) {
			// expected
		}
		try {
			numberStoreProvider.getIfAvailable();
			fail("Should have thrown NoUniqueBeanDefinitionException");
		}
		catch (NoUniqueBeanDefinitionException ex) {
			// expected
		}
		assertNull(numberStoreProvider.getIfUnique());
		assertSame(bf.getBean("store1"), doubleStoreProvider.getObject());
		assertSame(bf.getBean("store1"), doubleStoreProvider.getIfAvailable());
		assertSame(bf.getBean("store1"), doubleStoreProvider.getIfUnique());
		assertSame(bf.getBean("store2"), floatStoreProvider.getObject());
		assertSame(bf.getBean("store2"), floatStoreProvider.getIfAvailable());
		assertSame(bf.getBean("store2"), floatStoreProvider.getIfUnique());

		List<NumberStore<?>> resolved = new ArrayList<>();
		for (NumberStore<?> instance : numberStoreProvider) {
			resolved.add(instance);
		}
		assertEquals(2, resolved.size());
		assertSame(bf.getBean("store1"), resolved.get(0));
		assertSame(bf.getBean("store2"), resolved.get(1));

		resolved = numberStoreProvider.stream().collect(Collectors.toList());
		assertEquals(2, resolved.size());
		assertSame(bf.getBean("store1"), resolved.get(0));
		assertSame(bf.getBean("store2"), resolved.get(1));

		resolved = numberStoreProvider.orderedStream().collect(Collectors.toList());
		assertEquals(2, resolved.size());
		assertSame(bf.getBean("store2"), resolved.get(0));
		assertSame(bf.getBean("store1"), resolved.get(1));

		resolved = new ArrayList<>();
		for (NumberStore<Double> instance : doubleStoreProvider) {
			resolved.add(instance);
		}
		assertEquals(1, resolved.size());
		assertTrue(resolved.contains(bf.getBean("store1")));

		resolved = doubleStoreProvider.stream().collect(Collectors.toList());
		assertEquals(1, resolved.size());
		assertTrue(resolved.contains(bf.getBean("store1")));

		resolved = doubleStoreProvider.orderedStream().collect(Collectors.toList());
		assertEquals(1, resolved.size());
		assertTrue(resolved.contains(bf.getBean("store1")));

		resolved = new ArrayList<>();
		for (NumberStore<Float> instance : floatStoreProvider) {
			resolved.add(instance);
		}
		assertEquals(1, resolved.size());
		assertTrue(resolved.contains(bf.getBean("store2")));

		resolved = floatStoreProvider.stream().collect(Collectors.toList());
		assertEquals(1, resolved.size());
		assertTrue(resolved.contains(bf.getBean("store2")));

		resolved = floatStoreProvider.orderedStream().collect(Collectors.toList());
		assertEquals(1, resolved.size());
		assertTrue(resolved.contains(bf.getBean("store2")));
	}

	@Test
	public void testGenericMatchingWithUnresolvedOrderedStream() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		bf.setAutowireCandidateResolver(new GenericTypeAwareAutowireCandidateResolver());

		RootBeanDefinition bd1 = new RootBeanDefinition(NumberStoreFactory.class);
		bd1.setFactoryMethodName("newDoubleStore");
		bf.registerBeanDefinition("store1", bd1);
		RootBeanDefinition bd2 = new RootBeanDefinition(NumberStoreFactory.class);
		bd2.setFactoryMethodName("newFloatStore");
		bf.registerBeanDefinition("store2", bd2);

		ObjectProvider<NumberStore<?>> numberStoreProvider = bf.getBeanProvider(ResolvableType.forClass(NumberStore.class));
		List<NumberStore<?>> resolved = numberStoreProvider.orderedStream().collect(Collectors.toList());
		assertEquals(2, resolved.size());
		assertSame(bf.getBean("store2"), resolved.get(0));
		assertSame(bf.getBean("store1"), resolved.get(1));
	}


	@SuppressWarnings("serial")
	public static class NamedUrlList extends LinkedList<URL> {
	}


	@SuppressWarnings("serial")
	public static class NamedUrlSet extends HashSet<URL> {
	}


	@SuppressWarnings("serial")
	public static class NamedUrlMap extends HashMap<Integer, URL> {
	}


	public static class CollectionDependentBean {

		public CollectionDependentBean(NamedUrlList list, NamedUrlSet set, NamedUrlMap map) {
			assertEquals(1, list.size());
			assertEquals(1, set.size());
			assertEquals(1, map.size());
		}
	}


	@SuppressWarnings("serial")
	public static class UrlSet extends HashSet<URL> {

		public UrlSet() {
			super();
		}

		public UrlSet(Set<? extends URL> urls) {
			super();
		}

		public void setUrlNames(Set<URI> urlNames) throws MalformedURLException {
			for (URI urlName : urlNames) {
				add(urlName.toURL());
			}
		}
	}


	/**
	 * Pseudo-implementation of EasyMock's {@code MocksControl} class.
	 */
	public static class MocksControl {

		@SuppressWarnings("unchecked")
		public <T> T createMock(Class<T> toMock) {
			return (T) Proxy.newProxyInstance(BeanFactoryGenericsTests.class.getClassLoader(), new Class<?>[] {toMock},
					new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							throw new UnsupportedOperationException("mocked!");
						}
					});
		}
	}


	public static class NumberStore<T extends Number> {
	}


	public static class DoubleStore extends NumberStore<Double> {
	}


	public static class FloatStore extends NumberStore<Float> {
	}


	public static class NumberBean {

		private final NumberStore<Double> doubleStore;

		private final NumberStore<Float> floatStore;

		public NumberBean(NumberStore<Double> doubleStore, NumberStore<Float> floatStore) {
			this.doubleStore = doubleStore;
			this.floatStore = floatStore;
		}

		public NumberStore<Double> getDoubleStore() {
			return this.doubleStore;
		}

		public NumberStore<Float> getFloatStore() {
			return this.floatStore;
		}
	}


	public static class NumberStoreFactory {

		@Order(1)
		public static NumberStore<Double> newDoubleStore() {
			return new DoubleStore();
		}

		@Order(0)
		public static NumberStore<Float> newFloatStore() {
			return new FloatStore();
		}
	}

}
