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

package org.springframework.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.TestBean;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Arjen Poutsma
 */
public class ReflectionUtilsTests {

	@Test
	public void findField() {
		Field field = ReflectionUtils.findField(TestBeanSubclassWithPublicField.class, "publicField", String.class);
		assertNotNull(field);
		assertEquals("publicField", field.getName());
		assertEquals(String.class, field.getType());
		assertTrue("Field should be public.", Modifier.isPublic(field.getModifiers()));

		field = ReflectionUtils.findField(TestBeanSubclassWithNewField.class, "prot", String.class);
		assertNotNull(field);
		assertEquals("prot", field.getName());
		assertEquals(String.class, field.getType());
		assertTrue("Field should be protected.", Modifier.isProtected(field.getModifiers()));

		field = ReflectionUtils.findField(TestBeanSubclassWithNewField.class, "name", String.class);
		assertNotNull(field);
		assertEquals("name", field.getName());
		assertEquals(String.class, field.getType());
		assertTrue("Field should be private.", Modifier.isPrivate(field.getModifiers()));
	}

	@Test
	public void setField() {
		final TestBeanSubclassWithNewField testBean = new TestBeanSubclassWithNewField();
		final Field field = ReflectionUtils.findField(TestBeanSubclassWithNewField.class, "name", String.class);

		ReflectionUtils.makeAccessible(field);

		ReflectionUtils.setField(field, testBean, "FooBar");
		assertNotNull(testBean.getName());
		assertEquals("FooBar", testBean.getName());

		ReflectionUtils.setField(field, testBean, null);
		assertNull(testBean.getName());
	}

	@Test(expected = IllegalStateException.class)
	public void setFieldIllegal() {
		final TestBeanSubclassWithNewField testBean = new TestBeanSubclassWithNewField();
		final Field field = ReflectionUtils.findField(TestBeanSubclassWithNewField.class, "name", String.class);
		ReflectionUtils.setField(field, testBean, "FooBar");
	}

	@Test
	public void invokeMethod() throws Exception {
		String rob = "Rob Harrop";

		TestBean bean = new TestBean();
		bean.setName(rob);

		Method getName = TestBean.class.getMethod("getName", (Class[]) null);
		Method setName = TestBean.class.getMethod("setName", new Class[] { String.class });

		Object name = ReflectionUtils.invokeMethod(getName, bean);
		assertEquals("Incorrect name returned", rob, name);

		String juergen = "Juergen Hoeller";
		ReflectionUtils.invokeMethod(setName, bean, new Object[] { juergen });
		assertEquals("Incorrect name set", juergen, bean.getName());
	}

	@Test
	public void declaresException() throws Exception {
		Method remoteExMethod = A.class.getDeclaredMethod("foo", new Class[] { Integer.class });
		assertTrue(ReflectionUtils.declaresException(remoteExMethod, RemoteException.class));
		assertTrue(ReflectionUtils.declaresException(remoteExMethod, ConnectException.class));
		assertFalse(ReflectionUtils.declaresException(remoteExMethod, NoSuchMethodException.class));
		assertFalse(ReflectionUtils.declaresException(remoteExMethod, Exception.class));

		Method illegalExMethod = B.class.getDeclaredMethod("bar", new Class[] { String.class });
		assertTrue(ReflectionUtils.declaresException(illegalExMethod, IllegalArgumentException.class));
		assertTrue(ReflectionUtils.declaresException(illegalExMethod, NumberFormatException.class));
		assertFalse(ReflectionUtils.declaresException(illegalExMethod, IllegalStateException.class));
		assertFalse(ReflectionUtils.declaresException(illegalExMethod, Exception.class));
	}

	@Test
	public void copySrcToDestinationOfIncorrectClass() {
		TestBean src = new TestBean();
		String dest = new String();
		try {
			ReflectionUtils.shallowCopyFieldState(src, dest);
			fail();
		} catch (IllegalArgumentException ex) {
			// Ok
		}
	}

	@Test
	public void rejectsNullSrc() {
		TestBean src = null;
		String dest = new String();
		try {
			ReflectionUtils.shallowCopyFieldState(src, dest);
			fail();
		} catch (IllegalArgumentException ex) {
			// Ok
		}
	}

	@Test
	public void rejectsNullDest() {
		TestBean src = new TestBean();
		String dest = null;
		try {
			ReflectionUtils.shallowCopyFieldState(src, dest);
			fail();
		} catch (IllegalArgumentException ex) {
			// Ok
		}
	}

	@Test
	public void validCopy() {
		TestBean src = new TestBean();
		TestBean dest = new TestBean();
		testValidCopy(src, dest);
	}

	@Test
	public void validCopyOnSubTypeWithNewField() {
		TestBeanSubclassWithNewField src = new TestBeanSubclassWithNewField();
		TestBeanSubclassWithNewField dest = new TestBeanSubclassWithNewField();
		src.magic = 11;

		// Will check inherited fields are copied
		testValidCopy(src, dest);

		// Check subclass fields were copied
		assertEquals(src.magic, dest.magic);
		assertEquals(src.prot, dest.prot);
	}

	@Test
	public void validCopyToSubType() {
		TestBean src = new TestBean();
		TestBeanSubclassWithNewField dest = new TestBeanSubclassWithNewField();
		dest.magic = 11;
		testValidCopy(src, dest);
		// Should have left this one alone
		assertEquals(11, dest.magic);
	}

	@Test
	public void validCopyToSubTypeWithFinalField() {
		TestBeanSubclassWithFinalField src = new TestBeanSubclassWithFinalField();
		TestBeanSubclassWithFinalField dest = new TestBeanSubclassWithFinalField();
		// Check that this doesn't fail due to attempt to assign final
		testValidCopy(src, dest);
	}

	private void testValidCopy(TestBean src, TestBean dest) {
		src.setName("freddie");
		src.setAge(15);
		src.setSpouse(new TestBean());
		assertFalse(src.getAge() == dest.getAge());

		ReflectionUtils.shallowCopyFieldState(src, dest);
		assertEquals(src.getAge(), dest.getAge());
		assertEquals(src.getSpouse(), dest.getSpouse());
		assertEquals(src.getDoctor(), dest.getDoctor());
	}

	@Test
	public void doWithProtectedMethods() {
		ListSavingMethodCallback mc = new ListSavingMethodCallback();
		ReflectionUtils.doWithMethods(TestBean.class, mc, new ReflectionUtils.MethodFilter() {
			public boolean matches(Method m) {
				return Modifier.isProtected(m.getModifiers());
			}
		});
		assertFalse(mc.getMethodNames().isEmpty());
		assertTrue("Must find protected method on Object", mc.getMethodNames().contains("clone"));
		assertTrue("Must find protected method on Object", mc.getMethodNames().contains("finalize"));
		assertFalse("Public, not protected", mc.getMethodNames().contains("hashCode"));
		assertFalse("Public, not protected", mc.getMethodNames().contains("absquatulate"));
	}

	@Test
	public void duplicatesFound() {
		ListSavingMethodCallback mc = new ListSavingMethodCallback();
		ReflectionUtils.doWithMethods(TestBeanSubclass.class, mc);
		int absquatulateCount = 0;
		for (String name : mc.getMethodNames()) {
			if (name.equals("absquatulate")) {
				++absquatulateCount;
			}
		}
		assertEquals("Found 2 absquatulates", 2, absquatulateCount);
	}

	@Test
	public void findMethod() throws Exception {
		assertNotNull(ReflectionUtils.findMethod(B.class, "bar", String.class));
		assertNotNull(ReflectionUtils.findMethod(B.class, "foo", Integer.class));
		assertNotNull(ReflectionUtils.findMethod(B.class, "getClass"));
	}

	@Ignore("[SPR-8644] findMethod() does not currently support var-args")
	@Test
	public void findMethodWithVarArgs() throws Exception {
		assertNotNull(ReflectionUtils.findMethod(B.class, "add", int.class, int.class, int.class));
	}

	@Test
	public void isCglibRenamedMethod() throws SecurityException, NoSuchMethodException {
		@SuppressWarnings("unused")
		class C {
			public void CGLIB$m1$123() {
			}

			public void CGLIB$m1$0() {
			}

			public void CGLIB$$0() {
			}

			public void CGLIB$m1$() {
			}

			public void CGLIB$m1() {
			}

			public void m1() {
			}

			public void m1$() {
			}

			public void m1$1() {
			}
		}
		assertTrue(ReflectionUtils.isCglibRenamedMethod(C.class.getMethod("CGLIB$m1$123")));
		assertTrue(ReflectionUtils.isCglibRenamedMethod(C.class.getMethod("CGLIB$m1$0")));
		assertFalse(ReflectionUtils.isCglibRenamedMethod(C.class.getMethod("CGLIB$$0")));
		assertFalse(ReflectionUtils.isCglibRenamedMethod(C.class.getMethod("CGLIB$m1$")));
		assertFalse(ReflectionUtils.isCglibRenamedMethod(C.class.getMethod("CGLIB$m1")));
		assertFalse(ReflectionUtils.isCglibRenamedMethod(C.class.getMethod("m1")));
		assertFalse(ReflectionUtils.isCglibRenamedMethod(C.class.getMethod("m1$")));
		assertFalse(ReflectionUtils.isCglibRenamedMethod(C.class.getMethod("m1$1")));
	}

	@Test
	public void getAllDeclaredMethods() throws Exception {
		class Foo {
			@Override
			public String toString() {
				return super.toString();
			}
		}
		int toStringMethodCount = 0;
		for (Method method : ReflectionUtils.getAllDeclaredMethods(Foo.class)) {
			if (method.getName().equals("toString")) {
				toStringMethodCount++;
			}
		}
		assertThat(toStringMethodCount, is(2));
	}

	@Test
	public void getUniqueDeclaredMethods() throws Exception {
		class Foo {
			@Override
			public String toString() {
				return super.toString();
			}
		}
		int toStringMethodCount = 0;
		for (Method method : ReflectionUtils.getUniqueDeclaredMethods(Foo.class)) {
			if (method.getName().equals("toString")) {
				toStringMethodCount++;
			}
		}
		assertThat(toStringMethodCount, is(1));
	}

	@Test
	public void getUniqueDeclaredMethods_withCovariantReturnType() throws Exception {
		class Parent {
			@SuppressWarnings("unused")
			public Number m1() {
				return new Integer(42);
			}
		}
		class Leaf extends Parent {
			@Override
			public Integer m1() {
				return new Integer(42);
			}
		}
		int m1MethodCount = 0;
		Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(Leaf.class);
		for (Method method : methods) {
			if (method.getName().equals("m1")) {
				m1MethodCount++;
			}
		}
		assertThat(m1MethodCount, is(1));
		assertTrue(ObjectUtils.containsElement(methods, Leaf.class.getMethod("m1")));
		assertFalse(ObjectUtils.containsElement(methods, Parent.class.getMethod("m1")));
	}

	private static class ListSavingMethodCallback implements ReflectionUtils.MethodCallback {

		private List<String> methodNames = new LinkedList<String>();

		private List<Method> methods = new LinkedList<Method>();

		public void doWith(Method m) throws IllegalArgumentException, IllegalAccessException {
			this.methodNames.add(m.getName());
			this.methods.add(m);
		}

		public List<String> getMethodNames() {
			return this.methodNames;
		}

		@SuppressWarnings("unused")
		public List<Method> getMethods() {
			return this.methods;
		}
	}

	private static class TestBeanSubclass extends TestBean {

		@Override
		public void absquatulate() {
			throw new UnsupportedOperationException();
		}
	}

	private static class TestBeanSubclassWithPublicField extends TestBean {

		@SuppressWarnings("unused")
		public String publicField = "foo";
	}

	private static class TestBeanSubclassWithNewField extends TestBean {

		private int magic;

		protected String prot = "foo";
	}

	private static class TestBeanSubclassWithFinalField extends TestBean {

		@SuppressWarnings("unused")
		private final String foo = "will break naive copy that doesn't exclude statics";
	}

	private static class A {

		@SuppressWarnings("unused")
		private void foo(Integer i) throws RemoteException {
		}
	}

	@SuppressWarnings("unused")
	private static class B extends A {

		void bar(String s) throws IllegalArgumentException {
		}

		int add(int... args) {
			int sum = 0;
			for (int i = 0; i < args.length; i++) {
				sum += args[i];
			}
			return sum;
		}
	}

}
