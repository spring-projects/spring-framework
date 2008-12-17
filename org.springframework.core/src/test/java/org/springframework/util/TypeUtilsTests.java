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

package org.springframework.util;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author Juergen Hoeller
 */
public class TypeUtilsTests extends TestCase {

	public static List<Object> objects;

	public static List<? extends Object> openObjects;

	public static List<String> strings;

	public void testClasses() {
		assertTrue(TypeUtils.isAssignable(Object.class, Object.class));
		assertTrue(TypeUtils.isAssignable(Object.class, String.class));
		assertFalse(TypeUtils.isAssignable(String.class, Object.class));
		assertTrue(TypeUtils.isAssignable(List.class, List.class));
		assertTrue(TypeUtils.isAssignable(List.class, LinkedList.class));
		assertFalse(TypeUtils.isAssignable(List.class, Collection.class));
		assertFalse(TypeUtils.isAssignable(List.class, HashSet.class));
	}

	public void testParameterizedTypes() throws Exception {
		Type objectsType = getClass().getField("objects").getGenericType();
		Type openObjectsType = getClass().getField("openObjects").getGenericType();
		Type stringsType = getClass().getField("strings").getGenericType();
		assertTrue(TypeUtils.isAssignable(objectsType, objectsType));
		assertTrue(TypeUtils.isAssignable(openObjectsType, openObjectsType));
		assertTrue(TypeUtils.isAssignable(stringsType, stringsType));
		assertTrue(TypeUtils.isAssignable(openObjectsType, objectsType));
		assertTrue(TypeUtils.isAssignable(openObjectsType, stringsType));
		assertFalse(TypeUtils.isAssignable(stringsType, objectsType));
		assertFalse(TypeUtils.isAssignable(objectsType, stringsType));
	}

}
