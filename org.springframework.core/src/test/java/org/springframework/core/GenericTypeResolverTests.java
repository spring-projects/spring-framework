/*
 * Copyright 2002-2011 the original author or authors.
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

import java.util.Collection;

import org.junit.Test;

import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 */
public class GenericTypeResolverTests {

	@Test
	public void testSimpleInterfaceType() {
		assertEquals(String.class, GenericTypeResolver.resolveTypeArgument(MySimpleInterfaceType.class, MyInterfaceType.class));
	}

	@Test
	public void testSimpleCollectionInterfaceType() {
		assertEquals(Collection.class, GenericTypeResolver.resolveTypeArgument(MyCollectionInterfaceType.class, MyInterfaceType.class));
	}

	@Test
	public void testSimpleSuperclassType() {
		assertEquals(String.class, GenericTypeResolver.resolveTypeArgument(MySimpleSuperclassType.class, MySuperclassType.class));
	}

	@Test
	public void testSimpleCollectionSuperclassType() {
		assertEquals(Collection.class, GenericTypeResolver.resolveTypeArgument(MyCollectionSuperclassType.class, MySuperclassType.class));
	}

	@Test
	public void testMethodReturnType() {
		assertEquals(Integer.class, GenericTypeResolver.resolveReturnTypeArgument(ReflectionUtils.findMethod(MyTypeWithMethods.class, "integer"), MyInterfaceType.class));
		assertEquals(String.class, GenericTypeResolver.resolveReturnTypeArgument(ReflectionUtils.findMethod(MyTypeWithMethods.class, "string"), MyInterfaceType.class));
		assertEquals(null, GenericTypeResolver.resolveReturnTypeArgument(ReflectionUtils.findMethod(MyTypeWithMethods.class, "raw"), MyInterfaceType.class));
		assertEquals(null, GenericTypeResolver.resolveReturnTypeArgument(ReflectionUtils.findMethod(MyTypeWithMethods.class, "object"), MyInterfaceType.class));
	}

	@Test
	public void testNullIfNotResolvable() {
		GenericClass<String> obj = new GenericClass<String>();
		assertNull(GenericTypeResolver.resolveTypeArgument(obj.getClass(), GenericClass.class));
	}


	public interface MyInterfaceType<T> {
	}

	public class MySimpleInterfaceType implements MyInterfaceType<String> {
	}

	public class MyCollectionInterfaceType implements MyInterfaceType<Collection<String>> {
	}


	public abstract class MySuperclassType<T> {
	}

	public class MySimpleSuperclassType extends MySuperclassType<String> {
	}

	public class MyCollectionSuperclassType extends MySuperclassType<Collection<String>> {
	}

	public class MyTypeWithMethods {
		public MyInterfaceType<Integer> integer() { return null; }
		public MySimpleInterfaceType string() { return null; }
		public Object object() { return null; }
		@SuppressWarnings("rawtypes")
		public MyInterfaceType raw() { return null; }
	}

	static class GenericClass<T> {
	}

}
