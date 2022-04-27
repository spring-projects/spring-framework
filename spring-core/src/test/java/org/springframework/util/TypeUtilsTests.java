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

package org.springframework.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TypeUtils}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class TypeUtilsTests {

	public static Object object;

	public static String string;

	public static Integer number;

	public static List<Object> objects;

	public static List<String> strings;

	public static List<? extends Object> openObjects;

	public static List<? extends Number> openNumbers;

	public static List<? super Object> storableObjectList;

	public static List<Number>[] array;

	public static List<? extends Number>[] openArray;


	@Test
	void withClasses() {
		assertThat(TypeUtils.isAssignable(Object.class, Object.class)).isTrue();
		assertThat(TypeUtils.isAssignable(Object.class, String.class)).isTrue();
		assertThat(TypeUtils.isAssignable(String.class, Object.class)).isFalse();
		assertThat(TypeUtils.isAssignable(List.class, List.class)).isTrue();
		assertThat(TypeUtils.isAssignable(List.class, LinkedList.class)).isTrue();
		assertThat(TypeUtils.isAssignable(List.class, Collection.class)).isFalse();
		assertThat(TypeUtils.isAssignable(List.class, HashSet.class)).isFalse();
	}

	@Test
	void withParameterizedTypes() throws Exception {
		Type objectsType = getClass().getField("objects").getGenericType();
		Type openObjectsType = getClass().getField("openObjects").getGenericType();
		Type stringsType = getClass().getField("strings").getGenericType();
		assertThat(TypeUtils.isAssignable(Object.class, objectsType)).isTrue();
		assertThat(TypeUtils.isAssignable(Object.class, openObjectsType)).isTrue();
		assertThat(TypeUtils.isAssignable(Object.class, stringsType)).isTrue();
		assertThat(TypeUtils.isAssignable(List.class, objectsType)).isTrue();
		assertThat(TypeUtils.isAssignable(List.class, openObjectsType)).isTrue();
		assertThat(TypeUtils.isAssignable(List.class, stringsType)).isTrue();
		assertThat(TypeUtils.isAssignable(objectsType, List.class)).isTrue();
		assertThat(TypeUtils.isAssignable(openObjectsType, List.class)).isTrue();
		assertThat(TypeUtils.isAssignable(stringsType, List.class)).isTrue();
		assertThat(TypeUtils.isAssignable(objectsType, objectsType)).isTrue();
		assertThat(TypeUtils.isAssignable(openObjectsType, openObjectsType)).isTrue();
		assertThat(TypeUtils.isAssignable(stringsType, stringsType)).isTrue();
		assertThat(TypeUtils.isAssignable(openObjectsType, objectsType)).isTrue();
		assertThat(TypeUtils.isAssignable(openObjectsType, stringsType)).isTrue();
		assertThat(TypeUtils.isAssignable(stringsType, objectsType)).isFalse();
		assertThat(TypeUtils.isAssignable(objectsType, stringsType)).isFalse();
	}

	@Test
	void withWildcardTypes() throws Exception {
		ParameterizedType openObjectsType = (ParameterizedType) getClass().getField("openObjects").getGenericType();
		ParameterizedType openNumbersType = (ParameterizedType) getClass().getField("openNumbers").getGenericType();
		Type storableObjectListType = getClass().getField("storableObjectList").getGenericType();

		Type objectType = getClass().getField("object").getGenericType();
		Type numberType = getClass().getField("number").getGenericType();
		Type stringType = getClass().getField("string").getGenericType();

		Type openWildcard = openObjectsType.getActualTypeArguments()[0]; // '?'
		Type openNumbersWildcard = openNumbersType.getActualTypeArguments()[0]; // '? extends number'

		assertThat(TypeUtils.isAssignable(openWildcard, objectType)).isTrue();
		assertThat(TypeUtils.isAssignable(openNumbersWildcard, numberType)).isTrue();
		assertThat(TypeUtils.isAssignable(openNumbersWildcard, stringType)).isFalse();
		assertThat(TypeUtils.isAssignable(storableObjectListType, openObjectsType)).isFalse();
	}

	@Test
	void withGenericArrayTypes() throws Exception {
		Type arrayType = getClass().getField("array").getGenericType();
		Type openArrayType = getClass().getField("openArray").getGenericType();
		assertThat(TypeUtils.isAssignable(Object.class, arrayType)).isTrue();
		assertThat(TypeUtils.isAssignable(Object.class, openArrayType)).isTrue();
		assertThat(TypeUtils.isAssignable(List[].class, arrayType)).isTrue();
		assertThat(TypeUtils.isAssignable(List[].class, openArrayType)).isTrue();
		assertThat(TypeUtils.isAssignable(arrayType, List[].class)).isTrue();
		assertThat(TypeUtils.isAssignable(openArrayType, List[].class)).isTrue();
		assertThat(TypeUtils.isAssignable(arrayType, arrayType)).isTrue();
		assertThat(TypeUtils.isAssignable(openArrayType, openArrayType)).isTrue();
		assertThat(TypeUtils.isAssignable(openArrayType, arrayType)).isTrue();
	}

}
