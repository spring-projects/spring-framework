/*
 * Copyright 2002-2020 the original author or authors.
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.core.testfixture.EnabledForTestGroups;
import org.springframework.tests.sample.objects.TestObject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.core.testfixture.TestGroup.PERFORMANCE;

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
		assertThat(src.getAge() == dest.getAge()).isFalse();

		ReflectionUtils.shallowCopyFieldState(src, dest);
		assertThat(dest.getAge()).isEqualTo(src.getAge());
		assertThat(dest.getSpouse()).isEqualTo(src.getSpouse());
	}

	@Test
	void doWithProtectedMethods() {
		ListSavingMethodCallback mc = new ListSavingMethodCallback();
		ReflectionUtils.doWithMethods(TestObject.class, mc, new ReflectionUtils.MethodFilter() {
			@Override
			public boolean matches(Method m) {
				return Modifier.isProtected(m.getModifiers());
			}
		});
		assertThat(mc.getMethodNames().isEmpty()).isFalse();
		assertThat(mc.getMethodNames().contains("clone")).as("Must find protected method on Object").isTrue();
		assertThat(mc.getMethodNames().contains("finalize")).as("Must find protected method on Object").isTrue();
		assertThat(mc.getMethodNames().contains("hashCode")).as("Public, not protected").isFalse();
		assertThat(mc.getMethodNames().contains("absquatulate")).as("Public, not protected").isFalse();
	}

	@Test
	void duplicatesFound() {
		ListSavingMethodCallback mc = new ListSavingMethodCallback();
		ReflectionUtils.doWithMethods(TestObjectSubclass.class, mc);
		int absquatulateCount = 0;
		for (String name : mc.getMethodNames()) {
			if (name.equals("absquatulate")) {
				++absquatulateCount;
			}
		}
		assertThat(absquatulateCount).as("Found 2 absquatulates").isEqualTo(2);
	}

	@Test
	void findMethod() throws Exception {
		assertThat(ReflectionUtils.findMethod(B.class, "bar", String.class)).isNotNull();
		assertThat(ReflectionUtils.findMethod(B.class, "foo", Integer.class)).isNotNull();
		assertThat(ReflectionUtils.findMethod(B.class, "getClass")).isNotNull();
	}

	@Disabled("[SPR-8644] findMethod() does not currently support var-args")
	@Test
	void findMethodWithVarArgs() throws Exception {
		assertThat(ReflectionUtils.findMethod(B.class, "add", int.class, int.class, int.class)).isNotNull();
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
	void getAllDeclaredMethods() throws Exception {
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
		assertThat(toStringMethodCount).isEqualTo(2);
	}

	@Test
	void getUniqueDeclaredMethods() throws Exception {
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
		assertThat(toStringMethodCount).isEqualTo(1);
	}

	@Test
	void getUniqueDeclaredMethods_withCovariantReturnType() throws Exception {
		class Parent {
			@SuppressWarnings("unused")
			public Number m1() {
				return Integer.valueOf(42);
			}
		}
		class Leaf extends Parent {
			@Override
			public Integer m1() {
				return Integer.valueOf(42);
			}
		}
		int m1MethodCount = 0;
		Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(Leaf.class);
		for (Method method : methods) {
			if (method.getName().equals("m1")) {
				m1MethodCount++;
			}
		}
		assertThat(m1MethodCount).isEqualTo(1);
		assertThat(ObjectUtils.containsElement(methods, Leaf.class.getMethod("m1"))).isTrue();
		assertThat(ObjectUtils.containsElement(methods, Parent.class.getMethod("m1"))).isFalse();
	}

	@Test
	@EnabledForTestGroups(PERFORMANCE)
	void getUniqueDeclaredMethods_isFastEnough() {
		@SuppressWarnings("unused")
		class C {
			void m00() { } void m01() { } void m02() { } void m03() { } void m04() { }
			void m05() { } void m06() { } void m07() { } void m08() { } void m09() { }
			void m10() { } void m11() { } void m12() { } void m13() { } void m14() { }
			void m15() { } void m16() { } void m17() { } void m18() { } void m19() { }
			void m20() { } void m21() { } void m22() { } void m23() { } void m24() { }
			void m25() { } void m26() { } void m27() { } void m28() { } void m29() { }
			void m30() { } void m31() { } void m32() { } void m33() { } void m34() { }
			void m35() { } void m36() { } void m37() { } void m38() { } void m39() { }
			void m40() { } void m41() { } void m42() { } void m43() { } void m44() { }
			void m45() { } void m46() { } void m47() { } void m48() { } void m49() { }
			void m50() { } void m51() { } void m52() { } void m53() { } void m54() { }
			void m55() { } void m56() { } void m57() { } void m58() { } void m59() { }
			void m60() { } void m61() { } void m62() { } void m63() { } void m64() { }
			void m65() { } void m66() { } void m67() { } void m68() { } void m69() { }
			void m70() { } void m71() { } void m72() { } void m73() { } void m74() { }
			void m75() { } void m76() { } void m77() { } void m78() { } void m79() { }
			void m80() { } void m81() { } void m82() { } void m83() { } void m84() { }
			void m85() { } void m86() { } void m87() { } void m88() { } void m89() { }
			void m90() { } void m91() { } void m92() { } void m93() { } void m94() { }
			void m95() { } void m96() { } void m97() { } void m98() { } void m99() { }
		}

		StopWatch sw = new StopWatch();
		sw.start();
		Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(C.class);
		sw.stop();
		long totalMs = sw.getTotalTimeMillis();
		assertThat(methods.length).isGreaterThan(100);
		assertThat(totalMs).isLessThan(10L);
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
			for (int i = 0; i < args.length; i++) {
				sum += args[i];
			}
			return sum;
		}
	}

}
