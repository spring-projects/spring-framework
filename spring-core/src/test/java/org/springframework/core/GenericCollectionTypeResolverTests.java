/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.core;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.Resource;
import org.springframework.tests.sample.objects.GenericObject;

import static org.junit.Assert.*;

/**
 * @author Serge Bogatyrjov
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class GenericCollectionTypeResolverTests {

	protected Class<?> targetClass;

	protected String[] methods;

	protected Type[] expectedResults;

	@Before
	public void setUp() throws Exception {
		this.targetClass = Foo.class;
		this.methods = new String[] { "a", "b", "b2", "b3", "c", "d", "d2", "d3", "e",
			"e2", "e3" };
		this.expectedResults = new Class[] { Integer.class, null, Set.class, Set.class,
			null, Integer.class, Integer.class, Integer.class, Integer.class,
			Integer.class, Integer.class };
	}

	protected void executeTest(String methodName) throws NoSuchMethodException {
		for (int i = 0; i < this.methods.length; i++) {
			if (methodName.equals(this.methods[i])) {
				Method method = this.targetClass.getMethod(methodName);
				Type type = getType(method);
				assertEquals(this.expectedResults[i], type);
				return;
			}
		}
		throw new IllegalStateException("Bad test data");
	}

	protected Type getType(Method method) {
		return GenericCollectionTypeResolver.getMapValueReturnType(method);
	}

	@Test
	public void a() throws Exception {
		executeTest("a");
	}

	@Test
	public void b() throws Exception {
		executeTest("b");
	}

	@Test
	public void b2() throws Exception {
		executeTest("b2");
	}

	@Test
	public void b3() throws Exception {
		executeTest("b3");
	}

	@Test
	public void c() throws Exception {
		executeTest("c");
	}

	@Test
	public void d() throws Exception {
		executeTest("d");
	}

	@Test
	public void d2() throws Exception {
		executeTest("d2");
	}

	@Test
	public void d3() throws Exception {
		executeTest("d3");
	}

	@Test
	public void e() throws Exception {
		executeTest("e");
	}

	@Test
	public void e2() throws Exception {
		executeTest("e2");
	}

	@Test
	public void e3() throws Exception {
		executeTest("e3");
	}

	@Test
	public void programmaticListIntrospection() throws Exception {
		Method setter = GenericObject.class.getMethod("setResourceList", List.class);
		assertEquals(
				Resource.class,
				GenericCollectionTypeResolver.getCollectionParameterType(new MethodParameter(
						setter, 0)));

		Method getter = GenericObject.class.getMethod("getResourceList");
		assertEquals(Resource.class,
				GenericCollectionTypeResolver.getCollectionReturnType(getter));
	}

	@Test
	public void classResolution() {
		assertEquals(String.class,
				GenericCollectionTypeResolver.getCollectionType(CustomSet.class));
		assertEquals(String.class,
				GenericCollectionTypeResolver.getMapKeyType(CustomMap.class));
		assertEquals(Integer.class,
				GenericCollectionTypeResolver.getMapValueType(CustomMap.class));
	}

	private static abstract class CustomSet<T> extends AbstractSet<String> {
	}

	private static abstract class CustomMap<T> extends AbstractMap<String, Integer> {
	}

	private static abstract class OtherCustomMap<T> implements Map<String, Integer> {
	}

	@SuppressWarnings("rawtypes")
	private static interface Foo {

		Map<String, Integer> a();

		Map<?, ?> b();

		Map<?, ? extends Set> b2();

		Map<?, ? super Set> b3();

		Map c();

		CustomMap<Date> d();

		CustomMap<?> d2();

		CustomMap d3();

		OtherCustomMap<Date> e();

		OtherCustomMap<?> e2();

		OtherCustomMap e3();
	}

}
