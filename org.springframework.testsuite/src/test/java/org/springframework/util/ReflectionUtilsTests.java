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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.beans.TestBean;
import org.springframework.test.AssertThrows;

/**
 * <p>
 * JUnit 3.8 based unit tests for {@link ReflectionUtils}.
 * </p>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class ReflectionUtilsTests extends TestCase {

	public void testFindField() {
		Field field;

		field = ReflectionUtils.findField(TestBeanSubclassWithPublicField.class, "publicField", String.class);
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

	public void testSetField() {
		final TestBeanSubclassWithNewField testBean = new TestBeanSubclassWithNewField();
		final Field field = ReflectionUtils.findField(TestBeanSubclassWithNewField.class, "name", String.class);

		new AssertThrows(IllegalStateException.class,
				"Calling setField() with on a private field without making it accessible should throw an IllegalStateException.") {

			public void test() throws Exception {
				ReflectionUtils.setField(field, testBean, "FooBar");
			}
		}.runTest();

		ReflectionUtils.makeAccessible(field);

		ReflectionUtils.setField(field, testBean, "FooBar");
		assertNotNull(testBean.getName());
		assertEquals("FooBar", testBean.getName());

		ReflectionUtils.setField(field, testBean, null);
		assertNull(testBean.getName());
	}


	public void testInvokeMethod() throws Exception {
		String rob = "Rob Harrop";
		String juergen = "Juergen Hoeller";

		TestBean bean = new TestBean();
		bean.setName(rob);

		Method getName = TestBean.class.getMethod("getName", (Class[]) null);
		Method setName = TestBean.class.getMethod("setName", new Class[] { String.class });

		Object name = ReflectionUtils.invokeMethod(getName, bean);
		assertEquals("Incorrect name returned", rob, name);

		ReflectionUtils.invokeMethod(setName, bean, new Object[] { juergen });
		assertEquals("Incorrect name set", juergen, bean.getName());
	}

	public void testDeclaresException() throws Exception {
		Method remoteExMethod = A.class.getDeclaredMethod("foo", new Class[] {Integer.class});
		assertTrue(ReflectionUtils.declaresException(remoteExMethod, RemoteException.class));
		assertTrue(ReflectionUtils.declaresException(remoteExMethod, ConnectException.class));
		assertFalse(ReflectionUtils.declaresException(remoteExMethod, NoSuchMethodException.class));
		assertFalse(ReflectionUtils.declaresException(remoteExMethod, Exception.class));

		Method illegalExMethod = B.class.getDeclaredMethod("bar", new Class[] {String.class});
		assertTrue(ReflectionUtils.declaresException(illegalExMethod, IllegalArgumentException.class));
		assertTrue(ReflectionUtils.declaresException(illegalExMethod, NumberFormatException.class));
		assertFalse(ReflectionUtils.declaresException(illegalExMethod, IllegalStateException.class));
		assertFalse(ReflectionUtils.declaresException(illegalExMethod, Exception.class));
	}

	public void testCopySrcToDestinationOfIncorrectClass() {
		TestBean src = new TestBean();
		String dest = new String();
		try {
			ReflectionUtils.shallowCopyFieldState(src, dest);
			fail();
		}
		catch (IllegalArgumentException ex) {
			// Ok
		}
	}

	public void testRejectsNullSrc() {
		TestBean src = null;
		String dest = new String();
		try {
			ReflectionUtils.shallowCopyFieldState(src, dest);
			fail();
		}
		catch (IllegalArgumentException ex) {
			// Ok
		}
	}

	public void testRejectsNullDest() {
		TestBean src = new TestBean();
		String dest = null;
		try {
			ReflectionUtils.shallowCopyFieldState(src, dest);
			fail();
		}
		catch (IllegalArgumentException ex) {
			// Ok
		}
	}

	public void testValidCopy() {
		TestBean src = new TestBean();
		TestBean dest = new TestBean();
		testValidCopy(src, dest);
	}

	public void testValidCopyOnSubTypeWithNewField() {
		TestBeanSubclassWithNewField src = new TestBeanSubclassWithNewField();
		TestBeanSubclassWithNewField dest = new TestBeanSubclassWithNewField();
		src.magic = 11;

		// Will check inherited fields are copied
		testValidCopy(src, dest);

		// Check subclass fields were copied
		assertEquals(src.magic, dest.magic);
		assertEquals(src.prot, dest.prot);
	}

	public void testValidCopyToSubType() {
		TestBean src = new TestBean();
		TestBeanSubclassWithNewField dest = new TestBeanSubclassWithNewField();
		dest.magic = 11;
		testValidCopy(src, dest);
		// Should have left this one alone
		assertEquals(11, dest.magic);
	}

	public void testValidCopyToSubTypeWithFinalField() {
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

	static class ListSavingMethodCallback implements ReflectionUtils.MethodCallback {
		private List methodNames = new LinkedList();

		private List methods = new LinkedList();

		public void doWith(Method m) throws IllegalArgumentException, IllegalAccessException {
			this.methodNames.add(m.getName());
			this.methods.add(m);
		}

		public List getMethodNames() {
			return this.methodNames;
		}

		public List getMethods() {
			return this.methods;
		}
	};

	public void testDoWithProtectedMethods() {
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

	public static class TestBeanSubclass extends TestBean {
		public void absquatulate() {
			throw new UnsupportedOperationException();
		}
	}

	public void testDuplicatesFound() {
		ListSavingMethodCallback mc = new ListSavingMethodCallback();
		ReflectionUtils.doWithMethods(TestBeanSubclass.class, mc);
		int absquatulateCount = 0;
		for (Iterator it = mc.getMethodNames().iterator(); it.hasNext();) {
			String name = (String) it.next();
			if (name.equals("absquatulate")) {
				++absquatulateCount;
			}
		}
		assertEquals("Found 2 absquatulates", 2, absquatulateCount);
	}

	public void testFindMethod() throws Exception {
	  assertNotNull(ReflectionUtils.findMethod(B.class, "bar", new Class[]{String.class}));
	  assertNotNull(ReflectionUtils.findMethod(B.class, "foo", new Class[]{Integer.class}));
	}


	public static class TestBeanSubclassWithPublicField extends TestBean {
		public String publicField = "foo";
	}

	public static class TestBeanSubclassWithNewField extends TestBean {
		private int magic;
		protected String prot = "foo";
	}


	public static class TestBeanSubclassWithFinalField extends TestBean {
		private final String foo = "will break naive copy that doesn't exclude statics";
	}


	private static class A {
		private void foo(Integer i) throws RemoteException {}
	}


	private static class B extends A {
		void bar(String s) throws IllegalArgumentException {}
	}

}
