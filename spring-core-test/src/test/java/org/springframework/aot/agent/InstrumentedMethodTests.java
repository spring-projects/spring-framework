/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.aot.agent;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InstrumentedMethod}.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 */
class InstrumentedMethodTests {

	private final RuntimeHints hints = new RuntimeHints();


	@Nested
	class ClassReflectionInstrumentationTests {

		final RecordedInvocation stringGetClasses = RecordedInvocation.of(InstrumentedMethod.CLASS_GETCLASSES)
				.onInstance(String.class).returnValue(String.class.getClasses()).build();

		final RecordedInvocation stringGetDeclaredClasses = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDCLASSES)
				.onInstance(String.class).returnValue(String.class.getDeclaredClasses()).build();

		@Test
		void classForNameShouldMatchReflectionOnType() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_FORNAME)
					.withArgument("java.lang.String").returnValue(String.class).build();
			hints.reflection().registerType(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_FORNAME, invocation);
		}

		@Test
		void classForNameShouldNotMatchWhenMissingReflectionOnType() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_FORNAME)
					.withArgument("java.lang.String").returnValue(String.class).build();
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_FORNAME, invocation);
		}

		@Test
		void classGetClassesShouldMatchReflectionOnType() {
			hints.reflection().registerType(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCLASSES, this.stringGetClasses);
		}

		@Test
		void classGetDeclaredClassesShouldMatchReflectionOnType() {
			hints.reflection().registerType(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDCLASSES, this.stringGetDeclaredClasses);
		}

		@Test
		void classGetDeclaredClassesShouldNotMatchWhenMissingReflectionOnType() {
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDCLASSES, this.stringGetDeclaredClasses);
		}

		@Test
		void classLoaderLoadClassShouldMatchReflectionHintOnType() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASSLOADER_LOADCLASS)
					.onInstance(ClassLoader.getSystemClassLoader())
					.withArgument(PublicField.class.getCanonicalName()).returnValue(PublicField.class).build();
			hints.reflection().registerType(PublicField.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASSLOADER_LOADCLASS, invocation);
		}

	}

	@Nested
	class ConstructorReflectionInstrumentationTests {

		RecordedInvocation stringGetConstructor;

		final RecordedInvocation stringGetConstructors = RecordedInvocation.of(InstrumentedMethod.CLASS_GETCONSTRUCTORS)
				.onInstance(String.class).returnValue(String.class.getConstructors()).build();

		RecordedInvocation stringGetDeclaredConstructor;

		final RecordedInvocation stringGetDeclaredConstructors = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTORS)
				.onInstance(String.class).returnValue(String.class.getDeclaredConstructors()).build();

		@BeforeEach
		public void setup() throws Exception {
			this.stringGetConstructor = RecordedInvocation.of(InstrumentedMethod.CLASS_GETCONSTRUCTOR)
					.onInstance(String.class).withArgument(new Class[0]).returnValue(String.class.getConstructor()).build();
			this.stringGetDeclaredConstructor = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTOR)
					.onInstance(String.class).withArgument(new Class[] { byte[].class, byte.class })
					.returnValue(String.class.getDeclaredConstructor(byte[].class, byte.class)).build();
		}

		@Test
		void classGetConstructorShouldMatchTypeHint() {
			hints.reflection().registerType(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTOR, this.stringGetConstructor);
		}

		@Test
		void classGetConstructorShouldNotMatchWhenMissingTypeHint() {
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETCONSTRUCTOR, this.stringGetConstructor);
		}

		@Test
		void classGetConstructorShouldMatchInvokePublicConstructorsHint() {
			hints.reflection().registerType(String.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTOR, this.stringGetConstructor);
		}

		@Test
		void classGetConstructorShouldMatchInvokeDeclaredConstructorsHint() {
			hints.reflection().registerType(String.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTOR, this.stringGetConstructor);
		}

		@Test
		void classGetConstructorShouldMatchInvokeConstructorHint() {
			hints.reflection().registerType(String.class, typeHint ->
					typeHint.withConstructor(Collections.emptyList(), ExecutableMode.INVOKE));
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTOR, this.stringGetConstructor);
		}

		@Test
		void classGetConstructorsShouldMatchTypeHint() {
			hints.reflection().registerType(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTORS, this.stringGetConstructors);
		}

		@Test
		void classGetConstructorsShouldNotMatchWhemMissingTypeHint() {
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETCONSTRUCTORS, this.stringGetConstructors);
		}

		@Test
		void classGetConstructorsShouldMatchInvokePublicConstructorsHint() {
			hints.reflection().registerType(String.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTORS, this.stringGetConstructors);
		}

		@Test
		void classGetConstructorsShouldMatchInvokeDeclaredConstructorsHint() {
			hints.reflection().registerType(String.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTORS, this.stringGetConstructors);
		}

		@Test
		void classGetDeclaredConstructorShouldMatchTypeHint() {
			hints.reflection().registerType(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTOR, this.stringGetDeclaredConstructor);
		}

		@Test
		void classGetDeclaredConstructorShouldNotMatchWhenMissingTypeHint() {
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTOR, this.stringGetDeclaredConstructor);
		}

		@Test
		void classGetDeclaredConstructorShouldMatchInvokeConstructorHint() {
			hints.reflection().registerType(String.class, typeHint ->
					typeHint.withConstructor(TypeReference.listOf(byte[].class, byte.class), ExecutableMode.INVOKE));
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTOR, this.stringGetDeclaredConstructor);
		}

		@Test
		void classGetDeclaredConstructorsShouldMatchInvokeDeclaredConstructorsHint() {
			hints.reflection().registerType(String.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTORS, this.stringGetDeclaredConstructors);
		}

		@Test
		void classGetDeclaredConstructorsShouldMatchTypeReflectionHint() {
			hints.reflection().registerType(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTORS, this.stringGetDeclaredConstructors);
		}

		@Test
		void classGetDeclaredConstructorsShouldNotMatchWhenMissingTypeReflectionHint() {
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTORS, this.stringGetDeclaredConstructors);
		}

		@Test
		void constructorNewInstanceShouldMatchInvokeHintOnConstructor() throws NoSuchMethodException {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CONSTRUCTOR_NEWINSTANCE)
					.onInstance(String.class.getConstructor()).returnValue("").build();
			hints.reflection().registerType(String.class, typeHint ->
					typeHint.withConstructor(Collections.emptyList(), ExecutableMode.INVOKE));
			assertThatInvocationMatches(InstrumentedMethod.CONSTRUCTOR_NEWINSTANCE, invocation);
		}

	}

	@Nested
	class MethodReflectionInstrumentationTests {

		RecordedInvocation stringGetToStringMethod;

		RecordedInvocation stringGetScaleMethod;

		RecordedInvocation stringGetMethods;

		@BeforeEach
		void setup() throws Exception {
			this.stringGetToStringMethod = RecordedInvocation.of(InstrumentedMethod.CLASS_GETMETHOD)
					.onInstance(String.class).withArguments("toString", new Class[0])
					.returnValue(String.class.getMethod("toString")).build();
			this.stringGetScaleMethod = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDMETHOD)
					.onInstance(String.class).withArguments("scale", new Class[] { int.class, float.class })
					.returnValue(String.class.getDeclaredMethod("scale", int.class, float.class)).build();
			this.stringGetMethods = RecordedInvocation.of(InstrumentedMethod.CLASS_GETMETHODS)
					.onInstance(String.class).returnValue(String.class.getMethods()).build();
		}

		@Test
		void classGetDeclaredMethodShouldMatchTypeReflectionHint() {
			hints.reflection().registerType(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDMETHOD, this.stringGetScaleMethod);
		}

		@Test
		void classGetDeclaredMethodShouldMatchInvokeMethodHint() {
			List<TypeReference> parameterTypes = TypeReference.listOf(int.class, float.class);
			hints.reflection().registerType(String.class, typeHint ->
					typeHint.withMethod("scale", parameterTypes, ExecutableMode.INVOKE));
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDMETHOD, this.stringGetScaleMethod);
		}

		@Test
		void classGetDeclaredMethodsShouldMatchInvokeDeclaredMethodsHint() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDMETHODS).onInstance(String.class).build();
			hints.reflection().registerType(String.class, MemberCategory.INVOKE_DECLARED_METHODS);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDMETHODS, invocation);
		}

		@Test
		void classGetMethodsShouldMatchTypeReflectionHint() {
			hints.reflection().registerType(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHODS, this.stringGetMethods);
		}

		@Test
		void classGetMethodsShouldMatchInvokeDeclaredMethodsHint() {
			hints.reflection().registerType(String.class, MemberCategory.INVOKE_DECLARED_METHODS);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHODS, this.stringGetMethods);
		}

		@Test
		void classGetMethodsShouldMatchInvokePublicMethodsHint() {
			hints.reflection().registerType(String.class, MemberCategory.INVOKE_PUBLIC_METHODS);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHODS, this.stringGetMethods);
		}

		@Test
		void classGetMethodsShouldNotMatchForWrongType() {
			hints.reflection().registerType(Integer.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETMETHODS, this.stringGetMethods);
		}

		@Test
		void classGetMethodShouldMatchReflectionTypeHint() {
			hints.reflection().registerType(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetToStringMethod);
		}

		@Test
		void classGetMethodShouldMatchInvokePublicMethodsHint() {
			hints.reflection().registerType(String.class, MemberCategory.INVOKE_PUBLIC_METHODS);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetToStringMethod);
		}

		@Test
		void classGetMethodShouldMatchInvokeMethodHint() {
			hints.reflection().registerType(String.class, typeHint ->
					typeHint.withMethod("toString", Collections.emptyList(), ExecutableMode.INVOKE));
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetToStringMethod);
		}

		@Test
		void classGetMethodShouldNotMatchForWrongType() {
			hints.reflection().registerType(Integer.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetToStringMethod);
		}

		@Test
		void methodInvokeShouldMatchInvokeHintOnMethod() throws NoSuchMethodException {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.METHOD_INVOKE)
					.onInstance(String.class.getMethod("startsWith", String.class)).withArguments("testString", new Object[] { "test" }).build();
			hints.reflection().registerType(String.class, typeHint -> typeHint.withMethod("startsWith",
					TypeReference.listOf(String.class), ExecutableMode.INVOKE));
			assertThatInvocationMatches(InstrumentedMethod.METHOD_INVOKE, invocation);
		}

		@Test
		void methodInvokeShouldNotMatchReflectionTypeHint() throws NoSuchMethodException {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.METHOD_INVOKE)
					.onInstance(String.class.getMethod("toString")).withArguments("", new Object[0]).build();
			hints.reflection().registerType(String.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.METHOD_INVOKE, invocation);
		}

	}

	@Nested
	class FieldReflectionInstrumentationTests {

		RecordedInvocation getPublicField;

		RecordedInvocation stringGetDeclaredField;

		RecordedInvocation stringGetDeclaredFields;

		RecordedInvocation stringGetFields;

		@BeforeEach
		void setup() throws Exception {
			this.getPublicField = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELD)
					.onInstance(PublicField.class).withArgument("field")
					.returnValue(PublicField.class.getField("field")).build();
			this.stringGetDeclaredField = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDFIELD)
					.onInstance(String.class).withArgument("value").returnValue(String.class.getDeclaredField("value")).build();
			this.stringGetDeclaredFields = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDFIELDS)
					.onInstance(String.class).returnValue(String.class.getDeclaredFields()).build();
			this.stringGetFields = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELDS)
					.onInstance(String.class).returnValue(String.class.getFields()).build();
		}

		@Test
		void classGetDeclaredFieldShouldMatchTypeReflectionHint() {
			hints.reflection().registerType(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDFIELD, this.stringGetDeclaredField);
		}

		@Test
		void classGetDeclaredFieldShouldMatchFieldHint() {
			hints.reflection().registerType(String.class, typeHint -> typeHint.withField("value"));
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDFIELD, this.stringGetDeclaredField);
		}

		@Test
		void classGetDeclaredFieldsShouldMatchTypeReflectionHint() {
			hints.reflection().registerType(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDFIELDS, this.stringGetDeclaredFields);
		}

		@Test
		void classGetDeclaredFieldsShouldMatchFieldHint() throws Exception {
			hints.reflection().registerField(String.class.getDeclaredField("value"));
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDFIELDS, this.stringGetDeclaredFields);
		}

		@Test
		void classGetFieldShouldMatchTypeReflectionHint() {
			hints.reflection().registerType(PublicField.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETFIELD, this.getPublicField);
		}

		@Test
		void classGetFieldShouldMatchFieldHint() {
			hints.reflection().registerType(PublicField.class, typeHint -> typeHint.withField("field"));
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETFIELD, this.getPublicField);
		}

		@Test
		void classGetFieldShouldNotMatchPublicFieldsHintWhenPrivate() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELD)
					.onInstance(String.class).withArgument("value").returnValue(null).build();
			hints.reflection().registerType(String.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETFIELD, invocation);
		}

		@Test
		void classGetFieldShouldNotMatchForWrongType() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELD)
					.onInstance(String.class).withArgument("value").returnValue(null).build();
			hints.reflection().registerType(Integer.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETFIELD, invocation);
		}

		@Test
		void classGetFieldsShouldMatchReflectionHint() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELDS)
					.onInstance(PublicField.class).build();
			hints.reflection().registerType(PublicField.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETFIELDS, invocation);
		}

		@Test
		void classGetFieldsShouldMatchTypeHint() {
			hints.reflection().registerType(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETFIELDS, this.stringGetFields);
		}

	}


	@Nested
	class ResourcesInstrumentationTests {

		@Test
		void resourceBundleGetBundleShouldMatchBundleNameHint() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE)
					.withArgument("bundleName").build();
			hints.resources().registerResourceBundle("bundleName");
			assertThatInvocationMatches(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE, invocation);
		}

		@Test
		void resourceBundleGetBundleShouldNotMatchBundleNameHintWhenWrongName() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE)
					.withArgument("bundleName").build();
			hints.resources().registerResourceBundle("wrongBundleName");
			assertThatInvocationDoesNotMatch(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE, invocation);
		}

		@Test
		void classGetResourceShouldMatchResourcePatternWhenAbsolute() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETRESOURCE)
					.onInstance(InstrumentedMethodTests.class).withArgument("/some/path/resource.txt").build();
			hints.resources().registerPattern("some/**");
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETRESOURCE, invocation);
		}

		@Test
		void classGetResourceShouldMatchResourcePatternWhenRelative() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETRESOURCE)
					.onInstance(InstrumentedMethodTests.class).withArgument("resource.txt").build();
			hints.resources().registerPattern("org/springframework/aot/agent/*");
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETRESOURCE, invocation);
		}

		@Test
		void classGetResourceShouldNotMatchResourcePatternWhenInvalid() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETRESOURCE)
					.onInstance(InstrumentedMethodTests.class).withArgument("/some/path/resource.txt").build();
			hints.resources().registerPattern("other/*");
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETRESOURCE, invocation);
		}

		@Test
		void classGetResourceShouldMatchWhenGlobPattern() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETRESOURCE)
					.onInstance(InstrumentedMethodTests.class).withArgument("/some/path/resource.txt").build();
			hints.resources().registerPattern(resourceHint -> resourceHint.includes("some/**"));
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETRESOURCE, invocation);
		}

	}


	@Nested
	class ProxiesInstrumentationTests {

		RecordedInvocation newProxyInstance;

		@BeforeEach
		void setup() {
			this.newProxyInstance = RecordedInvocation.of(InstrumentedMethod.PROXY_NEWPROXYINSTANCE)
					.withArguments(ClassLoader.getSystemClassLoader(), new Class[] { AutoCloseable.class, Comparator.class }, null)
					.returnValue(Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[] { AutoCloseable.class, Comparator.class }, (proxy, method, args) -> null))
					.build();
		}

		@Test
		void proxyNewProxyInstanceShouldMatchWhenInterfacesMatch() {
			hints.proxies().registerJdkProxy(AutoCloseable.class, Comparator.class);
			assertThatInvocationMatches(InstrumentedMethod.PROXY_NEWPROXYINSTANCE, this.newProxyInstance);
		}

		@Test
		void proxyNewProxyInstanceShouldNotMatchWhenInterfacesDoNotMatch() {
			hints.proxies().registerJdkProxy(Comparator.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.PROXY_NEWPROXYINSTANCE, this.newProxyInstance);
		}

		@Test
		void proxyNewProxyInstanceShouldNotMatchWhenWrongOrder() {
			hints.proxies().registerJdkProxy(Comparator.class, AutoCloseable.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.PROXY_NEWPROXYINSTANCE, this.newProxyInstance);
		}
	}


	private void assertThatInvocationMatches(InstrumentedMethod method, RecordedInvocation invocation) {
		assertThat(method.matcher(invocation)).accepts(this.hints);
	}

	private void assertThatInvocationDoesNotMatch(InstrumentedMethod method, RecordedInvocation invocation) {
		assertThat(method.matcher(invocation)).rejects(this.hints);
	}


	static class PublicField {

		public String field;

	}

}
