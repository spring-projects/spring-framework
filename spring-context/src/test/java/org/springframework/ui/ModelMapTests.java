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

package org.springframework.ui;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class ModelMapTests {

	@Test
	public void testNoArgCtorYieldsEmptyModel() throws Exception {
		assertThat(new ModelMap()).isEmpty();
	}

	/*
	 * SPR-2185 - Null model assertion causes backwards compatibility issue
	 */
	@Test
	public void testAddNullObjectWithExplicitKey() throws Exception {
		ModelMap model = new ModelMap();
		model.addAttribute("foo", null);
		assertThat(model.containsKey("foo")).isTrue();
		assertThat(model.get("foo")).isNull();
	}

	/*
	 * SPR-2185 - Null model assertion causes backwards compatibility issue
	 */
	@Test
	public void testAddNullObjectViaCtorWithExplicitKey() throws Exception {
		ModelMap model = new ModelMap("foo", null);
		assertThat(model.containsKey("foo")).isTrue();
		assertThat(model.get("foo")).isNull();
	}

	@Test
	public void testNamedObjectCtor() throws Exception {
		ModelMap model = new ModelMap("foo", "bing");
		assertThat(model).hasSize(1);
		String bing = (String) model.get("foo");
		assertThat(bing).isNotNull();
		assertThat(bing).isEqualTo("bing");
	}

	@Test
	public void testUnnamedCtorScalar() throws Exception {
		ModelMap model = new ModelMap("foo", "bing");
		assertThat(model).hasSize(1);
		String bing = (String) model.get("foo");
		assertThat(bing).isNotNull();
		assertThat(bing).isEqualTo("bing");
	}

	@Test
	public void testOneArgCtorWithScalar() throws Exception {
		ModelMap model = new ModelMap("bing");
		assertThat(model).hasSize(1);
		String string = (String) model.get("string");
		assertThat(string).isNotNull();
		assertThat(string).isEqualTo("bing");
	}

	@Test
	public void testOneArgCtorWithNull() {
		//Null model arguments added without a name being explicitly supplied are not allowed
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ModelMap(null));
	}

	@Test
	public void testOneArgCtorWithCollection() throws Exception {
		ModelMap model = new ModelMap(new String[]{"foo", "boing"});
		assertThat(model).hasSize(1);
		String[] strings = (String[]) model.get("stringList");
		assertThat(strings).isNotNull();
		assertThat(strings).hasSize(2);
		assertThat(strings[0]).isEqualTo("foo");
		assertThat(strings[1]).isEqualTo("boing");
	}

	@Test
	public void testOneArgCtorWithEmptyCollection() throws Exception {
		ModelMap model = new ModelMap(new HashSet<>());
		// must not add if collection is empty...
		assertThat(model).isEmpty();
	}

	@Test
	public void testAddObjectWithNull() throws Exception {
		// Null model arguments added without a name being explicitly supplied are not allowed
		ModelMap model = new ModelMap();
		assertThatIllegalArgumentException().isThrownBy(() ->
				model.addAttribute(null));
	}

	@Test
	public void testAddObjectWithEmptyArray() throws Exception {
		ModelMap model = new ModelMap(new int[]{});
		assertThat(model).hasSize(1);
		int[] ints = (int[]) model.get("intList");
		assertThat(ints).isNotNull();
		assertThat(ints).isEmpty();
	}

	@Test
	public void testAddAllObjectsWithNullMap() throws Exception {
		ModelMap model = new ModelMap();
		model.addAllAttributes((Map<String, ?>) null);
		assertThat(model).isEmpty();
	}

	@Test
	public void testAddAllObjectsWithNullCollection() throws Exception {
		ModelMap model = new ModelMap();
		model.addAllAttributes((Collection<Object>) null);
		assertThat(model).isEmpty();
	}

	@Test
	public void testAddAllObjectsWithSparseArrayList() throws Exception {
		// Null model arguments added without a name being explicitly supplied are not allowed
		ModelMap model = new ModelMap();
		ArrayList<String> list = new ArrayList<>();
		list.add("bing");
		list.add(null);
		assertThatIllegalArgumentException().isThrownBy(() ->
				model.addAllAttributes(list));
	}

	@Test
	public void testAddMap() throws Exception {
		Map<String, String> map = new HashMap<>();
		map.put("one", "one-value");
		map.put("two", "two-value");
		ModelMap model = new ModelMap();
		model.addAttribute(map);
		assertThat(model).hasSize(1);
		String key = StringUtils.uncapitalize(ClassUtils.getShortName(map.getClass()));
		assertThat(model.containsKey(key)).isTrue();
	}

	@Test
	public void testAddObjectNoKeyOfSameTypeOverrides() throws Exception {
		ModelMap model = new ModelMap();
		model.addAttribute("foo");
		model.addAttribute("bar");
		assertThat(model).hasSize(1);
		String bar = (String) model.get("string");
		assertThat(bar).isEqualTo("bar");
	}

	@Test
	public void testAddListOfTheSameObjects() throws Exception {
		List<TestBean> beans = new ArrayList<>();
		beans.add(new TestBean("one"));
		beans.add(new TestBean("two"));
		beans.add(new TestBean("three"));
		ModelMap model = new ModelMap();
		model.addAllAttributes(beans);
		assertThat(model).hasSize(1);
	}

	@Test
	public void testMergeMapWithOverriding() throws Exception {
		Map<String, TestBean> beans = new HashMap<>();
		beans.put("one", new TestBean("one"));
		beans.put("two", new TestBean("two"));
		beans.put("three", new TestBean("three"));
		ModelMap model = new ModelMap();
		model.put("one", new TestBean("oneOld"));
		model.mergeAttributes(beans);
		assertThat(model).hasSize(3);
		assertThat(((TestBean) model.get("one")).getName()).isEqualTo("oneOld");
	}

	@Test
	public void testInnerClass() throws Exception {
		ModelMap map = new ModelMap();
		SomeInnerClass inner = new SomeInnerClass();
		map.addAttribute(inner);
		assertThat(map.get("someInnerClass")).isSameAs(inner);
	}

	@Test
	public void testInnerClassWithTwoUpperCaseLetters() throws Exception {
		ModelMap map = new ModelMap();
		UKInnerClass inner = new UKInnerClass();
		map.addAttribute(inner);
		assertThat(map.get("UKInnerClass")).isSameAs(inner);
	}

	@Test
	public void testAopCglibProxy() throws Exception {
		ModelMap map = new ModelMap();
		ProxyFactory factory = new ProxyFactory();
		SomeInnerClass val = new SomeInnerClass();
		factory.setTarget(val);
		factory.setProxyTargetClass(true);
		map.addAttribute(factory.getProxy());
		assertThat(map.containsKey("someInnerClass")).isTrue();
		assertThat(val).isEqualTo(map.get("someInnerClass"));
	}

	@Test
	public void testAopJdkProxy() throws Exception {
		ModelMap map = new ModelMap();
		ProxyFactory factory = new ProxyFactory();
		Map<?, ?> target = new HashMap<>();
		factory.setTarget(target);
		factory.addInterface(Map.class);
		Object proxy = factory.getProxy();
		map.addAttribute(proxy);
		assertThat(map.get("map")).isSameAs(proxy);
	}

	@Test
	public void testAopJdkProxyWithMultipleInterfaces() throws Exception {
		ModelMap map = new ModelMap();
		Map<?, ?> target = new HashMap<>();
		ProxyFactory factory = new ProxyFactory();
		factory.setTarget(target);
		factory.addInterface(Serializable.class);
		factory.addInterface(Cloneable.class);
		factory.addInterface(Comparable.class);
		factory.addInterface(Map.class);
		Object proxy = factory.getProxy();
		map.addAttribute(proxy);
		assertThat(map.get("map")).isSameAs(proxy);
	}

	@Test
	public void testAopJdkProxyWithDetectedInterfaces() throws Exception {
		ModelMap map = new ModelMap();
		Map<?, ?> target = new HashMap<>();
		ProxyFactory factory = new ProxyFactory(target);
		Object proxy = factory.getProxy();
		map.addAttribute(proxy);
		assertThat(map.get("map")).isSameAs(proxy);
	}

	@Test
	public void testRawJdkProxy() throws Exception {
		ModelMap map = new ModelMap();
		Object proxy = Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class<?>[] {Map.class},
				(proxy1, method, args) -> "proxy");
		map.addAttribute(proxy);
		assertThat(map.get("map")).isSameAs(proxy);
	}


	public static class SomeInnerClass {

		@Override
		public boolean equals(@Nullable Object obj) {
			return (obj instanceof SomeInnerClass);
		}

		@Override
		public int hashCode() {
			return SomeInnerClass.class.hashCode();
		}
	}


	public static class UKInnerClass {
	}

}
