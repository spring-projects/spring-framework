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

import org.junit.jupiter.api.Test;
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
import org.springframework.tests.EnabledForTestGroups;
import org.springframework.tests.sample.beans.GenericBean;
import org.springframework.tests.sample.beans.GenericIntegerBean;
import org.springframework.tests.sample.beans.GenericSetOfIntegerBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.tests.TestGroup.LONG_RUNNING;

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

		assertThat(gb.getIntegerSet().contains(new Integer(4))).isTrue();
		assertThat(gb.getIntegerSet().contains(new Integer(5))).isTrue();
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

		assertThat(gb.getResourceList().get(0)).isEqualTo(new UrlResource("http://localhost:8080"));
		assertThat(gb.getResourceList().get(1)).isEqualTo(new UrlResource("http://localhost:9090"));
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

		assertThat(gb.getResourceList().get(0)).isEqualTo(new UrlResource("http://localhost:8080"));
		assertThat(gb.getResourceList().get(1)).isEqualTo(new UrlResource("http://localhost:9090"));
	}

	@Test
	public void testGenericListPropertyWithInvalidElementType() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(GenericIntegerBean.class);

		List<Integer> input = new ArrayList<>();
		input.add(1);
		rbd.getPropertyValues().add("testBeanList", input);

		bf.registerBeanDefinition("genericBean", rbd);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				bf.getBean("genericBean"))
			.withMessageContaining("genericBean")
			.withMessageContaining("testBeanList[0]")
			.withMessageContaining(TestBean.class.getName())
			.withMessageContaining("Integer");
	}

	@Test
	public void testGenericListPropertyWithOptionalAutowiring() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertThat(gb.getResourceList()).isNull();
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

		assertThat(gb.getShortMap().get(new Short("4"))).isEqualTo(new Integer(5));
		assertThat(gb.getShortMap().get(new Short("6"))).isEqualTo(new Integer(7));
	}

	@Test
	public void testGenericListOfArraysProperty() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("listOfArrays");

		assertThat(gb.getListOfArrays().size()).isEqualTo(1);
		String[] array = gb.getListOfArrays().get(0);
		assertThat(array.length).isEqualTo(2);
		assertThat(array[0]).isEqualTo("value1");
		assertThat(array[1]).isEqualTo("value2");
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

		assertThat(gb.getIntegerSet().contains(new Integer(4))).isTrue();
		assertThat(gb.getIntegerSet().contains(new Integer(5))).isTrue();
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

		assertThat(gb.getIntegerSet().contains(new Integer(4))).isTrue();
		assertThat(gb.getIntegerSet().contains(new Integer(5))).isTrue();
	}

	@Test
	public void testGenericSetConstructorWithOptionalAutowiring() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		bf.registerBeanDefinition("genericBean", rbd);
		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

		assertThat(gb.getIntegerSet()).isNull();
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

		assertThat(gb.getIntegerSet().contains(new Integer(4))).isTrue();
		assertThat(gb.getIntegerSet().contains(new Integer(5))).isTrue();
		assertThat(gb.getResourceList().get(0)).isEqualTo(new UrlResource("http://localhost:8080"));
		assertThat(gb.getResourceList().get(1)).isEqualTo(new UrlResource("http://localhost:9090"));
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

		assertThat(gb.getIntegerSet().contains(new Integer(4))).isTrue();
		assertThat(gb.getIntegerSet().contains(new Integer(5))).isTrue();
		assertThat(gb.getResourceList().get(0)).isEqualTo(new UrlResource("http://localhost:8080"));
		assertThat(gb.getResourceList().get(1)).isEqualTo(new UrlResource("http://localhost:9090"));
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

		assertThat(gb.getIntegerSet()).isNull();
		assertThat(gb.getResourceList()).isNull();
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

		assertThat(gb.getIntegerSet().contains(new Integer(4))).isTrue();
		assertThat(gb.getIntegerSet().contains(new Integer(5))).isTrue();
		assertThat(gb.getShortMap().get(new Short("4"))).isEqualTo(new Integer(5));
		assertThat(gb.getShortMap().get(new Short("6"))).isEqualTo(new Integer(7));
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

		assertThat(gb.getShortMap().get(new Short("4"))).isEqualTo(new Integer(5));
		assertThat(gb.getShortMap().get(new Short("6"))).isEqualTo(new Integer(7));
		assertThat(gb.getResourceList().get(0)).isEqualTo(new UrlResource("http://localhost:8080"));
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

		assertThat(gb.getShortMap()).isNotSameAs(gb.getPlainMap());
		assertThat(gb.getPlainMap().size()).isEqualTo(2);
		assertThat(gb.getPlainMap().get("1")).isEqualTo("0");
		assertThat(gb.getPlainMap().get("2")).isEqualTo("3");
		assertThat(gb.getShortMap().size()).isEqualTo(2);
		assertThat(gb.getShortMap().get(new Short("4"))).isEqualTo(new Integer(5));
		assertThat(gb.getShortMap().get(new Short("6"))).isEqualTo(new Integer(7));
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

		assertThat(gb.getShortMap()).isNotSameAs(gb.getPlainMap());
		assertThat(gb.getPlainMap().size()).isEqualTo(2);
		assertThat(gb.getPlainMap().get("1")).isEqualTo("0");
		assertThat(gb.getPlainMap().get("2")).isEqualTo("3");
		assertThat(gb.getShortMap().size()).isEqualTo(2);
		assertThat(gb.getShortMap().get(new Short("1"))).isEqualTo(new Integer(0));
		assertThat(gb.getShortMap().get(new Short("2"))).isEqualTo(new Integer(3));
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

		assertThat(gb.getShortMap()).isSameAs(gb.getPlainMap());
		assertThat(gb.getShortMap().size()).isEqualTo(2);
		assertThat(gb.getShortMap().get(new Short("1"))).isEqualTo(new Integer(0));
		assertThat(gb.getShortMap().get(new Short("2"))).isEqualTo(new Integer(3));
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

		assertThat(gb.getLongMap().get(new Long("4"))).isEqualTo("5");
		assertThat(gb.getLongMap().get(new Long("6"))).isEqualTo("7");
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

		boolean condition1 = gb.getCollectionMap().get(new Integer(1)) instanceof HashSet;
		assertThat(condition1).isTrue();
		boolean condition = gb.getCollectionMap().get(new Integer(2)) instanceof ArrayList;
		assertThat(condition).isTrue();
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

		assertThat(gb.getIntegerSet().contains(new Integer(4))).isTrue();
		assertThat(gb.getIntegerSet().contains(new Integer(5))).isTrue();
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

		assertThat(gb.getIntegerSet().contains(new Integer(4))).isTrue();
		assertThat(gb.getIntegerSet().contains(new Integer(5))).isTrue();
		assertThat(gb.getResourceList().get(0)).isEqualTo(new UrlResource("http://localhost:8080"));
		assertThat(gb.getResourceList().get(1)).isEqualTo(new UrlResource("http://localhost:9090"));
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

		assertThat(gb.getIntegerSet().contains(new Integer(4))).isTrue();
		assertThat(gb.getIntegerSet().contains(new Integer(5))).isTrue();
		assertThat(gb.getShortMap().get(new Short("4"))).isEqualTo(new Integer(5));
		assertThat(gb.getShortMap().get(new Short("6"))).isEqualTo(new Integer(7));
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

		assertThat(gb.getShortMap().get(new Short("4"))).isEqualTo(new Integer(5));
		assertThat(gb.getShortMap().get(new Short("6"))).isEqualTo(new Integer(7));
		assertThat(gb.getResourceList().get(0)).isEqualTo(new UrlResource("http://localhost:8080"));
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

		assertThat(gb.getPlainMap().get("1")).isEqualTo("0");
		assertThat(gb.getPlainMap().get("2")).isEqualTo("3");
		assertThat(gb.getShortMap().get(new Short("4"))).isEqualTo(new Integer(5));
		assertThat(gb.getShortMap().get(new Short("6"))).isEqualTo(new Integer(7));
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

		assertThat(gb.getLongMap().get(new Long("4"))).isEqualTo("5");
		assertThat(gb.getLongMap().get(new Long("6"))).isEqualTo("7");
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

		boolean condition1 = gb.getCollectionMap().get(new Integer(1)) instanceof HashSet;
		assertThat(condition1).isTrue();
		boolean condition = gb.getCollectionMap().get(new Integer(2)) instanceof ArrayList;
		assertThat(condition).isTrue();
	}

	@Test
	public void testGenericListBean() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));
		List<?> list = (List<?>) bf.getBean("list");
		assertThat(list.size()).isEqualTo(1);
		assertThat(list.get(0)).isEqualTo(new URL("http://localhost:8080"));
	}

	@Test
	public void testGenericSetBean() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));
		Set<?> set = (Set<?>) bf.getBean("set");
		assertThat(set.size()).isEqualTo(1);
		assertThat(set.iterator().next()).isEqualTo(new URL("http://localhost:8080"));
	}

	@Test
	public void testGenericMapBean() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));
		Map<?, ?> map = (Map<?, ?>) bf.getBean("map");
		assertThat(map.size()).isEqualTo(1);
		assertThat(map.keySet().iterator().next()).isEqualTo(new Integer(10));
		assertThat(map.values().iterator().next()).isEqualTo(new URL("http://localhost:8080"));
	}

	@Test
	public void testGenericallyTypedIntegerBean() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));
		GenericIntegerBean gb = (GenericIntegerBean) bf.getBean("integerBean");
		assertThat(gb.getGenericProperty()).isEqualTo(new Integer(10));
		assertThat(gb.getGenericListProperty().get(0)).isEqualTo(new Integer(20));
		assertThat(gb.getGenericListProperty().get(1)).isEqualTo(new Integer(30));
	}

	@Test
	public void testGenericallyTypedSetOfIntegerBean() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));
		GenericSetOfIntegerBean gb = (GenericSetOfIntegerBean) bf.getBean("setOfIntegerBean");
		assertThat(gb.getGenericProperty().iterator().next()).isEqualTo(new Integer(10));
		assertThat(gb.getGenericListProperty().get(0).iterator().next()).isEqualTo(new Integer(20));
		assertThat(gb.getGenericListProperty().get(1).iterator().next()).isEqualTo(new Integer(30));
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	public void testSetBean() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));
		UrlSet us = (UrlSet) bf.getBean("setBean");
		assertThat(us.size()).isEqualTo(1);
		assertThat(us.iterator().next()).isEqualTo(new URL("https://www.springframework.org"));
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

		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertThat(beans.size()).isEqualTo(1);
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

		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertThat(beans.size()).isEqualTo(1);
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

		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertThat(beans.size()).isEqualTo(1);
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

		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertThat(beans.size()).isEqualTo(1);
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

		assertThat(bf.isTypeMatch("mock", Runnable.class)).isFalse();
		assertThat(bf.isTypeMatch("mock", Runnable.class)).isFalse();
		assertThat(bf.getType("mock")).isNull();
		assertThat(bf.getType("mock")).isNull();
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertThat(beans.size()).isEqualTo(0);
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

		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertThat(beans.size()).isEqualTo(1);
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

		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertThat(beans.size()).isEqualTo(1);
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
		assertThat(nb.getDoubleStore()).isSameAs(bf.getBean("doubleStore"));
		assertThat(nb.getFloatStore()).isSameAs(bf.getBean("floatStore"));

		String[] numberStoreNames = bf.getBeanNamesForType(ResolvableType.forClass(NumberStore.class));
		String[] doubleStoreNames = bf.getBeanNamesForType(ResolvableType.forClassWithGenerics(NumberStore.class, Double.class));
		String[] floatStoreNames = bf.getBeanNamesForType(ResolvableType.forClassWithGenerics(NumberStore.class, Float.class));
		assertThat(numberStoreNames.length).isEqualTo(2);
		assertThat(numberStoreNames[0]).isEqualTo("doubleStore");
		assertThat(numberStoreNames[1]).isEqualTo("floatStore");
		assertThat(doubleStoreNames.length).isEqualTo(0);
		assertThat(floatStoreNames.length).isEqualTo(0);
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
		assertThat(nb.getDoubleStore()).isSameAs(bf.getBean("store1"));
		assertThat(nb.getFloatStore()).isSameAs(bf.getBean("store2"));

		String[] numberStoreNames = bf.getBeanNamesForType(ResolvableType.forClass(NumberStore.class));
		String[] doubleStoreNames = bf.getBeanNamesForType(ResolvableType.forClassWithGenerics(NumberStore.class, Double.class));
		String[] floatStoreNames = bf.getBeanNamesForType(ResolvableType.forClassWithGenerics(NumberStore.class, Float.class));
		assertThat(numberStoreNames.length).isEqualTo(2);
		assertThat(numberStoreNames[0]).isEqualTo("store1");
		assertThat(numberStoreNames[1]).isEqualTo("store2");
		assertThat(doubleStoreNames.length).isEqualTo(1);
		assertThat(doubleStoreNames[0]).isEqualTo("store1");
		assertThat(floatStoreNames.length).isEqualTo(1);
		assertThat(floatStoreNames[0]).isEqualTo("store2");

		ObjectProvider<NumberStore<?>> numberStoreProvider = bf.getBeanProvider(ResolvableType.forClass(NumberStore.class));
		ObjectProvider<NumberStore<Double>> doubleStoreProvider = bf.getBeanProvider(ResolvableType.forClassWithGenerics(NumberStore.class, Double.class));
		ObjectProvider<NumberStore<Float>> floatStoreProvider = bf.getBeanProvider(ResolvableType.forClassWithGenerics(NumberStore.class, Float.class));
		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(numberStoreProvider::getObject);
		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(numberStoreProvider::getIfAvailable);
		assertThat(numberStoreProvider.getIfUnique()).isNull();
		assertThat(doubleStoreProvider.getObject()).isSameAs(bf.getBean("store1"));
		assertThat(doubleStoreProvider.getIfAvailable()).isSameAs(bf.getBean("store1"));
		assertThat(doubleStoreProvider.getIfUnique()).isSameAs(bf.getBean("store1"));
		assertThat(floatStoreProvider.getObject()).isSameAs(bf.getBean("store2"));
		assertThat(floatStoreProvider.getIfAvailable()).isSameAs(bf.getBean("store2"));
		assertThat(floatStoreProvider.getIfUnique()).isSameAs(bf.getBean("store2"));

		List<NumberStore<?>> resolved = new ArrayList<>();
		for (NumberStore<?> instance : numberStoreProvider) {
			resolved.add(instance);
		}
		assertThat(resolved.size()).isEqualTo(2);
		assertThat(resolved.get(0)).isSameAs(bf.getBean("store1"));
		assertThat(resolved.get(1)).isSameAs(bf.getBean("store2"));

		resolved = numberStoreProvider.stream().collect(Collectors.toList());
		assertThat(resolved.size()).isEqualTo(2);
		assertThat(resolved.get(0)).isSameAs(bf.getBean("store1"));
		assertThat(resolved.get(1)).isSameAs(bf.getBean("store2"));

		resolved = numberStoreProvider.orderedStream().collect(Collectors.toList());
		assertThat(resolved.size()).isEqualTo(2);
		assertThat(resolved.get(0)).isSameAs(bf.getBean("store2"));
		assertThat(resolved.get(1)).isSameAs(bf.getBean("store1"));

		resolved = new ArrayList<>();
		for (NumberStore<Double> instance : doubleStoreProvider) {
			resolved.add(instance);
		}
		assertThat(resolved.size()).isEqualTo(1);
		assertThat(resolved.contains(bf.getBean("store1"))).isTrue();

		resolved = doubleStoreProvider.stream().collect(Collectors.toList());
		assertThat(resolved.size()).isEqualTo(1);
		assertThat(resolved.contains(bf.getBean("store1"))).isTrue();

		resolved = doubleStoreProvider.orderedStream().collect(Collectors.toList());
		assertThat(resolved.size()).isEqualTo(1);
		assertThat(resolved.contains(bf.getBean("store1"))).isTrue();

		resolved = new ArrayList<>();
		for (NumberStore<Float> instance : floatStoreProvider) {
			resolved.add(instance);
		}
		assertThat(resolved.size()).isEqualTo(1);
		assertThat(resolved.contains(bf.getBean("store2"))).isTrue();

		resolved = floatStoreProvider.stream().collect(Collectors.toList());
		assertThat(resolved.size()).isEqualTo(1);
		assertThat(resolved.contains(bf.getBean("store2"))).isTrue();

		resolved = floatStoreProvider.orderedStream().collect(Collectors.toList());
		assertThat(resolved.size()).isEqualTo(1);
		assertThat(resolved.contains(bf.getBean("store2"))).isTrue();
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
		assertThat(resolved.size()).isEqualTo(2);
		assertThat(resolved.get(0)).isSameAs(bf.getBean("store2"));
		assertThat(resolved.get(1)).isSameAs(bf.getBean("store1"));
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
			assertThat(list.size()).isEqualTo(1);
			assertThat(set.size()).isEqualTo(1);
			assertThat(map.size()).isEqualTo(1);
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
