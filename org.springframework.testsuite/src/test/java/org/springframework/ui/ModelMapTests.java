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

package org.springframework.ui;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.TestBean;
import org.springframework.test.AssertThrows;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public final class ModelMapTests extends TestCase {

	public void testNoArgCtorYieldsEmptyModel() throws Exception {
		assertEquals(0, new ModelMap().size());
	}

	/*
	 * SPR-2185 - Null model assertion causes backwards compatibility issue
	 */
	public void testAddNullObjectWithExplicitKey() throws Exception {
		ModelMap model = new ModelMap();
		model.addAttribute("foo", null);
		assertTrue(model.containsKey("foo"));
		assertNull(model.get("foo"));
	}

	/*
	 * SPR-2185 - Null model assertion causes backwards compatibility issue
	 */
	public void testAddNullObjectViaCtorWithExplicitKey() throws Exception {
		ModelMap model = new ModelMap("foo", null);
		assertTrue(model.containsKey("foo"));
		assertNull(model.get("foo"));
	}

	public void testNamedObjectCtor() throws Exception {
		ModelMap model = new ModelMap("foo", "bing");
		assertEquals(1, model.size());
		String bing = (String) model.get("foo");
		assertNotNull(bing);
		assertEquals("bing", bing);
	}

	public void testUnnamedCtorScalar() throws Exception {
		ModelMap model = new ModelMap("foo", "bing");
		assertEquals(1, model.size());
		String bing = (String) model.get("foo");
		assertNotNull(bing);
		assertEquals("bing", bing);
	}

	public void testOneArgCtorWithScalar() throws Exception {
		ModelMap model = new ModelMap("bing");
		assertEquals(1, model.size());
		String string = (String) model.get("string");
		assertNotNull(string);
		assertEquals("bing", string);
	}

	public void testOneArgCtorWithNull() throws Exception {
		new AssertThrows(IllegalArgumentException.class, "Null model arguments added without a name being explicitly supplied are not allowed.") {
			public void test() throws Exception {
				new ModelMap(null);
			}
		}.runTest();
	}

	public void testOneArgCtorWithCollection() throws Exception {
		ModelMap model = new ModelMap(new String[]{"foo", "boing"});
		assertEquals(1, model.size());
		String[] strings = (String[]) model.get("stringList");
		assertNotNull(strings);
		assertEquals(2, strings.length);
		assertEquals("foo", strings[0]);
		assertEquals("boing", strings[1]);
	}

	public void testOneArgCtorWithEmptyCollection() throws Exception {
		ModelMap model = new ModelMap(new HashSet());
		// must not add if collection is empty...
		assertEquals(0, model.size());
	}

	public void testAddObjectWithNull() throws Exception {
		new AssertThrows(IllegalArgumentException.class, "Null model arguments added without a name being explicitly supplied are not allowed.") {
			public void test() throws Exception {
				ModelMap model = new ModelMap();
				model.addAttribute(null);
			}
		}.runTest();
	}

	public void testAddObjectWithEmptyArray() throws Exception {
		ModelMap model = new ModelMap(new int[]{});
		assertEquals(1, model.size());
		int[] ints = (int[]) model.get("intList");
		assertNotNull(ints);
		assertEquals(0, ints.length);
	}

	public void testAddAllObjectsWithNullMap() throws Exception {
		ModelMap model = new ModelMap();
		model.addAllAttributes((Map) null);
		assertEquals(0, model.size());
	}

	public void testAddAllObjectsWithNullCollection() throws Exception {
		ModelMap model = new ModelMap();
		model.addAllAttributes((Collection) null);
		assertEquals(0, model.size());
	}

	public void testAddAllObjectsWithSparseArrayList() throws Exception {
		new AssertThrows(IllegalArgumentException.class, "Null model arguments added without a name being explicitly supplied are not allowed.") {
			public void test() throws Exception {
				ModelMap model = new ModelMap();
				ArrayList list = new ArrayList();
				list.add("bing");
				list.add(null);
				model.addAllAttributes(list);
			}
		}.runTest();
	}

	public void testAddMap() throws Exception {
		Map map = new HashMap();
		map.put("one", "one-value");
		map.put("two", "two-value");
		ModelMap model = new ModelMap();
		model.addAttribute(map);
		assertEquals(1, model.size());
		String key = StringUtils.uncapitalize(ClassUtils.getShortName(map.getClass()));
		assertTrue(model.containsKey(key));
	}

	public void testAddObjectNoKeyOfSameTypeOverrides() throws Exception {
		ModelMap model = new ModelMap();
		model.addAttribute("foo");
		model.addAttribute("bar");
		assertEquals(1, model.size());
		String bar = (String) model.get("string");
		assertEquals("bar", bar);
	}

	public void testAddListOfTheSameObjects() throws Exception {
		List beans = new ArrayList();
		beans.add(new TestBean("one"));
		beans.add(new TestBean("two"));
		beans.add(new TestBean("three"));
		ModelMap model = new ModelMap();
		model.addAllAttributes(beans);
		assertEquals(1, model.size());
	}

	public void testMergeMapWithOverriding() throws Exception {
		Map beans = new HashMap();
		beans.put("one", new TestBean("one"));
		beans.put("two", new TestBean("two"));
		beans.put("three", new TestBean("three"));
		ModelMap model = new ModelMap();
		model.put("one", new TestBean("oneOld"));
		model.mergeAttributes(beans);
		assertEquals(3, model.size());
		assertEquals("oneOld", ((TestBean) model.get("one")).getName());
	}

	public void testInnerClass() throws Exception {
		ModelMap map = new ModelMap();
		SomeInnerClass inner = new SomeInnerClass();
		map.addAttribute(inner);
		assertSame(inner, map.get("someInnerClass"));
	}

	public void testInnerClassWithTwoUpperCaseLetters() throws Exception {
		ModelMap map = new ModelMap();
		UKInnerClass inner = new UKInnerClass();
		map.addAttribute(inner);
		assertSame(inner, map.get("UKInnerClass"));
	}

	public void testAopCglibProxy() throws Exception {
		ModelMap map = new ModelMap();
		ProxyFactory factory = new ProxyFactory();
		Date date = new Date();
		factory.setTarget(date);
		factory.setProxyTargetClass(true);
		map.addAttribute(factory.getProxy());
		assertTrue(map.containsKey("date"));
		assertEquals(date, map.get("date"));
	}

	public void testAopJdkProxy() throws Exception {
		ModelMap map = new ModelMap();
		ProxyFactory factory = new ProxyFactory();
		Map target = new HashMap();
		factory.setTarget(target);
		factory.addInterface(Map.class);
		Object proxy = factory.getProxy();
		map.addAttribute(proxy);
		assertSame(proxy, map.get("map"));
	}

	public void testAopJdkProxyWithMultipleInterfaces() throws Exception {
		ModelMap map = new ModelMap();
		Map target = new HashMap();
		ProxyFactory factory = new ProxyFactory();
		factory.setTarget(target);
		factory.addInterface(Serializable.class);
		factory.addInterface(Cloneable.class);
		factory.addInterface(Comparable.class);
		factory.addInterface(Map.class);
		Object proxy = factory.getProxy();
		map.addAttribute(proxy);
		assertSame(proxy, map.get("map"));
	}

	public void testAopJdkProxyWithDetectedInterfaces() throws Exception {
		ModelMap map = new ModelMap();
		Map target = new HashMap();
		ProxyFactory factory = new ProxyFactory(target);
		Object proxy = factory.getProxy();
		map.addAttribute(proxy);
		assertSame(proxy, map.get("map"));
	}

	public void testRawJdkProxy() throws Exception {
		ModelMap map = new ModelMap();
		Object proxy = Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class[] {Map.class},
				new InvocationHandler() {
					public Object invoke(Object proxy, Method method, Object[] args) {
						return "proxy";
					}
				});
		map.addAttribute(proxy);
		assertSame(proxy, map.get("map"));
	}


	private static class SomeInnerClass {
	}


	private static class UKInnerClass {
	}

}
