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

package org.springframework.beans.factory.support;

import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.beans.testfixture.beans.GenericBean;
import org.springframework.beans.testfixture.beans.GenericIntegerBean;
import org.springframework.beans.testfixture.beans.GenericSetOfIntegerBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.OverridingClassLoader;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.UrlResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 20.01.2006
 */
class BeanFactoryGenericsTests {

	@Test
	void genericSetProperty() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		bd.getPropertyValues().add("integerSet", Set.of("4", "5"));
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getIntegerSet()).containsExactlyInAnyOrder(4, 5);
	}

	@Test
	void genericListProperty() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		List<String> input = List.of("http://localhost:8080", "http://localhost:9090");
		bd.getPropertyValues().add("resourceList", input);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getResourceList())
				.containsExactly(new UrlResource("http://localhost:8080"), new UrlResource("http://localhost:9090"));
	}

	@Test
	void genericListPropertyWithAutowiring() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("resource1", new UrlResource("http://localhost:8080"));
		bf.registerSingleton("resource2", new UrlResource("http://localhost:9090"));

		RootBeanDefinition bd = new RootBeanDefinition(GenericIntegerBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		bf.registerBeanDefinition("genericBean", bd);

		GenericIntegerBean gb = (GenericIntegerBean) bf.getBean("genericBean");
		assertThat(gb.getResourceList())
				.containsExactly(new UrlResource("http://localhost:8080"), new UrlResource("http://localhost:9090"));
	}

	@Test
	void genericListPropertyWithInvalidElementType() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericIntegerBean.class);
		bd.getPropertyValues().add("testBeanList", List.of(1));
		bf.registerBeanDefinition("genericBean", bd);

		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() -> bf.getBean("genericBean"))
				.withMessageContaining("genericBean")
				.withMessageContaining("testBeanList[0]")
				.withMessageContaining(TestBean.class.getName())
				.withMessageContaining("Integer");
	}

	@Test
	void genericListPropertyWithOptionalAutowiring() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getResourceList()).isNull();
	}

	@Test
	void genericMapProperty() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		Map<String, String> input = Map.of(
				"4", "5",
				"6", "7");
		bd.getPropertyValues().add("shortMap", input);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getShortMap().get(Short.valueOf("4"))).isEqualTo(5);
		assertThat(gb.getShortMap().get(Short.valueOf("6"))).isEqualTo(7);
	}

	@Test
	void genericListOfArraysProperty() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("listOfArrays");
		assertThat(gb.getListOfArrays()).containsExactly(new String[] {"value1", "value2"});
	}

	@Test
	void genericSetConstructor() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		Set<String> input = Set.of("4", "5");
		bd.getConstructorArgumentValues().addGenericArgumentValue(input);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getIntegerSet()).containsExactlyInAnyOrder(4, 5);
	}

	@Test
	void genericSetConstructorWithAutowiring() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("integer1", 4);
		bf.registerSingleton("integer2", 5);

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getIntegerSet()).containsExactlyInAnyOrder(4, 5);
	}

	@Test
	void genericSetConstructorWithOptionalAutowiring() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getIntegerSet()).isNull();
	}

	@Test
	void genericSetListConstructor() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		Set<String> input1 = Set.of("4", "5");
		List<String> input2 = List.of("http://localhost:8080", "http://localhost:9090");
		bd.getConstructorArgumentValues().addGenericArgumentValue(input1);
		bd.getConstructorArgumentValues().addGenericArgumentValue(input2);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getIntegerSet()).containsExactlyInAnyOrder(4, 5);
		assertThat(gb.getResourceList())
				.containsExactly(new UrlResource("http://localhost:8080"), new UrlResource("http://localhost:9090"));
	}

	@Test
	void genericSetListConstructorWithAutowiring() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("integer1", 4);
		bf.registerSingleton("integer2", 5);
		bf.registerSingleton("resource1", new UrlResource("http://localhost:8080"));
		bf.registerSingleton("resource2", new UrlResource("http://localhost:9090"));

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getIntegerSet()).containsExactlyInAnyOrder(4, 5);
		assertThat(gb.getResourceList())
				.containsExactly(new UrlResource("http://localhost:8080"), new UrlResource("http://localhost:9090"));
	}

	@Test
	void genericSetListConstructorWithOptionalAutowiring() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("resource1", new UrlResource("http://localhost:8080"));
		bf.registerSingleton("resource2", new UrlResource("http://localhost:9090"));

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getIntegerSet()).isNull();
		assertThat(gb.getResourceList()).isNull();
	}

	@Test
	void genericSetMapConstructor() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		Set<String> input1 = Set.of("4", "5");
		Map<String, String> input2 = Map.of(
				"4", "5",
				"6", "7");
		bd.getConstructorArgumentValues().addGenericArgumentValue(input1);
		bd.getConstructorArgumentValues().addGenericArgumentValue(input2);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getIntegerSet()).containsExactlyInAnyOrder(4, 5);
		assertThat(gb.getShortMap().get(Short.valueOf("4"))).isEqualTo(5);
		assertThat(gb.getShortMap().get(Short.valueOf("6"))).isEqualTo(7);
	}

	@Test
	void genericMapResourceConstructor() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		Map<String, String> input = Map.of(
				"4", "5",
				"6", "7");
		bd.getConstructorArgumentValues().addGenericArgumentValue(input);
		bd.getConstructorArgumentValues().addGenericArgumentValue("http://localhost:8080");
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getShortMap().get(Short.valueOf("4"))).isEqualTo(5);
		assertThat(gb.getShortMap().get(Short.valueOf("6"))).isEqualTo(7);
		assertThat(gb.getResourceList()).containsExactly(new UrlResource("http://localhost:8080"));
	}

	@Test
	void genericMapMapConstructor() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		Map<String, String> input1 = Map.of(
				"1", "0",
				"2", "3");
		Map<String, String> input2 = Map.of(
				"4", "5",
				"6", "7");
		bd.getConstructorArgumentValues().addGenericArgumentValue(input1);
		bd.getConstructorArgumentValues().addGenericArgumentValue(input2);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getShortMap()).isNotSameAs(gb.getPlainMap());
		assertThat(gb.getPlainMap()).hasSize(2);
		assertThat(gb.getPlainMap().get("1")).isEqualTo("0");
		assertThat(gb.getPlainMap().get("2")).isEqualTo("3");
		assertThat(gb.getShortMap()).hasSize(2);
		assertThat(gb.getShortMap().get(Short.valueOf("4"))).isEqualTo(5);
		assertThat(gb.getShortMap().get(Short.valueOf("6"))).isEqualTo(7);
	}

	@Test
	void genericMapMapConstructorWithSameRefAndConversion() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		Map<String, String> input = Map.of(
				"1", "0",
				"2", "3");
		bd.getConstructorArgumentValues().addGenericArgumentValue(input);
		bd.getConstructorArgumentValues().addGenericArgumentValue(input);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getShortMap()).isNotSameAs(gb.getPlainMap());
		assertThat(gb.getPlainMap()).hasSize(2);
		assertThat(gb.getPlainMap().get("1")).isEqualTo("0");
		assertThat(gb.getPlainMap().get("2")).isEqualTo("3");
		assertThat(gb.getShortMap()).hasSize(2);
		assertThat(gb.getShortMap().get(Short.valueOf("1"))).isEqualTo(0);
		assertThat(gb.getShortMap().get(Short.valueOf("2"))).isEqualTo(3);
	}

	@Test
	void genericMapMapConstructorWithSameRefAndNoConversion() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		Map<Short, Integer> input = new HashMap<>();
		input.put((short) 1, 0);
		input.put((short) 2, 3);
		bd.getConstructorArgumentValues().addGenericArgumentValue(input);
		bd.getConstructorArgumentValues().addGenericArgumentValue(input);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getShortMap()).isSameAs(gb.getPlainMap());
		assertThat(gb.getShortMap()).hasSize(2);
		assertThat(gb.getShortMap().get(Short.valueOf("1"))).isEqualTo(0);
		assertThat(gb.getShortMap().get(Short.valueOf("2"))).isEqualTo(3);
	}

	@Test
	void genericMapWithKeyTypeConstructor() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		Map<String, String> input = Map.of(
				"4", "5",
				"6", "7");
		bd.getConstructorArgumentValues().addGenericArgumentValue(input);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getLongMap().get(4L)).isEqualTo("5");
		assertThat(gb.getLongMap().get(6L)).isEqualTo("7");
	}

	@Test
	void genericMapWithCollectionValueConstructor() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.addPropertyEditorRegistrar(registry ->
				registry.registerCustomEditor(Number.class, new CustomNumberEditor(Integer.class, false)));

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		Map<String, Collection<?>> input = Map.of(
				"1", Set.of(1),
				"2", List.of(Boolean.TRUE));
		bd.getConstructorArgumentValues().addGenericArgumentValue(Boolean.TRUE);
		bd.getConstructorArgumentValues().addGenericArgumentValue(input);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getCollectionMap().get(1)).isInstanceOf(Set.class);
		assertThat(gb.getCollectionMap().get(2)).isInstanceOf(List.class);
	}

	@Test
	void genericSetFactoryMethod() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		bd.setFactoryMethodName("createInstance");
		Set<String> input = Set.of("4", "5");
		bd.getConstructorArgumentValues().addGenericArgumentValue(input);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getIntegerSet()).containsExactlyInAnyOrder(4, 5);
	}

	@Test
	void genericSetListFactoryMethod() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		bd.setFactoryMethodName("createInstance");
		Set<String> input1 = Set.of("4", "5");
		List<String> input2 = List.of("http://localhost:8080", "http://localhost:9090");
		bd.getConstructorArgumentValues().addGenericArgumentValue(input1);
		bd.getConstructorArgumentValues().addGenericArgumentValue(input2);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getIntegerSet()).containsExactlyInAnyOrder(4, 5);
		assertThat(gb.getResourceList())
				.containsExactly(new UrlResource("http://localhost:8080"), new UrlResource("http://localhost:9090"));
	}

	@Test
	void genericSetMapFactoryMethod() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		bd.setFactoryMethodName("createInstance");
		Set<String> input1 = Set.of("4", "5");
		Map<String, String> input2 = Map.of(
				"4", "5",
				"6", "7");
		bd.getConstructorArgumentValues().addGenericArgumentValue(input1);
		bd.getConstructorArgumentValues().addGenericArgumentValue(input2);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getIntegerSet()).containsExactlyInAnyOrder(4, 5);
		assertThat(gb.getShortMap().get(Short.valueOf("4"))).isEqualTo(5);
		assertThat(gb.getShortMap().get(Short.valueOf("6"))).isEqualTo(7);
	}

	@Test
	void genericMapResourceFactoryMethod() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		bd.setFactoryMethodName("createInstance");
		Map<String, String> input = Map.of(
				"4", "5",
				"6", "7");
		bd.getConstructorArgumentValues().addGenericArgumentValue(input);
		bd.getConstructorArgumentValues().addGenericArgumentValue("http://localhost:8080");
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getShortMap().get(Short.valueOf("4"))).isEqualTo(5);
		assertThat(gb.getShortMap().get(Short.valueOf("6"))).isEqualTo(7);
		assertThat(gb.getResourceList()).containsExactly(new UrlResource("http://localhost:8080"));
	}

	@Test
	void genericMapMapFactoryMethod() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		bd.setFactoryMethodName("createInstance");
		Map<String, String> input1 = Map.of(
				"1", "0",
				"2", "3");
		Map<String, String> input2 = Map.of(
				"4", "5",
				"6", "7");
		bd.getConstructorArgumentValues().addGenericArgumentValue(input1);
		bd.getConstructorArgumentValues().addGenericArgumentValue(input2);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getPlainMap().get("1")).isEqualTo("0");
		assertThat(gb.getPlainMap().get("2")).isEqualTo("3");
		assertThat(gb.getShortMap().get(Short.valueOf("4"))).isEqualTo(5);
		assertThat(gb.getShortMap().get(Short.valueOf("6"))).isEqualTo(7);
	}

	@Test
	void genericMapWithKeyTypeFactoryMethod() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		bd.setFactoryMethodName("createInstance");
		Map<String, String> input = Map.of(
				"4", "5",
				"6", "7");
		bd.getConstructorArgumentValues().addGenericArgumentValue(input);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getLongMap().get(Long.valueOf("4"))).isEqualTo("5");
		assertThat(gb.getLongMap().get(Long.valueOf("6"))).isEqualTo("7");
	}

	@Test
	void genericMapWithCollectionValueFactoryMethod() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.addPropertyEditorRegistrar(registry ->
				registry.registerCustomEditor(Number.class, new CustomNumberEditor(Integer.class, false)));

		RootBeanDefinition bd = new RootBeanDefinition(GenericBean.class);
		bd.setFactoryMethodName("createInstance");
		Map<String, Collection<?>> input = Map.of(
				"1", Set.of(1),
				"2", List.of(Boolean.TRUE));
		bd.getConstructorArgumentValues().addGenericArgumentValue(Boolean.TRUE);
		bd.getConstructorArgumentValues().addGenericArgumentValue(input);
		bf.registerBeanDefinition("genericBean", bd);

		GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");
		assertThat(gb.getCollectionMap().get(1)).isInstanceOf(Set.class);
		assertThat(gb.getCollectionMap().get(2)).isInstanceOf(List.class);
	}

	@Test
	void genericListBean() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));

		NamedUrlList list = bf.getBean("list", NamedUrlList.class);
		assertThat(list).containsExactly(new URL("http://localhost:8080"));
	}

	@Test
	void genericSetBean() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));

		NamedUrlSet set = bf.getBean("set", NamedUrlSet.class);
		assertThat(set).containsExactly(new URL("http://localhost:8080"));
	}

	@Test
	void genericMapBean() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));

		NamedUrlMap map = bf.getBean("map", NamedUrlMap.class);
		assertThat(map).containsExactly(entry(10, new URL("http://localhost:8080")));
	}

	@Test
	void genericallyTypedIntegerBean() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));

		GenericIntegerBean gb = (GenericIntegerBean) bf.getBean("integerBean");
		assertThat(gb.getGenericProperty()).isEqualTo(10);
		assertThat(gb.getGenericListProperty()).containsExactly(20, 30);
	}

	@Test
	void genericallyTypedSetOfIntegerBean() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));

		GenericSetOfIntegerBean gb = (GenericSetOfIntegerBean) bf.getBean("setOfIntegerBean");
		assertThat(gb.getGenericProperty()).singleElement().isEqualTo(10);
		assertThat(gb.getGenericListProperty()).satisfiesExactly(
				zero -> assertThat(zero).containsExactly(20),
				first -> assertThat(first).containsExactly(30));
	}

	@Test
	void setBean() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("genericBeanTests.xml", getClass()));

		UrlSet urlSet = bf.getBean("setBean", UrlSet.class);
		assertThat(urlSet).containsExactly(new URL("https://www.springframework.org"));
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
	void parameterizedStaticFactoryMethod() {
		RootBeanDefinition bd = new RootBeanDefinition(getClass());
		bd.setFactoryMethodName("createMockitoMock");
		bd.getConstructorArgumentValues().addGenericArgumentValue(Runnable.class);

		assertRunnableMockFactory(bd);
	}

	@Test
	void parameterizedStaticFactoryMethodWithWrappedClassName() {
		RootBeanDefinition bd = new RootBeanDefinition();
		bd.setBeanClassName(getClass().getName());
		bd.setFactoryMethodName("createMockitoMock");
		// TypedStringValue is used as an equivalent to an XML-defined argument String
		bd.getConstructorArgumentValues().addGenericArgumentValue(new TypedStringValue(Runnable.class.getName()));

		assertRunnableMockFactory(bd);
	}

	private void assertRunnableMockFactory(RootBeanDefinition bd) {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("mock", bd);

		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertThat(beans).hasSize(1);
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
	void parameterizedInstanceFactoryMethod() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd1 = new RootBeanDefinition(MocksControl.class);
		bf.registerBeanDefinition("mocksControl", bd1);

		RootBeanDefinition bd2 = new RootBeanDefinition();
		bd2.setFactoryBeanName("mocksControl");
		bd2.setFactoryMethodName("createMock");
		bd2.getConstructorArgumentValues().addGenericArgumentValue(Runnable.class);
		bf.registerBeanDefinition("mock", bd2);

		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertThat(beans).hasSize(1);
	}

	@Test
	void parameterizedInstanceFactoryMethodWithNonResolvedClassName() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd1 = new RootBeanDefinition(MocksControl.class);
		bf.registerBeanDefinition("mocksControl", bd1);

		RootBeanDefinition bd2 = new RootBeanDefinition();
		bd2.setFactoryBeanName("mocksControl");
		bd2.setFactoryMethodName("createMock");
		bd2.getConstructorArgumentValues().addGenericArgumentValue(Runnable.class.getName());
		bf.registerBeanDefinition("mock", bd2);

		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertThat(beans).hasSize(1);
	}

	@Test
	void parameterizedInstanceFactoryMethodWithInvalidClassName() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd1 = new RootBeanDefinition(MocksControl.class);
		bf.registerBeanDefinition("mocksControl", bd1);

		RootBeanDefinition rbd2 = new RootBeanDefinition();
		rbd2.setFactoryBeanName("mocksControl");
		rbd2.setFactoryMethodName("createMock");
		rbd2.getConstructorArgumentValues().addGenericArgumentValue("x");
		bf.registerBeanDefinition("mock", rbd2);

		assertThat(bf.isTypeMatch("mock", Runnable.class)).isFalse();
		assertThat(bf.isTypeMatch("mock", Runnable.class)).isFalse();
		assertThat(bf.getType("mock")).isNull();
		assertThat(bf.getType("mock")).isNull();
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertThat(beans).isEmpty();
	}

	@Test
	void parameterizedInstanceFactoryMethodWithIndexedArgument() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition bd1 = new RootBeanDefinition(MocksControl.class);
		bf.registerBeanDefinition("mocksControl", bd1);

		RootBeanDefinition bd2 = new RootBeanDefinition();
		bd2.setFactoryBeanName("mocksControl");
		bd2.setFactoryMethodName("createMock");
		bd2.getConstructorArgumentValues().addIndexedArgumentValue(0, Runnable.class);
		bf.registerBeanDefinition("mock", bd2);

		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertThat(beans).hasSize(1);
	}

	@Test  // SPR-16720
	void parameterizedInstanceFactoryMethodWithTempClassLoader() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setTempClassLoader(new OverridingClassLoader(getClass().getClassLoader()));

		RootBeanDefinition bd1 = new RootBeanDefinition(MocksControl.class);
		bf.registerBeanDefinition("mocksControl", bd1);

		RootBeanDefinition bd2 = new RootBeanDefinition();
		bd2.setFactoryBeanName("mocksControl");
		bd2.setFactoryMethodName("createMock");
		bd2.getConstructorArgumentValues().addGenericArgumentValue(Runnable.class);
		bf.registerBeanDefinition("mock", bd2);

		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
		Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
		assertThat(beans).hasSize(1);
	}

	@Test
	void genericMatchingWithBeanNameDifferentiation() {
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
		assertThat(numberStoreNames).containsExactly("doubleStore", "floatStore");
		assertThat(doubleStoreNames).isEmpty();
		assertThat(floatStoreNames).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(classes = {NumberStoreFactory.class, NumberStoreFactoryBeans.class})
	void genericMatchingWithFullTypeDifferentiation(Class<?> factoryClass) {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		bf.setAutowireCandidateResolver(new GenericTypeAwareAutowireCandidateResolver());

		RootBeanDefinition bd1 = new RootBeanDefinition(factoryClass);
		bd1.setFactoryMethodName("newDoubleStore");
		bf.registerBeanDefinition("store1", bd1);
		RootBeanDefinition bd2 = new RootBeanDefinition(factoryClass);
		bd2.setFactoryMethodName("newFloatStore");
		bf.registerBeanDefinition("store2", bd2);
		RootBeanDefinition bd3 = new RootBeanDefinition(NumberBean.class);
		bd3.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		bd3.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		bf.registerBeanDefinition("numberBean", bd3);

		NumberStore<?> store1 = bf.getBean("store1", NumberStore.class);
		NumberStore<?> store2 = bf.getBean("store2", NumberStore.class);
		NumberBean nb = bf.getBean(NumberBean.class);
		assertThat(nb.getDoubleStore()).isSameAs(store1);
		assertThat(nb.getFloatStore()).isSameAs(store2);
		nb = bf.getBean(NumberBean.class);
		assertThat(nb.getDoubleStore()).isSameAs(store1);
		assertThat(nb.getFloatStore()).isSameAs(store2);

		String[] numberStoreNames = bf.getBeanNamesForType(ResolvableType.forClass(NumberStore.class));
		String[] doubleStoreNames = bf.getBeanNamesForType(ResolvableType.forClassWithGenerics(NumberStore.class, Double.class));
		String[] floatStoreNames = bf.getBeanNamesForType(ResolvableType.forClassWithGenerics(NumberStore.class, Float.class));
		assertThat(numberStoreNames).containsExactly("store1", "store2");
		assertThat(doubleStoreNames).containsExactly("store1");
		assertThat(floatStoreNames).containsExactly("store2");

		ObjectProvider<NumberStore<?>> numberStoreProvider = bf.getBeanProvider(ResolvableType.forClass(NumberStore.class));
		ObjectProvider<NumberStore<Double>> doubleStoreProvider = bf.getBeanProvider(ResolvableType.forClassWithGenerics(NumberStore.class, Double.class));
		ObjectProvider<NumberStore<Float>> floatStoreProvider = bf.getBeanProvider(ResolvableType.forClassWithGenerics(NumberStore.class, Float.class));
		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(numberStoreProvider::getObject);
		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(numberStoreProvider::getIfAvailable);
		assertThat(numberStoreProvider.getIfUnique()).isNull();
		assertThat(doubleStoreProvider.getObject()).isSameAs(store1);
		assertThat(doubleStoreProvider.getIfAvailable()).isSameAs(store1);
		assertThat(doubleStoreProvider.getIfUnique()).isSameAs(store1);
		assertThat(floatStoreProvider.getObject()).isSameAs(store2);
		assertThat(floatStoreProvider.getIfAvailable()).isSameAs(store2);
		assertThat(floatStoreProvider.getIfUnique()).isSameAs(store2);

		List<NumberStore<?>> resolved = new ArrayList<>();
		for (NumberStore<?> instance : numberStoreProvider) {
			resolved.add(instance);
		}
		assertThat(resolved).containsExactly(store1, store2);
		assertThat(numberStoreProvider.stream()).containsExactly(store1, store2);
		assertThat(numberStoreProvider.orderedStream()).containsExactly(store2, store1);

		resolved = new ArrayList<>();
		for (NumberStore<Double> instance : doubleStoreProvider) {
			resolved.add(instance);
		}
		assertThat(resolved).containsExactly(store1);
		assertThat(doubleStoreProvider.stream()).singleElement().isEqualTo(store1);
		assertThat(doubleStoreProvider.orderedStream()).singleElement().isEqualTo(store1);

		resolved = new ArrayList<>();
		for (NumberStore<Float> instance : floatStoreProvider) {
			resolved.add(instance);
		}
		assertThat(resolved).containsExactly(store2);
		assertThat(floatStoreProvider.stream()).singleElement().isEqualTo(store2);
		assertThat(floatStoreProvider.orderedStream()).singleElement().isEqualTo(store2);
	}

	@ParameterizedTest
	@ValueSource(classes = {NumberStoreFactory.class, NumberStoreFactoryBeans.class})
	void genericMatchingWithUnresolvedOrderedStream(Class<?> factoryClass) {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		bf.setAutowireCandidateResolver(new GenericTypeAwareAutowireCandidateResolver());

		RootBeanDefinition bd1 = new RootBeanDefinition(factoryClass);
		bd1.setFactoryMethodName("newDoubleStore");
		bf.registerBeanDefinition("store1", bd1);
		RootBeanDefinition bd2 = new RootBeanDefinition(factoryClass);
		bd2.setFactoryMethodName("newFloatStore");
		bf.registerBeanDefinition("store2", bd2);

		ObjectProvider<NumberStore<?>> numberStoreProvider = bf.getBeanProvider(ResolvableType.forClass(NumberStore.class));
		assertThat(numberStoreProvider.orderedStream()).containsExactly(
				bf.getBean("store2", NumberStore.class), bf.getBean("store1", NumberStore.class));
	}

	@Test  // gh-32489
	void genericMatchingAgainstFactoryBeanClass() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setAutowireCandidateResolver(new GenericTypeAwareAutowireCandidateResolver());

		RootBeanDefinition bd = new RootBeanDefinition(MyFactoryBean.class);
		bd.setTargetType(ResolvableType.forClassWithGenerics(MyFactoryBean.class, String.class));
		bf.registerBeanDefinition("myFactoryBean", bd);
		bf.registerBeanDefinition("myFactoryBeanHolder",
				new RootBeanDefinition(MyFactoryBeanHolder.class, AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR, false));

		assertThat(bf.getBean(MyFactoryBeanHolder.class).factoryBeans).containsOnly(bf.getBean(MyFactoryBean.class));
		assertThat(bf.getBeanProvider(MyGenericInterfaceForFactoryBeans.class)).containsOnly(bf.getBean(MyFactoryBean.class));
		assertThat(bf.getBeanProvider(bd.getResolvableType())).containsOnly(bf.getBean(MyFactoryBean.class));
	}

	@Test  // gh-32489
	void genericMatchingAgainstLazyFactoryBeanClass() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setAutowireCandidateResolver(new GenericTypeAwareAutowireCandidateResolver());

		RootBeanDefinition bd = new RootBeanDefinition(MyFactoryBean.class);
		// Replicate org.springframework.data.repository.config.RepositoryConfigurationDelegate#registerRepositoriesIn
		// behavior of setting targetType, required to hit other branch in
		// org.springframework.beans.factory.support.GenericTypeAwareAutowireCandidateResolver.checkGenericTypeMatch
		bd.setTargetType(ResolvableType.forClassWithGenerics(MyFactoryBean.class, String.class));
		bd.setLazyInit(true);
		bf.registerBeanDefinition("myFactoryBean", bd);
		bf.registerBeanDefinition("myFactoryBeanHolder",
				new RootBeanDefinition(MyFactoryBeanHolder.class, AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR, false));

		assertThat(bf.getBeanProvider(bd.getResolvableType())).containsOnly(bf.getBean(MyFactoryBean.class));
		assertThat(bf.getBeanProvider(MyGenericInterfaceForFactoryBeans.class)).containsOnly(bf.getBean(MyFactoryBean.class));
		assertThat(bf.getBean(MyFactoryBeanHolder.class).factoryBeans).containsOnly(bf.getBean(MyFactoryBean.class));
		assertThat(bf.getBeanProvider(bd.getResolvableType())).containsOnly(bf.getBean(MyFactoryBean.class));
	}


	/**
	 * Mimics and delegates to {@link Mockito#mock(Class)} -- created here to avoid factory
	 * method resolution issues caused by the introduction of {@code Mockito.mock(T...)}
	 * in Mockito 4.10.
	 */
	public static <T> T createMockitoMock(Class<T> classToMock) {
		return Mockito.mock(classToMock);
	}


	@SuppressWarnings("serial")
	public static class NamedUrlList extends ArrayList<URL> {
	}


	@SuppressWarnings("serial")
	public static class NamedUrlSet extends HashSet<URL> {
	}


	@SuppressWarnings("serial")
	public static class NamedUrlMap extends HashMap<Integer, URL> {
	}


	public static class CollectionDependentBean {

		public CollectionDependentBean(NamedUrlList list, NamedUrlSet set, NamedUrlMap map) {
			assertThat(list).hasSize(1);
			assertThat(set).hasSize(1);
			assertThat(map).hasSize(1);
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
					(proxy, method, args) -> {
						throw new UnsupportedOperationException("mocked!");
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


	public static class NumberStoreFactoryBeans {

		@Order(1)
		public static FactoryBean<NumberStore<Double>> newDoubleStore() {
			return new FactoryBean<>() {
				@Override
				public NumberStore<Double> getObject() {
					return new DoubleStore();
				}
				@Override
				public Class<?> getObjectType() {
					return DoubleStore.class;
				}
			};
		}

		@Order(0)
		public static FactoryBean<NumberStore<Float>> newFloatStore() {
			return new FactoryBean<>() {
				@Override
				public NumberStore<Float> getObject() {
					return new FloatStore();
				}
				@Override
				public Class<?> getObjectType() {
					return FloatStore.class;
				}
			};
		}
	}


	public interface MyGenericInterfaceForFactoryBeans<T> {
	}


	public static class MyFactoryBean<T extends CharSequence> implements FactoryBean<T>, MyGenericInterfaceForFactoryBeans<T> {

		@Override
		public T getObject() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Class<?> getObjectType() {
			return String.class;
		}
	}


	public static class MyFactoryBeanHolder {

		List<MyGenericInterfaceForFactoryBeans<?>> factoryBeans;  // Requested type is not a FactoryBean type

		public MyFactoryBeanHolder(List<MyGenericInterfaceForFactoryBeans<?>> factoryBeans) {
			this.factoryBeans = factoryBeans;
		}
	}

}
