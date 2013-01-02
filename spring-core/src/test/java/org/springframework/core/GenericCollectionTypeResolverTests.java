/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.core.io.Resource;
import org.springframework.tests.sample.objects.GenericObject;

/**
 * @author Serge Bogatyrjov
 * @author Juergen Hoeller
 */
public class GenericCollectionTypeResolverTests extends AbstractGenericsTests {

	@Override
	protected void setUp() throws Exception {
		this.targetClass = Foo.class;
		this.methods = new String[] {"a", "b", "b2", "b3", "c", "d", "d2", "d3", "e", "e2", "e3"};
		this.expectedResults = new Class[] {
			Integer.class, null, Set.class, Set.class, null, Integer.class,
			Integer.class, Integer.class, Integer.class, Integer.class, Integer.class};
	}

	@Override
	protected Type getType(Method method) {
		return GenericCollectionTypeResolver.getMapValueReturnType(method);
	}

	public void testA() throws Exception {
		executeTest();
	}

	public void testB() throws Exception {
		executeTest();
	}

	public void testB2() throws Exception {
		executeTest();
	}

	public void testB3() throws Exception {
		executeTest();
	}

	public void testC() throws Exception {
		executeTest();
	}

	public void testD() throws Exception {
		executeTest();
	}

	public void testD2() throws Exception {
		executeTest();
	}

	public void testD3() throws Exception {
		executeTest();
	}

	public void testE() throws Exception {
		executeTest();
	}

	public void testE2() throws Exception {
		executeTest();
	}

	public void testE3() throws Exception {
		executeTest();
	}

	public void testProgrammaticListIntrospection() throws Exception {
		Method setter = GenericObject.class.getMethod("setResourceList", List.class);
		assertEquals(Resource.class,
				GenericCollectionTypeResolver.getCollectionParameterType(new MethodParameter(setter, 0)));

		Method getter = GenericObject.class.getMethod("getResourceList");
		assertEquals(Resource.class,
				GenericCollectionTypeResolver.getCollectionReturnType(getter));
	}

	public void testClassResolution() {
		assertEquals(String.class, GenericCollectionTypeResolver.getCollectionType(CustomSet.class));
		assertEquals(String.class, GenericCollectionTypeResolver.getMapKeyType(CustomMap.class));
		assertEquals(Integer.class, GenericCollectionTypeResolver.getMapValueType(CustomMap.class));
	}


	private abstract class CustomSet<T> extends AbstractSet<String> {
	}


	private abstract class CustomMap<T> extends AbstractMap<String, Integer> {
	}


	private abstract class OtherCustomMap<T> implements Map<String, Integer> {
	}


	private interface Foo {

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
