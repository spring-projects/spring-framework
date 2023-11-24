/*
 * Copyright 2002-2023 the original author or authors.
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.tests.sample.objects.TestObject;
import org.springframework.util.ReflectionUtils.MethodFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Arjen Poutsma
 */
class ReflectionUtilsTests {

	@Test
	void findField() {
		Field field = ReflectionUtils.findField(TestObjectSubclassWithPublicField.class, "publicField", String.class);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("publicField");
		assertThat(field.getType()).isEqualTo(String.class);
		assertThat(Modifier.isPublic(field.getModifiers())).as("Field should be public.").isTrue();

		field = ReflectionUtils.findField(TestObjectSubclassWithNewField.class, "prot", String.class);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("prot");
		assertThat(field.getType()).isEqualTo(String.class);
		assertThat(Modifier.isProtected(field.getModifiers())).as("Field should be protected.").isTrue();

		field = ReflectionUtils.findField(TestObjectSubclassWithNewField.class, "name", String.class);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("name");
		assertThat(field.getType()).isEqualTo(String.class);
		assertThat(Modifier.isPrivate(field.getModifiers())).as("Field should be private.").isTrue();
	}

	@Test
	void setField() {
		TestObjectSubclassWithNewField testBean = new TestObjectSubclassWithNewField();
		Field field = ReflectionUtils.findField(TestObjectSubclassWithNewField.class, "name", String.class);

		ReflectionUtils.makeAccessible(field);

		ReflectionUtils.setField(field, testBean, "FooBar");
		assertThat(testBean.getName()).isNotNull();
		assertThat(testBean.getName()).isEqualTo("FooBar");

		ReflectionUtils.setField(field, testBean, null);
		assertThat((Object) testBean.getName()).isNull();
	}

	@Test
	void invokeMethod() throws Exception {
		String rob = "Rob Harrop";

		TestObject bean = new TestObject();
		bean.setName(rob);

		Method getName = TestObject.class.getMethod("getName");
		Method setName = TestObject.class.getMethod("setName", String.class);

		Object name = ReflectionUtils.invokeMethod(getName, bean);
		assertThat(name).as("Incorrect name returned").isEqualTo(rob);

		String juergen = "Juergen Hoeller";
		ReflectionUtils.invokeMethod(setName, bean, juergen);
		assertThat(bean.getName()).as("Incorrect name set").isEqualTo(juergen);
	}

	@Test
	void declaresException() throws Exception {
		Method remoteExMethod = A.class.getDeclaredMethod("foo", Integer.class);
		assertThat(ReflectionUtils.declaresException(remoteExMethod, RemoteException.class)).isTrue();
		assertThat(ReflectionUtils.declaresException(remoteExMethod, ConnectException.class)).isTrue();
		assertThat(ReflectionUtils.declaresException(remoteExMethod, NoSuchMethodException.class)).isFalse();
		assertThat(ReflectionUtils.declaresException(remoteExMethod, Exception.class)).isFalse();

		Method illegalExMethod = B.class.getDeclaredMethod("bar", String.class);
		assertThat(ReflectionUtils.declaresException(illegalExMethod, IllegalArgumentException.class)).isTrue();
		assertThat(ReflectionUtils.declaresException(illegalExMethod, NumberFormatException.class)).isTrue();
		assertThat(ReflectionUtils.declaresException(illegalExMethod, IllegalStateException.class)).isFalse();
		assertThat(ReflectionUtils.declaresException(illegalExMethod, Exception.class)).isFalse();
	}

	@Test
	void copySrcToDestinationOfIncorrectClass() {
		TestObject src = new TestObject();
		String dest = new String();
		assertThatIllegalArgumentException().isThrownBy(() ->
				ReflectionUtils.shallowCopyFieldState(src, dest));
	}

	@Test
	void rejectsNullSrc() {
		TestObject src = null;
		String dest = new String();
		assertThatIllegalArgumentException().isThrownBy(() ->
				ReflectionUtils.shallowCopyFieldState(src, dest));
	}

	@Test
	void rejectsNullDest() {
		TestObject src = new TestObject();
		String dest = null;
		assertThatIllegalArgumentException().isThrownBy(() ->
				ReflectionUtils.shallowCopyFieldState(src, dest));
	}

	@Test
	void validCopy() {
		TestObject src = new TestObject();
		TestObject dest = new TestObject();
		testValidCopy(src, dest);
	}

	@Test
	void validCopyOnSubTypeWithNewField() {
		TestObjectSubclassWithNewField src = new TestObjectSubclassWithNewField();
		TestObjectSubclassWithNewField dest = new TestObjectSubclassWithNewField();
		src.magic = 11;

		// Will check inherited fields are copied
		testValidCopy(src, dest);

		// Check subclass fields were copied
		assertThat(dest.magic).isEqualTo(src.magic);
		assertThat(dest.prot).isEqualTo(src.prot);
	}

	@Test
	void validCopyToSubType() {
		TestObject src = new TestObject();
		TestObjectSubclassWithNewField dest = new TestObjectSubclassWithNewField();
		dest.magic = 11;
		testValidCopy(src, dest);
		// Should have left this one alone
		assertThat(dest.magic).isEqualTo(11);
	}

	@Test
	void validCopyToSubTypeWithFinalField() {
		TestObjectSubclassWithFinalField src = new TestObjectSubclassWithFinalField();
		TestObjectSubclassWithFinalField dest = new TestObjectSubclassWithFinalField();
		// Check that this doesn't fail due to attempt to assign final
		testValidCopy(src, dest);
	}

	private void testValidCopy(TestObject src, TestObject dest) {
		src.setName("freddie");
		src.setAge(15);
		src.setSpouse(new TestObject());
		assertThat(src.getAge()).isNotEqualTo(dest.getAge());

		ReflectionUtils.shallowCopyFieldState(src, dest);
		assertThat(dest.getAge()).isEqualTo(src.getAge());
		assertThat(dest.getSpouse()).isEqualTo(src.getSpouse());
	}

	@Test
	void doWithMethodsUsingProtectedFilter() {
		ListSavingMethodCallback mc = new ListSavingMethodCallback();
		ReflectionUtils.doWithMethods(TestObject.class, mc, method -> Modifier.isProtected(method.getModifiers()));
		assertThat(mc.getMethodNames())
			.hasSizeGreaterThanOrEqualTo(2)
			.as("Must find protected methods on Object").contains("clone", "finalize")
			.as("Public, not protected").doesNotContain("hashCode", "absquatulate");
	}

	@Test
	void doWithMethodsUsingUserDeclaredMethodsFilterStartingWithObject() {
		ListSavingMethodCallback mc = new ListSavingMethodCallback();
		ReflectionUtils.doWithMethods(Object.class, mc, ReflectionUtils.USER_DECLARED_METHODS);
		assertThat(mc.getMethodNames()).isEmpty();
	}

	@Test
	void doWithMethodsUsingUserDeclaredMethodsFilterStartingWithTestObject() {
		ListSavingMethodCallback mc = new ListSavingMethodCallback();
		ReflectionUtils.doWithMethods(TestObject.class, mc, ReflectionUtils.USER_DECLARED_METHODS);
		assertThat(mc.getMethodNames())
			.as("user declared methods").contains("absquatulate", "compareTo", "getName", "setName", "getAge", "setAge", "getSpouse", "setSpouse")
			.as("methods on Object").doesNotContain("equals", "hashCode", "toString", "clone", "finalize", "getClass", "notify", "notifyAll", "wait");
	}

	@Test
	void doWithMethodsUsingUserDeclaredMethodsComposedFilter() {
		ListSavingMethodCallback mc = new ListSavingMethodCallback();
		// "q" because both absquatulate() and equals() contain "q"
		MethodFilter isSetterMethodOrNameContainsQ = m -> m.getName().startsWith("set") || m.getName().contains("q");
		MethodFilter methodFilter = ReflectionUtils.USER_DECLARED_METHODS.and(isSetterMethodOrNameContainsQ);
		ReflectionUtils.doWithMethods(TestObject.class, mc, methodFilter);
		assertThat(mc.getMethodNames()).containsExactlyInAnyOrder("setName", "setAge", "setSpouse", "absquatulate");
	}

	@Test
	void doWithMethodsFindsDuplicatesInClassHierarchy() {
		ListSavingMethodCallback mc = new ListSavingMethodCallback();
		ReflectionUtils.doWithMethods(TestObjectSubclass.class, mc);
		assertThat(mc.getMethodNames().stream()).filteredOn("absquatulate"::equals).as("Found 2 absquatulates").hasSize(2);
	}

	@Test
	void findMethod() {
		assertThat(ReflectionUtils.findMethod(B.class, "bar", String.class)).isNotNull();
		assertThat(ReflectionUtils.findMethod(B.class, "foo", Integer.class)).isNotNull();
		assertThat(ReflectionUtils.findMethod(B.class, "getClass")).isNotNull();
	}

	@Test
	void findMethodWithVarArgs() {
		assertThat(ReflectionUtils.findMethod(B.class, "add", int[].class)).isNotNull();
	}

	@Test
	void isCglibRenamedMethod() throws SecurityException, NoSuchMethodException {
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
		assertThat(ReflectionUtils.isCglibRenamedMethod(C.class.getMethod("CGLIB$m1$123"))).isTrue();
		assertThat(ReflectionUtils.isCglibRenamedMethod(C.class.getMethod("CGLIB$m1$0"))).isTrue();
		assertThat(ReflectionUtils.isCglibRenamedMethod(C.class.getMethod("CGLIB$$0"))).isFalse();
		assertThat(ReflectionUtils.isCglibRenamedMethod(C.class.getMethod("CGLIB$m1$"))).isFalse();
		assertThat(ReflectionUtils.isCglibRenamedMethod(C.class.getMethod("CGLIB$m1"))).isFalse();
		assertThat(ReflectionUtils.isCglibRenamedMethod(C.class.getMethod("m1"))).isFalse();
		assertThat(ReflectionUtils.isCglibRenamedMethod(C.class.getMethod("m1$"))).isFalse();
		assertThat(ReflectionUtils.isCglibRenamedMethod(C.class.getMethod("m1$1"))).isFalse();
	}

	@Test
	void getAllDeclaredMethods() {
		class Foo {
			@Override
			public String toString() {
				return super.toString();
			}
		}
		Method[] allDeclaredMethods = ReflectionUtils.getAllDeclaredMethods(Foo.class);
		assertThat(allDeclaredMethods).extracting(Method::getName).filteredOn("toString"::equals).hasSize(2);
	}

	@Test
	void getUniqueDeclaredMethods() {
		class Foo {
			@Override
			public String toString() {
				return super.toString();
			}
		}
		Method[] uniqueDeclaredMethods = ReflectionUtils.getUniqueDeclaredMethods(Foo.class);
		assertThat(uniqueDeclaredMethods).extracting(Method::getName).filteredOn("toString"::equals).hasSize(1);
	}

	@Test
	void getUniqueDeclaredMethods_withCovariantReturnType() throws Exception {
		class Parent {
			@SuppressWarnings("unused")
			public Number m1() {
				return 42;
			}
		}
		class Leaf extends Parent {
			@Override
			public Integer m1() {
				return 42;
			}
		}
		Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(Leaf.class);
		assertThat(methods).extracting(Method::getName).filteredOn("m1"::equals).hasSize(1);
		assertThat(methods).contains(Leaf.class.getMethod("m1"));
		assertThat(methods).doesNotContain(Parent.class.getMethod("m1"));
	}

	@Test
	void getDeclaredMethodsReturnsCopy() {
		Method[] m1 = ReflectionUtils.getDeclaredMethods(A.class);
		Method[] m2 = ReflectionUtils.getDeclaredMethods(A.class);
		assertThat(m1). isNotSameAs(m2);
	}

	private static class ListSavingMethodCallback implements ReflectionUtils.MethodCallback {

		private List<String> methodNames = new ArrayList<>();

		private List<Method> methods = new ArrayList<>();

		@Override
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

	private static class TestObjectSubclass extends TestObject {

		@Override
		public void absquatulate() {
			throw new UnsupportedOperationException();
		}
	}

	private static class TestObjectSubclassWithPublicField extends TestObject {

		@SuppressWarnings("unused")
		public String publicField = "foo";
	}

	private static class TestObjectSubclassWithNewField extends TestObject {

		private int magic;

		protected String prot = "foo";
	}

	private static class TestObjectSubclassWithFinalField extends TestObject {

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
			for (int arg : args) {
				sum += arg;
			}
			return sum;
		}
	}

}
