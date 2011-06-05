/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.core.convert;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor.Property;

/**
 * @author Keith Donald
 * @author Andy Clement
 */
public class TypeDescriptorTests {

	public List<String> listOfString;

	public List<List<String>> listOfListOfString = new ArrayList<List<String>>();

	public List<List> listOfListOfUnknown = new ArrayList<List>();

	public int[] intArray;

	public List<String>[] arrayOfListOfString;

	public List<Integer> listField = new ArrayList<Integer>();

	public Map<String, Integer> mapField = new HashMap<String, Integer>();

	public Map<String, List<Integer>> nestedMapField = new HashMap<String, List<Integer>>();

	@Test
	public void parameterPrimitive() throws Exception {
		TypeDescriptor desc = new TypeDescriptor(new MethodParameter(getClass().getMethod("testParameterPrimitive", int.class), 0));
		assertEquals(int.class, desc.getType());
		assertEquals(Integer.class, desc.getObjectType());
		assertEquals("int", desc.getName());
		assertEquals("int", desc.toString());
		assertTrue(desc.isPrimitive());
		assertEquals(0, desc.getAnnotations().length);
		assertFalse(desc.isCollection());
		assertFalse(desc.isMap());
	}
	
	public void testParameterPrimitive(int primitive) {
		
	}
	
	@Test
	public void parameterScalar() throws Exception {
		TypeDescriptor desc = new TypeDescriptor(new MethodParameter(getClass().getMethod("testParameterScalar", String.class), 0));
		assertEquals(String.class, desc.getType());
		assertEquals(String.class, desc.getObjectType());
		assertEquals("java.lang.String", desc.getName());
		assertEquals("java.lang.String", desc.toString());
		assertTrue(!desc.isPrimitive());
		assertEquals(0, desc.getAnnotations().length);
		assertFalse(desc.isCollection());
		assertFalse(desc.isArray());
		assertFalse(desc.isMap());
	}
	
	public void testParameterScalar(String value) {
		
	}

	@Test
	public void parameterList() throws Exception {
		MethodParameter methodParameter = new MethodParameter(getClass().getMethod("testParameterList", List.class), 0);
		TypeDescriptor desc = new TypeDescriptor(methodParameter);
		assertEquals(List.class, desc.getType());
		assertEquals(List.class, desc.getObjectType());
		assertEquals("java.util.List", desc.getName());
		assertEquals("java.util.List<java.util.List<java.util.Map<java.lang.Integer, java.lang.Enum>>>", desc.toString());
		assertTrue(!desc.isPrimitive());
		assertEquals(0, desc.getAnnotations().length);
		assertTrue(desc.isCollection());
		assertFalse(desc.isArray());
		assertEquals(List.class, desc.getElementType().getType());
		assertEquals(TypeDescriptor.nested(methodParameter, 1), desc.getElementType());
		assertEquals(TypeDescriptor.nested(methodParameter, 2), desc.getElementType().getElementType());
		assertEquals(TypeDescriptor.nested(methodParameter, 3), desc.getElementType().getElementType().getMapValueType());
		assertEquals(Integer.class, desc.getElementType().getElementType().getMapKeyType().getType());
		assertEquals(Enum.class, desc.getElementType().getElementType().getMapValueType().getType());
		assertFalse(desc.isMap());
	}

	public void testParameterList(List<List<Map<Integer, Enum<?>>>> list) {
		
	}

	@Test
	public void parameterListNoParamTypes() throws Exception {
		MethodParameter methodParameter = new MethodParameter(getClass().getMethod("testParameterListNoParamTypes", List.class), 0);
		TypeDescriptor desc = new TypeDescriptor(methodParameter);
		assertEquals(List.class, desc.getType());
		assertEquals(List.class, desc.getObjectType());
		assertEquals("java.util.List", desc.getName());
		assertEquals("java.util.List<?>", desc.toString());
		assertTrue(!desc.isPrimitive());
		assertEquals(0, desc.getAnnotations().length);
		assertTrue(desc.isCollection());
		assertFalse(desc.isArray());		
		assertNull(desc.getElementType());
		assertFalse(desc.isMap());
	}

	public void testParameterListNoParamTypes(List list) {
		
	}

	@Test
	public void parameterArray() throws Exception {
		MethodParameter methodParameter = new MethodParameter(getClass().getMethod("testParameterArray", Integer[].class), 0);
		TypeDescriptor desc = new TypeDescriptor(methodParameter);
		assertEquals(Integer[].class, desc.getType());
		assertEquals(Integer[].class, desc.getObjectType());
		assertEquals("java.lang.Integer[]", desc.getName());
		assertEquals("java.lang.Integer[]", desc.toString());
		assertTrue(!desc.isPrimitive());
		assertEquals(0, desc.getAnnotations().length);
		assertFalse(desc.isCollection());
		assertTrue(desc.isArray());
		assertEquals(Integer.class, desc.getElementType().getType());
		assertEquals(TypeDescriptor.valueOf(Integer.class), desc.getElementType());
		assertFalse(desc.isMap());
	}

	public void testParameterArray(Integer[] array) {
		
	}
	
	@Test
	public void parameterMap() throws Exception {
		MethodParameter methodParameter = new MethodParameter(getClass().getMethod("testParameterMap", Map.class), 0);
		TypeDescriptor desc = new TypeDescriptor(methodParameter);
		assertEquals(Map.class, desc.getType());
		assertEquals(Map.class, desc.getObjectType());
		assertEquals("java.util.Map", desc.getName());
		assertEquals("java.util.Map<java.lang.Integer, java.util.List<java.lang.String>>", desc.toString());
		assertTrue(!desc.isPrimitive());
		assertEquals(0, desc.getAnnotations().length);
		assertFalse(desc.isCollection());
		assertFalse(desc.isArray());
		assertTrue(desc.isMap());
		assertEquals(TypeDescriptor.nested(methodParameter, 1), desc.getMapValueType());
		assertEquals(TypeDescriptor.nested(methodParameter, 2), desc.getMapValueType().getElementType());
		assertEquals(Integer.class, desc.getMapKeyType().getType());
		assertEquals(List.class, desc.getMapValueType().getType());
		assertEquals(String.class, desc.getMapValueType().getElementType().getType());
	}

	public void testParameterMap(Map<Integer, List<String>> map) {
		
	}

	@Test
	public void parameterAnnotated() throws Exception {
		TypeDescriptor t1 = new TypeDescriptor(new MethodParameter(getClass().getMethod("testAnnotatedMethod", String.class), 0));
		assertEquals(String.class, t1.getType());
		assertEquals(1, t1.getAnnotations().length);
		assertNotNull(t1.getAnnotation(ParameterAnnotation.class));
	}

	@Target({ElementType.PARAMETER})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ParameterAnnotation {
		
	}
	
	public void testAnnotatedMethod(@ParameterAnnotation String parameter) {
		
	}

	@Test
	public void propertyComplex() throws Exception {
		Property property = new Property(getClass(), getClass().getMethod("getComplexProperty", null), getClass().getMethod("setComplexProperty", Map.class));		
		TypeDescriptor desc = new TypeDescriptor(property);
		assertEquals(String.class, desc.getMapKeyType().getType());
		assertEquals(Integer.class, desc.getMapValueType().getElementType().getElementType().getType());
	}
	
	public Map<String, List<List<Integer>>> getComplexProperty() {
		return null;
	}
	
	public void setComplexProperty(Map<String, List<List<Integer>>> complexProperty) {
		
	}

	@Test
	public void propertyGenericType() throws Exception {
		GenericType<Integer> genericBean = new IntegerType();
		Property property = new Property(getClass(), genericBean.getClass().getMethod("getProperty", null), genericBean.getClass().getMethod("setProperty", Integer.class));		
		TypeDescriptor desc = new TypeDescriptor(property);
		assertEquals(Integer.class, desc.getType());
	}

	@Test
	public void propertyGenericTypeList() throws Exception {
		GenericType<Integer> genericBean = new IntegerType();
		Property property = new Property(getClass(), genericBean.getClass().getMethod("getListProperty", null), genericBean.getClass().getMethod("setListProperty", List.class));		
		TypeDescriptor desc = new TypeDescriptor(property);
		assertEquals(List.class, desc.getType());
		assertEquals(Integer.class, desc.getElementType().getType());
	}

	public interface GenericType<T> {
		T getProperty();
	
		void setProperty(T t);
		
		List<T> getListProperty();
		
		void setListProperty(List<T> t);
		
 	}
	
	public class IntegerType implements GenericType<Integer> {

		public Integer getProperty() {
			// TODO Auto-generated method stub
			return null;
		}

		public void setProperty(Integer t) {
			// TODO Auto-generated method stub
			
		}

		public List<Integer> getListProperty() {
			// TODO Auto-generated method stub
			return null;
		}

		public void setListProperty(List<Integer> t) {
			// TODO Auto-generated method stub
			
		}
		
	}

	@Test
	public void propertyGenericClassList() throws Exception {
		IntegerClass genericBean = new IntegerClass();
		Property property = new Property(genericBean.getClass(), genericBean.getClass().getMethod("getListProperty", null), genericBean.getClass().getMethod("setListProperty", List.class));		
		TypeDescriptor desc = new TypeDescriptor(property);
		assertEquals(List.class, desc.getType());
		assertEquals(Integer.class, desc.getElementType().getType());
		assertNotNull(desc.getAnnotation(MethodAnnotation1.class));
	}
	
	public static class GenericClass<T> {
		
		public T getProperty() {
			return null;
		}
		
		public void setProperty(T t) {
		}

		@MethodAnnotation1
		public List<T> getListProperty() {
			return null;
		}
		
		public void setListProperty(List<T> t) {
		}
		
	}
	
	public static class IntegerClass extends GenericClass<Integer> {
		
	}

	@Test
	public void property() throws Exception {
		Property property = new Property(getClass(), getClass().getMethod("getProperty", null), getClass().getMethod("setProperty", Map.class));		
		TypeDescriptor desc = new TypeDescriptor(property);
		assertEquals(Map.class, desc.getType());
		assertEquals(Integer.class, desc.getMapKeyType().getElementType().getType());
		assertEquals(Long.class, desc.getMapValueType().getElementType().getType());
		assertNotNull(desc.getAnnotation(MethodAnnotation1.class));
		assertNotNull(desc.getAnnotation(MethodAnnotation2.class));
		assertNotNull(desc.getAnnotation(MethodAnnotation3.class));
	}

	@MethodAnnotation1
	public Map<List<Integer>, List<Long>> getProperty() {
		return property;
	}
	
	@MethodAnnotation2
	public void setProperty(Map<List<Integer>, List<Long>> property) {
		this.property = property;
	}

	@MethodAnnotation3
	private Map<List<Integer>, List<Long>> property;
	
	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MethodAnnotation1 {
		
	}

	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MethodAnnotation2 {
		
	}

	@Target({ElementType.FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MethodAnnotation3 {
		
	}

	@Test
	public void fieldScalar() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(getClass().getField("fieldScalar"));
		assertFalse(typeDescriptor.isPrimitive());
		assertFalse(typeDescriptor.isArray());
		assertFalse(typeDescriptor.isCollection());
		assertFalse(typeDescriptor.isMap());
		assertEquals(Integer.class, typeDescriptor.getType());
		assertEquals(Integer.class, typeDescriptor.getObjectType());
	}
	
	public Integer fieldScalar;
	
	@Test
	public void fieldList() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("listOfString"));
		assertFalse(typeDescriptor.isArray());
		assertEquals(List.class, typeDescriptor.getType());
		assertEquals(String.class, typeDescriptor.getElementType().getType());
		assertEquals("java.util.List<java.lang.String>", typeDescriptor.toString());
	}

	@Test
	public void fieldListOfListOfString() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("listOfListOfString"));
		assertFalse(typeDescriptor.isArray());
		assertEquals(List.class, typeDescriptor.getType());
		assertEquals(List.class, typeDescriptor.getElementType().getType());
		assertEquals(String.class, typeDescriptor.getElementType().getElementType().getType());
		assertEquals("java.util.List<java.util.List<java.lang.String>>", typeDescriptor.toString());
	}

	@Test
	public void fieldListOfListUnknown() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("listOfListOfUnknown"));
		assertFalse(typeDescriptor.isArray());
		assertEquals(List.class, typeDescriptor.getType());
		assertEquals(List.class, typeDescriptor.getElementType().getType());
		assertNull(typeDescriptor.getElementType().getElementType());
		assertEquals("java.util.List<java.util.List<?>>", typeDescriptor.toString());
	}

	@Test
	public void fieldArray() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("intArray"));
		assertTrue(typeDescriptor.isArray());
		assertEquals(Integer.TYPE,typeDescriptor.getElementType().getType());
		assertEquals("int[]",typeDescriptor.toString());
	}

	@Test
	@Ignore
	public void fieldComplexTypeDescriptor() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("arrayOfListOfString"));
		assertTrue(typeDescriptor.isArray());
		assertEquals(List.class,typeDescriptor.getElementType());
		assertEquals(String.class, typeDescriptor.getElementType().getElementType());
		assertEquals("java.util.List[]",typeDescriptor.toString());
	}

	@Test
	public void fieldComplexTypeDescriptor2() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("nestedMapField"));
		assertTrue(typeDescriptor.isMap());
		assertEquals(String.class,typeDescriptor.getMapKeyType().getType());
		assertEquals(List.class, typeDescriptor.getMapValueType().getType());
		assertEquals(Integer.class, typeDescriptor.getMapValueType().getElementType().getType());
		assertEquals("java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>", typeDescriptor.toString());
	}

	@Test
	@Ignore
	public void fieldMap() throws Exception {
		// TODO: SPR-8394: typeIndex handling not currently supported by fields
		TypeDescriptor desc = new TypeDescriptor(getClass().getField("fieldMap"));
		assertTrue(desc.isMap());
		assertEquals(Integer.class, desc.getMapKeyType().getElementType());
		assertEquals(Long.class, desc.getMapValueType().getElementType());
	}
	
	public Map<List<Integer>, List<Long>> fieldMap;

	@Test
	public void fieldAnnotated() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(getClass().getField("fieldAnnotated"));
		assertEquals(1, typeDescriptor.getAnnotations().length);
		assertNotNull(typeDescriptor.getAnnotation(FieldAnnotation.class));
	}

	@FieldAnnotation
	public List<String> fieldAnnotated;

	@Target({ElementType.FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface FieldAnnotation {
		
	}

	@Test
	public void valueOfScalar() {
		TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(Integer.class);
		assertFalse(typeDescriptor.isPrimitive());
		assertFalse(typeDescriptor.isArray());
		assertFalse(typeDescriptor.isCollection());
		assertFalse(typeDescriptor.isMap());
		assertEquals(Integer.class, typeDescriptor.getType());
		assertEquals(Integer.class, typeDescriptor.getObjectType());
	}

	@Test
	public void valueOfPrimitive() {
		TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(int.class);
		assertTrue(typeDescriptor.isPrimitive());
		assertFalse(typeDescriptor.isArray());
		assertFalse(typeDescriptor.isCollection());
		assertFalse(typeDescriptor.isMap());
		assertEquals(Integer.TYPE, typeDescriptor.getType());
		assertEquals(Integer.class, typeDescriptor.getObjectType());
	}
	
	@Test
	public void valueOfArray() throws Exception {
		TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(int[].class);
		assertTrue(typeDescriptor.isArray());
		assertFalse(typeDescriptor.isCollection());
		assertFalse(typeDescriptor.isMap());
		assertEquals(Integer.TYPE, typeDescriptor.getElementType().getType());
	}

	@Test
	public void valueOfCollection() throws Exception {
		TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(Collection.class);
		assertTrue(typeDescriptor.isCollection());
		assertFalse(typeDescriptor.isArray());
		assertFalse(typeDescriptor.isMap());
		assertNull(typeDescriptor.getElementType());
	}

	@Test
	public void forObject() {
		TypeDescriptor desc = TypeDescriptor.forObject("3");
		assertEquals(String.class, desc.getType());
	}

	@Test
	public void forObjectNullTypeDescriptor() {
		TypeDescriptor desc = TypeDescriptor.forObject(null);
		assertNull(desc);
	}

	@Test
	public void nestedMethodParameterType2Levels() throws Exception {
		TypeDescriptor t1 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test2", List.class), 0), 2);
		assertEquals(String.class, t1.getType());
	}

	@Test
	public void nestedMethodParameterTypeMap() throws Exception {
		TypeDescriptor t1 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test3", Map.class), 0), 1);
		assertEquals(String.class, t1.getType());
	}

	@Test
	public void nestedMethodParameterTypeMapTwoLevels() throws Exception {
		TypeDescriptor t1 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test4", List.class), 0), 2);
		assertEquals(String.class, t1.getType());
	}

	@Test(expected=IllegalStateException.class)
	public void nestedTooManyLevels() throws Exception {
		TypeDescriptor t1 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test4", List.class), 0), 3);
		assertEquals(String.class, t1.getType());
	}

	@Test(expected=IllegalStateException.class)
	public void nestedMethodParameterTypeNotNestable() throws Exception {
		TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test5", String.class), 0), 2);
	}

	@Test(expected=IllegalArgumentException.class)
	public void nestedMethodParameterTypeInvalidNestingLevel() throws Exception {
		TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test5", String.class), 0, 2), 2);
	}

	public void test1(List<String> param1) {
		
	}

	public void test2(List<List<String>> param1) {
		
	}
	
	public void test3(Map<Integer, String> param1) {
		
	}

	public void test4(List<Map<Integer, String>> param1) {
		
	}

	public void test5(String param1) {
		
	}

	@Test
	public void nestedNotParameterized() throws Exception {
		TypeDescriptor t1 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test6", List.class), 0), 1);
		assertEquals(List.class,t1.getType());
		assertEquals("java.util.List<?>", t1.toString());
		TypeDescriptor t2 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test6", List.class), 0), 2);
		assertNull(t2);
	}

	public void test6(List<List> param1) {
		
	}

	@Test
	public void nestedFieldTypeMapTwoLevels() throws Exception {
		TypeDescriptor t1 = TypeDescriptor.nested(getClass().getField("test4"), 2);
		assertEquals(String.class, t1.getType());
	}

	public List<Map<Integer, String>> test4;

	@Test
	public void nestedPropertyTypeMapTwoLevels() throws Exception {
		Property property = new Property(getClass(), getClass().getMethod("getTest4", null), getClass().getMethod("setTest4", List.class));
		TypeDescriptor t1 = TypeDescriptor.nested(property, 2);
		assertEquals(String.class, t1.getType());
	}

	public List<Map<Integer, String>> getTest4() {
		return null;
	}
	
	public void setTest4(List<Map<Integer, String>> test4) {
		
	}

	@Test
	public void collection() {
		TypeDescriptor desc = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(Integer.class));
		assertEquals(List.class, desc.getType());
		assertEquals(List.class, desc.getObjectType());
		assertEquals("java.util.List", desc.getName());
		assertEquals("java.util.List<java.lang.Integer>", desc.toString());
		assertTrue(!desc.isPrimitive());
		assertEquals(0, desc.getAnnotations().length);
		assertTrue(desc.isCollection());
		assertFalse(desc.isArray());
		assertEquals(Integer.class, desc.getElementType().getType());
		assertEquals(TypeDescriptor.valueOf(Integer.class), desc.getElementType());
		assertFalse(desc.isMap());
	}

	@Test
	public void collectionNested() {
		TypeDescriptor desc = TypeDescriptor.collection(List.class, TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(Integer.class)));
		assertEquals(List.class, desc.getType());
		assertEquals(List.class, desc.getObjectType());
		assertEquals("java.util.List", desc.getName());
		assertEquals("java.util.List<java.util.List<java.lang.Integer>>", desc.toString());
		assertTrue(!desc.isPrimitive());
		assertEquals(0, desc.getAnnotations().length);
		assertTrue(desc.isCollection());
		assertFalse(desc.isArray());
		assertEquals(List.class, desc.getElementType().getType());
		assertEquals(TypeDescriptor.valueOf(Integer.class), desc.getElementType().getElementType());
		assertFalse(desc.isMap());
	}

	@Test
	public void map() {
		TypeDescriptor desc = TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class));
		assertEquals(Map.class, desc.getType());
		assertEquals(Map.class, desc.getObjectType());
		assertEquals("java.util.Map", desc.getName());
		assertEquals("java.util.Map<java.lang.String, java.lang.Integer>", desc.toString());
		assertTrue(!desc.isPrimitive());
		assertEquals(0, desc.getAnnotations().length);
		assertFalse(desc.isCollection());
		assertFalse(desc.isArray());
		assertTrue(desc.isMap());
		assertEquals(String.class, desc.getMapKeyType().getType());
		assertEquals(Integer.class, desc.getMapValueType().getType());
	}

	@Test
	public void mapNested() {
		TypeDescriptor desc = TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(String.class), 
				TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class)));
		assertEquals(Map.class, desc.getType());
		assertEquals(Map.class, desc.getObjectType());
		assertEquals("java.util.Map", desc.getName());
		assertEquals("java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.Integer>>", desc.toString());
		assertTrue(!desc.isPrimitive());
		assertEquals(0, desc.getAnnotations().length);
		assertFalse(desc.isCollection());
		assertFalse(desc.isArray());
		assertTrue(desc.isMap());
		assertEquals(String.class, desc.getMapKeyType().getType());
		assertEquals(String.class, desc.getMapValueType().getMapKeyType().getType());
		assertEquals(Integer.class, desc.getMapValueType().getMapValueType().getType());
	}

	@Test
	public void equals() throws Exception {
		TypeDescriptor t1 = TypeDescriptor.valueOf(String.class);
		TypeDescriptor t2 = TypeDescriptor.valueOf(String.class);
		TypeDescriptor t3 = TypeDescriptor.valueOf(Date.class);
		TypeDescriptor t4 = TypeDescriptor.valueOf(Date.class);
		TypeDescriptor t5 = TypeDescriptor.valueOf(List.class);
		TypeDescriptor t6 = TypeDescriptor.valueOf(List.class);
		TypeDescriptor t7 = TypeDescriptor.valueOf(Map.class);
		TypeDescriptor t8 = TypeDescriptor.valueOf(Map.class);
		assertEquals(t1, t2);
		assertEquals(t3, t4);
		assertEquals(t5, t6);
		assertEquals(t7, t8);
		
		TypeDescriptor t9 = new TypeDescriptor(getClass().getField("listField"));
		TypeDescriptor t10 = new TypeDescriptor(getClass().getField("listField"));
		assertEquals(t9, t10);

		TypeDescriptor t11 = new TypeDescriptor(getClass().getField("mapField"));
		TypeDescriptor t12 = new TypeDescriptor(getClass().getField("mapField"));
		assertEquals(t11, t12);
	}
	
}
