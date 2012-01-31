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

package org.springframework.beans;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.ExtendedBeanInfo.PropertyDescriptorComparator;

import test.beans.TestBean;

/**
 * Unit tests for {@link ExtendedBeanInfo}.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class ExtendedBeanInfoTests {

	@Test
	public void standardReadMethodOnly() throws IntrospectionException {
		@SuppressWarnings("unused") class C {
			public String getFoo() { return null; }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);
		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		assertThat(hasReadMethodForProperty(bi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "foo"), is(false));

		assertThat(hasReadMethodForProperty(ebi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(ebi, "foo"), is(false));
	}

	@Test
	public void standardWriteMethodOnly() throws IntrospectionException {
		@SuppressWarnings("unused") class C {
			public void setFoo(String f) { }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);
		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		assertThat(hasReadMethodForProperty(bi, "foo"), is(false));
		assertThat(hasWriteMethodForProperty(bi, "foo"), is(true));

		assertThat(hasReadMethodForProperty(ebi, "foo"), is(false));
		assertThat(hasWriteMethodForProperty(ebi, "foo"), is(true));
	}

	@Test
	public void standardReadAndWriteMethods() throws IntrospectionException {
		@SuppressWarnings("unused") class C {
			public void setFoo(String f) { }
			public String getFoo() { return null; }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);
		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		assertThat(hasReadMethodForProperty(bi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "foo"), is(true));

		assertThat(hasReadMethodForProperty(ebi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(ebi, "foo"), is(true));
	}

	@Test
	public void nonStandardWriteMethodOnly() throws IntrospectionException {
		@SuppressWarnings("unused") class C {
			public C setFoo(String foo) { return this; }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);
		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		assertThat(hasReadMethodForProperty(bi, "foo"), is(false));
		assertThat(hasWriteMethodForProperty(bi, "foo"), is(false));

		assertThat(hasReadMethodForProperty(ebi, "foo"), is(false));
		assertThat(hasWriteMethodForProperty(ebi, "foo"), is(true));
	}

	@Test
	public void standardReadAndNonStandardWriteMethods() throws IntrospectionException {
		@SuppressWarnings("unused") class C {
			public String getFoo() { return null; }
			public C setFoo(String foo) { return this; }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);

		assertThat(hasReadMethodForProperty(bi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "foo"), is(false));

		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		assertThat(hasReadMethodForProperty(bi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "foo"), is(true));

		assertThat(hasReadMethodForProperty(ebi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(ebi, "foo"), is(true));
	}

	@Test
	public void standardReadMethodsAndOverloadedNonStandardWriteMethods() throws Exception {
		@SuppressWarnings("unused") class C {
			public String getFoo() { return null; }
			public C setFoo(String foo) { return this; }
			public C setFoo(Number foo) { return this; }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);

		assertThat(hasReadMethodForProperty(bi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "foo"), is(false));

		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		assertThat(hasReadMethodForProperty(bi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "foo"), is(true));

		assertThat(hasReadMethodForProperty(ebi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(ebi, "foo"), is(true));

		for (PropertyDescriptor pd : ebi.getPropertyDescriptors()) {
			if (pd.getName().equals("foo")) {
				assertThat(pd.getWriteMethod(), is(C.class.getMethod("setFoo", String.class)));
				return;
			}
		}
		fail("never matched write method");
	}

	@Test
	public void standardReadMethodInSuperclassAndNonStandardWriteMethodInSubclass() throws Exception {
		@SuppressWarnings("unused") class B {
			public String getFoo() { return null; }
		}
		@SuppressWarnings("unused") class C extends B {
			public C setFoo(String foo) { return this; }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);

		assertThat(hasReadMethodForProperty(bi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "foo"), is(false));

		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		assertThat(hasReadMethodForProperty(bi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "foo"), is(true));

		assertThat(hasReadMethodForProperty(ebi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(ebi, "foo"), is(true));
	}

	@Test
	public void standardReadMethodInSuperAndSubclassesAndGenericBuilderStyleNonStandardWriteMethodInSuperAndSubclasses() throws Exception {
		abstract class B<This extends B<This>> {
			@SuppressWarnings("unchecked")
			protected final This instance = (This) this;
			private String foo;
			public String getFoo() { return foo; }
			public This setFoo(String foo) {
				this.foo = foo;
				return this.instance;
			}
		}

		class C extends B<C> {
			private int bar = -1;
			public int getBar() { return bar; }
			public C setBar(int bar) {
				this.bar = bar;
				return this.instance;
			}
		}

		C c = new C()
			.setFoo("blue")
			.setBar(42);

		assertThat(c.getFoo(), is("blue"));
		assertThat(c.getBar(), is(42));

		BeanInfo bi = Introspector.getBeanInfo(C.class);

		assertThat(hasReadMethodForProperty(bi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "foo"), is(false));

		assertThat(hasReadMethodForProperty(bi, "bar"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "bar"), is(false));

		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		assertThat(hasReadMethodForProperty(bi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "foo"), is(true));

		assertThat(hasReadMethodForProperty(bi, "bar"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "bar"), is(true));

		assertThat(hasReadMethodForProperty(ebi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(ebi, "foo"), is(true));

		assertThat(hasReadMethodForProperty(ebi, "bar"), is(true));
		assertThat(hasWriteMethodForProperty(ebi, "bar"), is(true));
	}

	@Test
	public void nonPublicStandardReadAndWriteMethods() throws Exception {
		@SuppressWarnings("unused") class C {
			String getFoo() { return null; }
			C setFoo(String foo) { return this; }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);
		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		assertThat(hasReadMethodForProperty(bi, "foo"), is(false));
		assertThat(hasWriteMethodForProperty(bi, "foo"), is(false));

		assertThat(hasReadMethodForProperty(ebi, "foo"), is(false));
		assertThat(hasWriteMethodForProperty(ebi, "foo"), is(false));
	}

	/**
	 * {@link ExtendedBeanInfo} should behave exactly like {@link BeanInfo}
	 * in strange edge cases.
	 */
	@Test
	public void readMethodReturnsSupertypeOfWriteMethodParameter() throws IntrospectionException {
		@SuppressWarnings("unused") class C {
			public Number getFoo() { return null; }
			public void setFoo(Integer foo) { }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);
		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		assertThat(hasReadMethodForProperty(bi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "foo"), is(false));

		assertThat(hasReadMethodForProperty(ebi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(ebi, "foo"), is(false));
	}

	@Test
	public void indexedReadMethodReturnsSupertypeOfIndexedWriteMethodParameter() throws IntrospectionException {
		@SuppressWarnings("unused") class C {
			public Number getFoos(int index) { return null; }
			public void setFoos(int index, Integer foo) { }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);
		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		assertThat(hasIndexedReadMethodForProperty(bi, "foos"), is(true));
		assertThat(hasIndexedWriteMethodForProperty(bi, "foos"), is(false));

		assertThat(hasIndexedReadMethodForProperty(ebi, "foos"), is(true));
		assertThat(hasIndexedWriteMethodForProperty(ebi, "foos"), is(false));
	}

	/**
	 * {@link ExtendedBeanInfo} should behave exactly like {@link BeanInfo}
	 * in strange edge cases.
	 */
	@Test
	public void readMethodReturnsSubtypeOfWriteMethodParameter() throws IntrospectionException {
		@SuppressWarnings("unused") class C {
			public Integer getFoo() { return null; }
			public void setFoo(Number foo) { }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);
		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		assertThat(hasReadMethodForProperty(bi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "foo"), is(false));

		assertThat(hasReadMethodForProperty(ebi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(ebi, "foo"), is(false));
	}

	@Test
	public void indexedReadMethodReturnsSubtypeOfIndexedWriteMethodParameter() throws IntrospectionException {
		@SuppressWarnings("unused") class C {
			public Integer getFoos(int index) { return null; }
			public void setFoo(int index, Number foo) { }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);
		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		assertThat(hasIndexedReadMethodForProperty(bi, "foos"), is(true));
		assertThat(hasIndexedWriteMethodForProperty(bi, "foos"), is(false));

		assertThat(hasIndexedReadMethodForProperty(ebi, "foos"), is(true));
		assertThat(hasIndexedWriteMethodForProperty(ebi, "foos"), is(false));
	}

	@Test
	public void indexedReadMethodOnly() throws IntrospectionException {
		@SuppressWarnings("unused")
		class C {
			// indexed read method
			public String getFoos(int i) { return null; }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);
		BeanInfo ebi = new ExtendedBeanInfo(Introspector.getBeanInfo(C.class));

		assertThat(hasReadMethodForProperty(bi, "foos"), is(false));
		assertThat(hasIndexedReadMethodForProperty(bi, "foos"), is(true));

		assertThat(hasReadMethodForProperty(ebi, "foos"), is(false));
		assertThat(hasIndexedReadMethodForProperty(ebi, "foos"), is(true));
	}

	@Test
	public void indexedWriteMethodOnly() throws IntrospectionException {
		@SuppressWarnings("unused")
		class C {
			// indexed write method
			public void setFoos(int i, String foo) { }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);
		BeanInfo ebi = new ExtendedBeanInfo(Introspector.getBeanInfo(C.class));

		assertThat(hasWriteMethodForProperty(bi, "foos"), is(false));
		assertThat(hasIndexedWriteMethodForProperty(bi, "foos"), is(true));

		assertThat(hasWriteMethodForProperty(ebi, "foos"), is(false));
		assertThat(hasIndexedWriteMethodForProperty(ebi, "foos"), is(true));
	}

	@Test
	public void indexedReadAndIndexedWriteMethods() throws IntrospectionException {
		@SuppressWarnings("unused")
		class C {
			// indexed read method
			public String getFoos(int i) { return null; }
			// indexed write method
			public void setFoos(int i, String foo) { }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);
		BeanInfo ebi = new ExtendedBeanInfo(Introspector.getBeanInfo(C.class));

		assertThat(hasReadMethodForProperty(bi, "foos"), is(false));
		assertThat(hasIndexedReadMethodForProperty(bi, "foos"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "foos"), is(false));
		assertThat(hasIndexedWriteMethodForProperty(bi, "foos"), is(true));

		assertThat(hasReadMethodForProperty(ebi, "foos"), is(false));
		assertThat(hasIndexedReadMethodForProperty(ebi, "foos"), is(true));
		assertThat(hasWriteMethodForProperty(ebi, "foos"), is(false));
		assertThat(hasIndexedWriteMethodForProperty(ebi, "foos"), is(true));
	}

	@Test
	public void readAndWriteAndIndexedReadAndIndexedWriteMethods() throws IntrospectionException {
		@SuppressWarnings("unused")
		class C {
			// read method
			public String[] getFoos() { return null; }
			// indexed read method
			public String getFoos(int i) { return null; }
			// write method
			public void setFoos(String[] foos) { }
			// indexed write method
			public void setFoos(int i, String foo) { }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);
		BeanInfo ebi = new ExtendedBeanInfo(Introspector.getBeanInfo(C.class));

		assertThat(hasReadMethodForProperty(bi, "foos"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "foos"), is(true));
		assertThat(hasIndexedReadMethodForProperty(bi, "foos"), is(true));
		assertThat(hasIndexedWriteMethodForProperty(bi, "foos"), is(true));

		assertThat(hasReadMethodForProperty(ebi, "foos"), is(true));
		assertThat(hasWriteMethodForProperty(ebi, "foos"), is(true));
		assertThat(hasIndexedReadMethodForProperty(ebi, "foos"), is(true));
		assertThat(hasIndexedWriteMethodForProperty(ebi, "foos"), is(true));
	}

	@Test
	public void indexedReadAndNonStandardIndexedWrite() throws IntrospectionException {
		@SuppressWarnings("unused")
		class C {
			// indexed read method
			public String getFoos(int i) { return null; }
			// non-standard indexed write method
			public C setFoos(int i, String foo) { return this; }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);
		BeanInfo ebi = new ExtendedBeanInfo(Introspector.getBeanInfo(C.class));

		assertThat(hasIndexedReadMethodForProperty(bi, "foos"), is(true));
		// interesting! standard Inspector picks up non-void return types on indexed write methods by default
		assertThat(hasIndexedWriteMethodForProperty(bi, "foos"), is(true));

		assertThat(hasIndexedReadMethodForProperty(ebi, "foos"), is(true));
		assertThat(hasIndexedWriteMethodForProperty(ebi, "foos"), is(true));
	}

	@Test
	public void indexedReadAndNonStandardWriteAndNonStandardIndexedWrite() throws IntrospectionException {
		@SuppressWarnings("unused")
		class C {
			// non-standard write method
			public C setFoos(String[] foos) { return this; }
			// indexed read method
			public String getFoos(int i) { return null; }
			// non-standard indexed write method
			public C setFoos(int i, String foo) { return this; }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);

		assertThat(hasIndexedReadMethodForProperty(bi, "foos"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "foos"), is(false));
		// again as above, standard Inspector picks up non-void return types on indexed write methods by default
		assertThat(hasIndexedWriteMethodForProperty(bi, "foos"), is(true));

		BeanInfo ebi = new ExtendedBeanInfo(Introspector.getBeanInfo(C.class));

		assertThat(hasIndexedReadMethodForProperty(bi, "foos"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "foos"), is(true));
		// again as above, standard Inspector picks up non-void return types on indexed write methods by default
		assertThat(hasIndexedWriteMethodForProperty(bi, "foos"), is(true));

		assertThat(hasIndexedReadMethodForProperty(ebi, "foos"), is(true));
		assertThat(hasWriteMethodForProperty(ebi, "foos"), is(true));
		assertThat(hasIndexedWriteMethodForProperty(ebi, "foos"), is(true));
	}

	@Test
	public void subclassWriteMethodWithCovariantReturnType() throws IntrospectionException {
		@SuppressWarnings("unused") class B {
			public String getFoo() { return null; }
			public Number setFoo(String foo) { return null; }
		}
		class C extends B {
			public String getFoo() { return null; }
			public Integer setFoo(String foo) { return null; }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);

		assertThat(hasReadMethodForProperty(bi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "foo"), is(false));

		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		assertThat(hasReadMethodForProperty(bi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(bi, "foo"), is(true));

		assertThat(hasReadMethodForProperty(ebi, "foo"), is(true));
		assertThat(hasWriteMethodForProperty(ebi, "foo"), is(true));

		assertThat(ebi.getPropertyDescriptors().length, equalTo(bi.getPropertyDescriptors().length));
	}

	@Test
	public void nonStandardReadMethodAndStandardWriteMethod() throws IntrospectionException {
		@SuppressWarnings("unused") class C {
			public void getFoo() { }
			public void setFoo(String foo) { }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);
		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		assertThat(hasReadMethodForProperty(bi, "foo"), is(false));
		assertThat(hasWriteMethodForProperty(bi, "foo"), is(true));

		assertThat(hasReadMethodForProperty(ebi, "foo"), is(false));
		assertThat(hasWriteMethodForProperty(ebi, "foo"), is(true));
	}

	/**
	 * Ensures that an empty string is not passed into a PropertyDescriptor constructor. This
	 * could occur when handling ArrayList.set(int,Object)
	 */
	@Test
	public void emptyPropertiesIgnored() throws IntrospectionException {
		@SuppressWarnings("unused") class C {
			public Object set(Object o) { return null; }
			public Object set(int i, Object o) { return null; }
		}

		BeanInfo bi = Introspector.getBeanInfo(C.class);
		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		assertThat(ebi.getPropertyDescriptors(), equalTo(bi.getPropertyDescriptors()));
	}

	/**
	 * Corners the bug revealed by SPR-8522, in which an (apparently) indexed write method
	 * without a corresponding indexed read method would fail to be processed correctly by
	 * ExtendedBeanInfo. The local class C below represents the relevant methods from
	 * Google's GsonBuilder class. Interestingly, the setDateFormat(int, int) method was
	 * not actually intended to serve as an indexed write method; it just appears that way.
	 */
	@Test
	public void reproSpr8522() throws IntrospectionException {
		@SuppressWarnings("unused") class C {
			public Object setDateFormat(String pattern) { return new Object(); }
			public Object setDateFormat(int style) { return new Object(); }
			public Object setDateFormat(int dateStyle, int timeStyle) { return new Object(); }
		}
		BeanInfo bi = Introspector.getBeanInfo(C.class);

		assertThat(hasReadMethodForProperty(bi, "dateFormat"), is(false));
		assertThat(hasWriteMethodForProperty(bi, "dateFormat"), is(false));
		assertThat(hasIndexedReadMethodForProperty(bi, "dateFormat"), is(false));
		assertThat(hasIndexedWriteMethodForProperty(bi, "dateFormat"), is(true));

		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		assertThat(hasReadMethodForProperty(bi, "dateFormat"), is(false));
		assertThat(hasWriteMethodForProperty(bi, "dateFormat"), is(true));
		assertThat(hasIndexedReadMethodForProperty(bi, "dateFormat"), is(false));
		assertThat(hasIndexedWriteMethodForProperty(bi, "dateFormat"), is(true));

		assertThat(hasReadMethodForProperty(ebi, "dateFormat"), is(false));
		assertThat(hasWriteMethodForProperty(ebi, "dateFormat"), is(true));
		assertThat(hasIndexedReadMethodForProperty(ebi, "dateFormat"), is(false));
		assertThat(hasIndexedWriteMethodForProperty(ebi, "dateFormat"), is(true));
	}

	@Test
	public void propertyCountsMatch() throws IntrospectionException {
		BeanInfo bi = Introspector.getBeanInfo(TestBean.class);
		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		assertThat(ebi.getPropertyDescriptors().length, equalTo(bi.getPropertyDescriptors().length));
	}

	@Test
	public void propertyCountsWithNonStandardWriteMethod() throws IntrospectionException {
		class ExtendedTestBean extends TestBean {
			@SuppressWarnings("unused")
			public ExtendedTestBean setFoo(String s) { return this; }
		}
		BeanInfo bi = Introspector.getBeanInfo(ExtendedTestBean.class);
		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		boolean found = false;
		for (PropertyDescriptor pd : ebi.getPropertyDescriptors()) {
			if (pd.getName().equals("foo")) {
				found = true;
			}
		}
		assertThat(found, is(true));
		assertThat(ebi.getPropertyDescriptors().length, equalTo(bi.getPropertyDescriptors().length+1));
	}

	/**
	 * {@link BeanInfo#getPropertyDescriptors()} returns alphanumerically sorted.
	 * Test that {@link ExtendedBeanInfo#getPropertyDescriptors()} does the same.
	 */
	@Test
	public void propertyDescriptorOrderIsEqual() throws IntrospectionException {
		BeanInfo bi = Introspector.getBeanInfo(TestBean.class);
		ExtendedBeanInfo ebi = new ExtendedBeanInfo(bi);

		for (int i = 0; i < bi.getPropertyDescriptors().length; i++) {
			assertThat("element " + i + " in BeanInfo and ExtendedBeanInfo propertyDescriptor arrays do not match",
					ebi.getPropertyDescriptors()[i].getName(), equalTo(bi.getPropertyDescriptors()[i].getName()));
		}
	}

	@Test
	public void propertyDescriptorComparator() throws IntrospectionException {
		PropertyDescriptorComparator c = new PropertyDescriptorComparator();
		assertThat(c.compare(new PropertyDescriptor("a", null, null), new PropertyDescriptor("a", null, null)), equalTo(0));
		assertThat(c.compare(new PropertyDescriptor("abc", null, null), new PropertyDescriptor("abc", null, null)), equalTo(0));
		assertThat(c.compare(new PropertyDescriptor("a", null, null), new PropertyDescriptor("b", null, null)), lessThan(0));
		assertThat(c.compare(new PropertyDescriptor("b", null, null), new PropertyDescriptor("a", null, null)), greaterThan(0));
		assertThat(c.compare(new PropertyDescriptor("abc", null, null), new PropertyDescriptor("abd", null, null)), lessThan(0));
		assertThat(c.compare(new PropertyDescriptor("xyz", null, null), new PropertyDescriptor("123", null, null)), greaterThan(0));
		assertThat(c.compare(new PropertyDescriptor("a", null, null), new PropertyDescriptor("abc", null, null)), lessThan(0));
		assertThat(c.compare(new PropertyDescriptor("abc", null, null), new PropertyDescriptor("a", null, null)), greaterThan(0));
		assertThat(c.compare(new PropertyDescriptor("abc", null, null), new PropertyDescriptor("b", null, null)), lessThan(0));

		assertThat(c.compare(new PropertyDescriptor(" ", null, null), new PropertyDescriptor("a", null, null)), lessThan(0));
		assertThat(c.compare(new PropertyDescriptor("1", null, null), new PropertyDescriptor("a", null, null)), lessThan(0));
		assertThat(c.compare(new PropertyDescriptor("a", null, null), new PropertyDescriptor("A", null, null)), greaterThan(0));
	}

	private boolean hasWriteMethodForProperty(BeanInfo beanInfo, String propertyName) {
		for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
			if (pd.getName().equals(propertyName)) {
				return pd.getWriteMethod() != null;
			}
		}
		return false;
	}

	private boolean hasReadMethodForProperty(BeanInfo beanInfo, String propertyName) {
		for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
			if (pd.getName().equals(propertyName)) {
				return pd.getReadMethod() != null;
			}
		}
		return false;
	}

	private boolean hasIndexedWriteMethodForProperty(BeanInfo beanInfo, String propertyName) {
		for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
			if (pd.getName().equals(propertyName)) {
				assertThat(propertyName + " property is not indexed", pd, instanceOf(IndexedPropertyDescriptor.class));
				return ((IndexedPropertyDescriptor)pd).getIndexedWriteMethod() != null;
			}
		}
		return false;
	}

	private boolean hasIndexedReadMethodForProperty(BeanInfo beanInfo, String propertyName) {
		for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
			if (pd.getName().equals(propertyName)) {
				assertThat(propertyName + " property is not indexed", pd, instanceOf(IndexedPropertyDescriptor.class));
				return ((IndexedPropertyDescriptor)pd).getIndexedReadMethod() != null;
			}
		}
		return false;
	}

	@Test
	public void reproSpr8806_y() throws IntrospectionException, SecurityException, NoSuchMethodException {
		Introspector.getBeanInfo(LawLibrary.class);
	}

	@Ignore @Test
	public void reproSpr8806_x() throws IntrospectionException, SecurityException, NoSuchMethodException {
		BeanInfo info = Introspector.getBeanInfo(LawLibrary.class);
		for (PropertyDescriptor d : info.getPropertyDescriptors()) {
			if (d.getName().equals("book")) {
				Method readMethod = d.getReadMethod();
				Method writeMethod = d.getWriteMethod();
				System.out.println(format("READ : %s.%s (bridge:%s)",
						readMethod.getDeclaringClass().getSimpleName(), readMethod.getName(), readMethod.isBridge()));
				System.out.println(format("WRITE: %s.%s (bridge:%s)",
						writeMethod.getDeclaringClass().getSimpleName(), writeMethod.getName(), writeMethod.isBridge()));
				new PropertyDescriptor("book", readMethod, writeMethod);
			}
		}

		Method readMethod = LawLibrary.class.getMethod("getBook");
		Method writeMethod = LawLibrary.class.getMethod("setBook", Book.class);

		System.out.println(format("read : %s.%s (bridge:%s)",
				readMethod.getDeclaringClass().getSimpleName(), readMethod.getName(), readMethod.isBridge()));
		System.out.println(format("write: %s.%s (bridge:%s)",
				writeMethod.getDeclaringClass().getSimpleName(), writeMethod.getName(), writeMethod.isBridge()));


		System.out.println("--------");
		for (Method m : LawLibrary.class.getMethods()) {
			if (m.getDeclaringClass() == Object.class) continue;
			System.out.println(format("%s %s.%s(%s) [bridge:%s]",
					m.getReturnType().getSimpleName(), m.getDeclaringClass().getSimpleName(),
					m.getName(),
					m.getParameterTypes().length == 1 ? m.getParameterTypes()[0].getSimpleName() : "",
					m.isBridge()));
		}

		//new ExtendedBeanInfo(info);
	}

	@Test
	public void reproSpr8806() throws IntrospectionException {
		BeanInfo bi = Introspector.getBeanInfo(LawLibrary.class);
		new ExtendedBeanInfo(bi); // throws
	}

	interface Book { }

	interface TextBook extends Book { }

	interface LawBook extends TextBook { }

	interface BookOperations {
		Book getBook();
		void setBook(Book book);
	}

	interface TextBookOperations extends BookOperations {
		TextBook getBook();
	}

	abstract class Library {
		public Book getBook() { return null; }
		public void setBook(Book book) { }
	}

	class LawLibrary extends Library implements TextBookOperations {
		public LawBook getBook() { return null; }
	}
}
