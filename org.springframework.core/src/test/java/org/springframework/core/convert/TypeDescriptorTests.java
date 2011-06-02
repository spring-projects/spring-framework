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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
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
	public void forCollection() {
		List<String> list = new ArrayList<String>();
		list.add("1");
		TypeDescriptor desc = TypeDescriptor.forObject(list);
		assertEquals(String.class, desc.getElementType());
	}

	@Test
	public void forCollectionEmpty() {
		List<String> list = new ArrayList<String>();
		TypeDescriptor desc = TypeDescriptor.forObject(list);
		assertNull(desc.getElementType());
	}

	@Test
	public void forCollectionSuperClassCommonType() throws SecurityException, NoSuchFieldException {
		List<Number> list = new ArrayList<Number>();
		list.add(1);
		list.add(2L);
		TypeDescriptor desc = TypeDescriptor.forObject(list);
		assertEquals(Number.class, desc.getElementType());
	}

	public List<Long> longs;
	
	@Test
	public void forCollectionNoObviousCommonType() {
		List<Object> collection = new ArrayList<Object>();
		List<String> list = new ArrayList<String>();
		list.add("1");
		collection.add(list);
		Map<String, String> map = new HashMap<String, String>();
		collection.add(map);
		map.put("1", "2");
		TypeDescriptor desc = TypeDescriptor.forObject(collection);
		assertEquals(Cloneable.class, desc.getElementType());
	}

	@Test
	public void forCollectionNoCommonType() {
		List<Object> collection = new ArrayList<Object>();
		collection.add(new Object());
		collection.add("1");
		TypeDescriptor desc = TypeDescriptor.forObject(collection);		
		assertEquals(Object.class, desc.getElementType());
	}

	@Test
	public void forCollectionNested() {
		List<Object> collection = new ArrayList<Object>();
		collection.add(Arrays.asList("1", "2"));
		collection.add(Arrays.asList("3", "4"));
		TypeDescriptor desc = TypeDescriptor.forObject(collection);
		assertEquals(Arrays.asList("foo").getClass(), desc.getElementType());
		assertEquals(String.class, desc.getElementTypeDescriptor().getElementType());
	}

	@Test
	public void forMap() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("1", "2");
		TypeDescriptor desc = TypeDescriptor.forObject(map);
		assertEquals(String.class, desc.getMapKeyType());
		assertEquals(String.class, desc.getMapValueType());
	}

	@Test
	public void forMapEmpty() {
		Map<String, String> map = new HashMap<String, String>();
		TypeDescriptor desc = TypeDescriptor.forObject(map);
		assertNull(desc.getMapKeyType());
		assertNull(desc.getMapValueType());
	}

	@Test
	public void forMapCommonSuperClass() {
		Map<Number, Number> map = new HashMap<Number, Number>();
		map.put(1, 2);
		map.put(2L, 3L);
		TypeDescriptor desc = TypeDescriptor.forObject(map);
		assertEquals(Number.class, desc.getMapKeyType());
		assertEquals(Number.class, desc.getMapValueType());
	}

	@Test
	public void forMapNoObviousCommonType() {
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put("1", "2");
		map.put(2, 2);
		TypeDescriptor desc = TypeDescriptor.forObject(map);
		assertEquals(Comparable.class, desc.getMapKeyType());
		assertEquals(Comparable.class, desc.getMapValueType());
	}

	@Test
	public void forMapNested() {
		Map<Integer, List<String>> map = new HashMap<Integer, List<String>>();
		map.put(1, Arrays.asList("1, 2"));
		TypeDescriptor desc = TypeDescriptor.forObject(map);
		assertEquals(Integer.class, desc.getMapKeyType());
		assertEquals(String.class, desc.getMapValueTypeDescriptor().getElementType());		
	}
	
	@Test
	public void listDescriptor() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("listOfString"));
		assertFalse(typeDescriptor.isArray());
		assertEquals(List.class, typeDescriptor.getType());
		assertEquals(String.class, typeDescriptor.getElementType());
		// TODO caught shorten these names but it is OK that they are fully qualified for now
		assertEquals("java.util.List<java.lang.String>", typeDescriptor.toString());
	}

	@Test
	public void listOfListOfStringDescriptor() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("listOfListOfString"));
		assertFalse(typeDescriptor.isArray());
		assertEquals(List.class, typeDescriptor.getType());
		assertEquals(List.class, typeDescriptor.getElementType());
		assertEquals(String.class, typeDescriptor.getElementTypeDescriptor().getElementType());
		assertEquals("java.util.List<java.util.List<java.lang.String>>", typeDescriptor.toString());
	}

	@Test
	public void listOfListOfUnknownDescriptor() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("listOfListOfUnknown"));
		assertFalse(typeDescriptor.isArray());
		assertEquals(List.class, typeDescriptor.getType());
		assertEquals(List.class, typeDescriptor.getElementType());
		assertEquals(Object.class, typeDescriptor.getElementTypeDescriptor().getElementType());
		assertEquals("java.util.List<java.util.List<java.lang.Object>>", typeDescriptor.toString());
	}

	@Test
	public void arrayTypeDescriptor() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("intArray"));
		assertTrue(typeDescriptor.isArray());
		assertEquals(Integer.TYPE,typeDescriptor.getElementType());
		assertEquals("int[]",typeDescriptor.toString());
	}

	@Test
	public void buildingArrayTypeDescriptor() throws Exception {
		TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(int[].class);
		assertTrue(typeDescriptor.isArray());
		assertEquals(Integer.TYPE, typeDescriptor.getElementType());
	}

	@Test
	@Ignore
	public void complexTypeDescriptor() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("arrayOfListOfString"));
		assertTrue(typeDescriptor.isArray());
		assertEquals(List.class,typeDescriptor.getElementType());
		assertEquals(String.class, typeDescriptor.getElementTypeDescriptor().getElementType());
		assertEquals("java.util.List[]",typeDescriptor.toString());
	}

	@Test
	public void complexTypeDescriptor2() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("nestedMapField"));
		assertTrue(typeDescriptor.isMap());
		assertEquals(String.class,typeDescriptor.getMapKeyType());
		assertEquals(List.class, typeDescriptor.getMapValueType());
		assertEquals(Integer.class, typeDescriptor.getMapValueTypeDescriptor().getElementType());
		assertEquals("java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>", typeDescriptor.toString());
	}

	@Test
	public void testEquals() throws Exception {
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
	
	@Test
	public void annotatedMethod() throws Exception {
		TypeDescriptor t1 = new TypeDescriptor(new MethodParameter(getClass().getMethod("testAnnotatedMethod", String.class), 0));
		assertEquals(String.class, t1.getType());
		assertNotNull(t1.getAnnotation(ParameterAnnotation.class));
	}

	@Target({ElementType.PARAMETER})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ParameterAnnotation {
		
	}
	
	public void testAnnotatedMethod(@ParameterAnnotation String parameter) {
		
	}
	
	@Test
	public void nestedMethodParameterType() throws Exception {
		TypeDescriptor t1 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test1", List.class), 0), 1);
		assertEquals(String.class, t1.getType());
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
	public void nestedFieldTypeMapTwoLevels() throws Exception {
		TypeDescriptor t1 = TypeDescriptor.nested(getClass().getField("test4"), 2);
		assertEquals(String.class, t1.getType());
	}

	public List<Map<Integer, String>> test4;

	@Test
	public void nestedPropertyTypeMapTwoLevels() throws Exception {
		PropertyDescriptor property = new PropertyDescriptor("test4", getClass().getMethod("getTest4", null), getClass().getMethod("setTest4", List.class));
		TypeDescriptor t1 = TypeDescriptor.nested(getClass(), property, 2);
		assertEquals(String.class, t1.getType());
	}

	public List<Map<Integer, String>> getTest4() {
		return null;
	}
	
	public void setTest4(List<Map<Integer, String>> test4) {
		
	}
	
	@Test
	public void property() throws Exception {
		PropertyDescriptor property = new PropertyDescriptor("property", getClass().getMethod("getProperty", null), getClass().getMethod("setProperty", Map.class));		
		TypeDescriptor desc = new TypeDescriptor(getClass(), property);
		assertEquals(Integer.class, desc.getMapKeyTypeDescriptor().getElementType());
		assertEquals(Long.class, desc.getMapValueTypeDescriptor().getElementType());
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
	@Ignore
	public void field() throws Exception {
		// typeIndex handling not currently supported by fields
		TypeDescriptor desc = new TypeDescriptor(getClass().getField("field"));
		assertEquals(Integer.class, desc.getMapKeyTypeDescriptor().getElementType());
		assertEquals(Long.class, desc.getMapValueTypeDescriptor().getElementType());
	}
	
	public Map<List<Integer>, List<Long>> field;


	@Test
	public void methodParameter() throws Exception {
		TypeDescriptor desc = new TypeDescriptor(new MethodParameter(getClass().getMethod("setProperty", Map.class), 0));
		assertEquals(Integer.class, desc.getMapKeyTypeDescriptor().getElementType());
		assertEquals(Long.class, desc.getMapValueTypeDescriptor().getElementType());
	}
	
	@Test
	public void complexProperty() throws Exception {
		PropertyDescriptor property = new PropertyDescriptor("complexProperty", getClass().getMethod("getComplexProperty", null), getClass().getMethod("setComplexProperty", Map.class));		
		TypeDescriptor desc = new TypeDescriptor(getClass(), property);
		//assertEquals(String.class, desc.getMapKeyType());
		assertEquals(Integer.class, desc.getMapValueTypeDescriptor().getElementTypeDescriptor().getElementType());
	}
	
	public Map<String, List<List<Integer>>> getComplexProperty() {
		return null;
	}
	
	public void setComplexProperty(Map<String, List<List<Integer>>> complexProperty) {
		
	}

	@Test
	public void genericType() throws Exception {
		GenericType<Integer> genericBean = new IntegerType();
		PropertyDescriptor property = new PropertyDescriptor("property", genericBean.getClass().getMethod("getProperty", null), genericBean.getClass().getMethod("setProperty", Integer.class));		
		TypeDescriptor desc = new TypeDescriptor(genericBean.getClass(), property);
		assertEquals(Integer.class, desc.getType());
	}

	@Test
	public void genericTypeList() throws Exception {
		GenericType<Integer> genericBean = new IntegerType();
		PropertyDescriptor property = new PropertyDescriptor("listProperty", genericBean.getClass().getMethod("getListProperty", null), genericBean.getClass().getMethod("setListProperty", List.class));		
		TypeDescriptor desc = new TypeDescriptor(genericBean.getClass(), property);
		assertEquals(List.class, desc.getType());
		assertEquals(Integer.class, desc.getElementType());
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
	public void genericClassList() throws Exception {
		IntegerClass genericBean = new IntegerClass();
		PropertyDescriptor property = new GenericTypeAwarePropertyDescriptor(genericBean.getClass(), "listProperty", genericBean.getClass().getMethod("getListProperty", null), genericBean.getClass().getMethod("setListProperty", List.class), null);		
		TypeDescriptor desc = new TypeDescriptor(genericBean.getClass(), property);
		assertEquals(List.class, desc.getType());
		assertEquals(Integer.class, desc.getElementType());
	}
	
	public static class GenericClass<T> {
		
		public T getProperty() {
			return null;
		}
		
		public void setProperty(T t) {
		}
		
		public List<T> getListProperty() {
			return null;
		}
		
		public void setListProperty(List<T> t) {
		}
		
	}
	
	public static class IntegerClass extends GenericClass<Integer> {
		
	}
	
	private static class GenericTypeAwarePropertyDescriptor extends PropertyDescriptor {

		private final Class beanClass;

		private final Method readMethod;

		private final Method writeMethod;

		private final Class propertyEditorClass;

		private volatile Set<Method> ambiguousWriteMethods;

		private Class propertyType;

		private MethodParameter writeMethodParameter;


		public GenericTypeAwarePropertyDescriptor(Class beanClass, String propertyName,
				Method readMethod, Method writeMethod, Class propertyEditorClass)
				throws IntrospectionException {

			super(propertyName, null, null);
			this.beanClass = beanClass;
			this.propertyEditorClass = propertyEditorClass;

			Method readMethodToUse = BridgeMethodResolver.findBridgedMethod(readMethod);
			Method writeMethodToUse = BridgeMethodResolver.findBridgedMethod(writeMethod);
			if (writeMethodToUse == null && readMethodToUse != null) {
				// Fallback: Original JavaBeans introspection might not have found matching setter
				// method due to lack of bridge method resolution, in case of the getter using a
				// covariant return type whereas the setter is defined for the concrete property type.
				writeMethodToUse = ClassUtils.getMethodIfAvailable(this.beanClass,
						"set" + StringUtils.capitalize(getName()), readMethodToUse.getReturnType());
			}
			this.readMethod = readMethodToUse;
			this.writeMethod = writeMethodToUse;

			if (this.writeMethod != null && this.readMethod == null) {
				// Write method not matched against read method: potentially ambiguous through
				// several overloaded variants, in which case an arbitrary winner has been chosen
				// by the JDK's JavaBeans Introspector...
				Set<Method> ambiguousCandidates = new HashSet<Method>();
				for (Method method : beanClass.getMethods()) {
					if (method.getName().equals(writeMethodToUse.getName()) &&
							!method.equals(writeMethodToUse) && !method.isBridge()) {
						ambiguousCandidates.add(method);
					}
				}
				if (!ambiguousCandidates.isEmpty()) {
					this.ambiguousWriteMethods = ambiguousCandidates;
				}
			}
		}


		@Override
		public Method getReadMethod() {
			return this.readMethod;
		}

		@Override
		public Method getWriteMethod() {
			return this.writeMethod;
		}

		public Method getWriteMethodForActualAccess() {
			Set<Method> ambiguousCandidates = this.ambiguousWriteMethods;
			if (ambiguousCandidates != null) {
				this.ambiguousWriteMethods = null;
				LogFactory.getLog(GenericTypeAwarePropertyDescriptor.class).warn("Invalid JavaBean property '" +
						getName() + "' being accessed! Ambiguous write methods found next to actually used [" +
						this.writeMethod + "]: " + ambiguousCandidates);
			}
			return this.writeMethod;
		}

		@Override
		public Class getPropertyEditorClass() {
			return this.propertyEditorClass;
		}

		@Override
		public synchronized Class getPropertyType() {
			if (this.propertyType == null) {
				if (this.readMethod != null) {
					this.propertyType = GenericTypeResolver.resolveReturnType(this.readMethod, this.beanClass);
				}
				else {
					MethodParameter writeMethodParam = getWriteMethodParameter();
					if (writeMethodParam != null) {
						this.propertyType = writeMethodParam.getParameterType();
					}
					else {
						this.propertyType = super.getPropertyType();
					}
				}
			}
			return this.propertyType;
		}

		public synchronized MethodParameter getWriteMethodParameter() {
			if (this.writeMethod == null) {
				return null;
			}
			if (this.writeMethodParameter == null) {
				this.writeMethodParameter = new MethodParameter(this.writeMethod, 0);
				GenericTypeResolver.resolveParameterType(this.writeMethodParameter, this.beanClass);
			}
			return this.writeMethodParameter;
		}

	}

	
}
