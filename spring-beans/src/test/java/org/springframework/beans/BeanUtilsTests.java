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

package org.springframework.beans;

import static org.junit.Assert.*;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;

import test.beans.DerivedTestBean;
import test.beans.ITestBean;
import test.beans.TestBean;

/**
 * Unit tests for {@link BeanUtils}.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @since 19.05.2003
 */
public final class BeanUtilsTests {

	@Test
	public void testInstantiateClass() {
		// give proper class
		BeanUtils.instantiateClass(ArrayList.class);

		try {
			// give interface
			BeanUtils.instantiateClass(List.class);
			fail("Should have thrown FatalBeanException");
		}
		catch (FatalBeanException ex) {
			// expected
		}

		try {
			// give class without default constructor
			BeanUtils.instantiateClass(CustomDateEditor.class);
			fail("Should have thrown FatalBeanException");
		}
		catch (FatalBeanException ex) {
			// expected
		}
	}

	@Test
	public void testGetPropertyDescriptors() throws Exception {
		PropertyDescriptor[] actual = Introspector.getBeanInfo(TestBean.class).getPropertyDescriptors();
		PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(TestBean.class);
		assertNotNull("Descriptors should not be null", descriptors);
		assertEquals("Invalid number of descriptors returned", actual.length, descriptors.length);
	}

	@Test
	public void testBeanPropertyIsArray() {
		PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(ContainerBean.class);
		for (int i = 0; i < descriptors.length; i++) {
			PropertyDescriptor descriptor = descriptors[i];
			if ("containedBeans".equals(descriptor.getName())) {
				assertTrue("Property should be an array", descriptor.getPropertyType().isArray());
				assertEquals(descriptor.getPropertyType().getComponentType(), ContainedBean.class);
			}
		}
	}

	@Test
	public void testFindEditorByConvention() {
		assertEquals(ResourceEditor.class, BeanUtils.findEditorByConvention(Resource.class).getClass());
	}

	@Test
	public void testCopyProperties() throws Exception {
		TestBean tb = new TestBean();
		tb.setName("rod");
		tb.setAge(32);
		tb.setTouchy("touchy");
		TestBean tb2 = new TestBean();
		assertTrue("Name empty", tb2.getName() == null);
		assertTrue("Age empty", tb2.getAge() == 0);
		assertTrue("Touchy empty", tb2.getTouchy() == null);
		BeanUtils.copyProperties(tb, tb2);
		assertTrue("Name copied", tb2.getName().equals(tb.getName()));
		assertTrue("Age copied", tb2.getAge() == tb.getAge());
		assertTrue("Touchy copied", tb2.getTouchy().equals(tb.getTouchy()));
	}

	@Test
	public void testCopyPropertiesWithDifferentTypes1() throws Exception {
		DerivedTestBean tb = new DerivedTestBean();
		tb.setName("rod");
		tb.setAge(32);
		tb.setTouchy("touchy");
		TestBean tb2 = new TestBean();
		assertTrue("Name empty", tb2.getName() == null);
		assertTrue("Age empty", tb2.getAge() == 0);
		assertTrue("Touchy empty", tb2.getTouchy() == null);
		BeanUtils.copyProperties(tb, tb2);
		assertTrue("Name copied", tb2.getName().equals(tb.getName()));
		assertTrue("Age copied", tb2.getAge() == tb.getAge());
		assertTrue("Touchy copied", tb2.getTouchy().equals(tb.getTouchy()));
	}

	@Test
	public void testCopyPropertiesWithDifferentTypes2() throws Exception {
		TestBean tb = new TestBean();
		tb.setName("rod");
		tb.setAge(32);
		tb.setTouchy("touchy");
		DerivedTestBean tb2 = new DerivedTestBean();
		assertTrue("Name empty", tb2.getName() == null);
		assertTrue("Age empty", tb2.getAge() == 0);
		assertTrue("Touchy empty", tb2.getTouchy() == null);
		BeanUtils.copyProperties(tb, tb2);
		assertTrue("Name copied", tb2.getName().equals(tb.getName()));
		assertTrue("Age copied", tb2.getAge() == tb.getAge());
		assertTrue("Touchy copied", tb2.getTouchy().equals(tb.getTouchy()));
	}

	@Test
	public void testCopyPropertiesWithEditable() throws Exception {
		TestBean tb = new TestBean();
		assertTrue("Name empty", tb.getName() == null);
		tb.setAge(32);
		tb.setTouchy("bla");
		TestBean tb2 = new TestBean();
		tb2.setName("rod");
		assertTrue("Age empty", tb2.getAge() == 0);
		assertTrue("Touchy empty", tb2.getTouchy() == null);

		// "touchy" should not be copied: it's not defined in ITestBean
		BeanUtils.copyProperties(tb, tb2, ITestBean.class);
		assertTrue("Name copied", tb2.getName() == null);
		assertTrue("Age copied", tb2.getAge() == 32);
		assertTrue("Touchy still empty", tb2.getTouchy() == null);
	}

	@Test
	public void testCopyPropertiesWithIgnore() throws Exception {
		TestBean tb = new TestBean();
		assertTrue("Name empty", tb.getName() == null);
		tb.setAge(32);
		tb.setTouchy("bla");
		TestBean tb2 = new TestBean();
		tb2.setName("rod");
		assertTrue("Age empty", tb2.getAge() == 0);
		assertTrue("Touchy empty", tb2.getTouchy() == null);

		// "spouse", "touchy", "age" should not be copied
		BeanUtils.copyProperties(tb, tb2, new String[]{"spouse", "touchy", "age"});
		assertTrue("Name copied", tb2.getName() == null);
		assertTrue("Age still empty", tb2.getAge() == 0);
		assertTrue("Touchy still empty", tb2.getTouchy() == null);
	}

	@Test
	public void testCopyPropertiesWithIgnoredNonExistingProperty() {
		NameAndSpecialProperty source = new NameAndSpecialProperty();
		source.setName("name");
		TestBean target = new TestBean();
		BeanUtils.copyProperties(source, target, new String[]{"specialProperty"});
		assertEquals(target.getName(), "name");
	}

	@Test
	public void testResolveSimpleSignature() throws Exception {
		Method desiredMethod = MethodSignatureBean.class.getMethod("doSomething");
		assertSignatureEquals(desiredMethod, "doSomething");
		assertSignatureEquals(desiredMethod, "doSomething()");
	}

	@Test
	public void testResolveInvalidSignature() throws Exception {
		try {
			BeanUtils.resolveSignature("doSomething(", MethodSignatureBean.class);
			fail("Should not be able to parse with opening but no closing paren.");
		}
		catch (IllegalArgumentException ex) {
			// success
		}

		try {
			BeanUtils.resolveSignature("doSomething)", MethodSignatureBean.class);
			fail("Should not be able to parse with closing but no opening paren.");
		}
		catch (IllegalArgumentException ex) {
			// success
		}
	}

	@Test
	public void testResolveWithAndWithoutArgList() throws Exception {
		Method desiredMethod = MethodSignatureBean.class.getMethod("doSomethingElse", new Class[]{String.class, int.class});
		assertSignatureEquals(desiredMethod, "doSomethingElse");
		assertNull(BeanUtils.resolveSignature("doSomethingElse()", MethodSignatureBean.class));
	}

	@Test
	public void testResolveTypedSignature() throws Exception {
		Method desiredMethod = MethodSignatureBean.class.getMethod("doSomethingElse", new Class[]{String.class, int.class});
		assertSignatureEquals(desiredMethod, "doSomethingElse(java.lang.String, int)");
	}

	@Test
	public void testResolveOverloadedSignature() throws Exception {
		// test resolve with no args
		Method desiredMethod = MethodSignatureBean.class.getMethod("overloaded");
		assertSignatureEquals(desiredMethod, "overloaded()");

		// resolve with single arg
		desiredMethod = MethodSignatureBean.class.getMethod("overloaded", new Class[]{String.class});
		assertSignatureEquals(desiredMethod, "overloaded(java.lang.String)");

		// resolve with two args
		desiredMethod = MethodSignatureBean.class.getMethod("overloaded", new Class[]{String.class, BeanFactory.class});
		assertSignatureEquals(desiredMethod, "overloaded(java.lang.String, org.springframework.beans.factory.BeanFactory)");
	}

	@Test
	public void testResolveSignatureWithArray() throws Exception {
		Method desiredMethod = MethodSignatureBean.class.getMethod("doSomethingWithAnArray", new Class[]{String[].class});
		assertSignatureEquals(desiredMethod, "doSomethingWithAnArray(java.lang.String[])");

		desiredMethod = MethodSignatureBean.class.getMethod("doSomethingWithAMultiDimensionalArray", new Class[]{String[][].class});
		assertSignatureEquals(desiredMethod, "doSomethingWithAMultiDimensionalArray(java.lang.String[][])");
	}

	@Test
	public void testSPR6063() {
		PropertyDescriptor[] descrs = BeanUtils.getPropertyDescriptors(Bean.class);

		PropertyDescriptor keyDescr = BeanUtils.getPropertyDescriptor(Bean.class, "value");
		assertEquals(String.class, keyDescr.getPropertyType());
		for (PropertyDescriptor propertyDescriptor : descrs) {
			if (propertyDescriptor.getName().equals(keyDescr.getName())) {
				assertEquals(propertyDescriptor.getName() + " has unexpected type", keyDescr.getPropertyType(), propertyDescriptor.getPropertyType());
			}
		}
	}

	private void assertSignatureEquals(Method desiredMethod, String signature) {
		assertEquals(desiredMethod, BeanUtils.resolveSignature(signature, MethodSignatureBean.class));
	}


	private static class NameAndSpecialProperty {

		private String name;

		private int specialProperty;

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public void setSpecialProperty(int specialProperty) {
			this.specialProperty = specialProperty;
		}

		public int getSpecialProperty() {
			return specialProperty;
		}
	}


	private static class ContainerBean {

		private ContainedBean[] containedBeans;

		public ContainedBean[] getContainedBeans() {
			return containedBeans;
		}

		public void setContainedBeans(ContainedBean[] containedBeans) {
			this.containedBeans = containedBeans;
		}
	}


	private static class ContainedBean {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}


	private static class MethodSignatureBean {

		public void doSomething() {
		}

		public void doSomethingElse(String s, int x) {
		}

		public void overloaded() {
		}

		public void overloaded(String s) {
		}

		public void overloaded(String s, BeanFactory beanFactory) {
		}

		public void doSomethingWithAnArray(String[] strings) {
		}

		public void doSomethingWithAMultiDimensionalArray(String[][] strings) {
		}
	}

	private interface MapEntry<K, V> {

		K getKey();

		void setKey(V value);

		V getValue();

		void setValue(V value);
	}

	private static class Bean implements MapEntry<String, String> {

		private String key;

		private String value;

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public void setKey(String aKey) {
			key = aKey;
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public void setValue(String aValue) {
			value = aValue;
		}
	}

}
