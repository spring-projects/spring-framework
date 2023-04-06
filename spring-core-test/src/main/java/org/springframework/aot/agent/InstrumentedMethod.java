/*
 * Copyright 2002-2022 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.reflection;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.resource;

/**
 * Java method that is instrumented by the {@link RuntimeHintsAgent}.
 *
 * <p>All {@linkplain RecordedInvocation invocations are recorded} by the agent
 * at runtime. We can then verify that the {@link RuntimeHints} configuration
 * {@linkplain #matcher(RecordedInvocation) matches} the runtime behavior of the
 * codebase.
 *
 * @author Brian Clozel
 * @since 6.0
 * @see RuntimeHintsPredicates
 */
enum InstrumentedMethod {

	/*
	 * Reflection calls
	 */

	/**
	 * {@link Class#forName(String)} and {@link Class#forName(String, boolean, ClassLoader)}.
	 */
	CLASS_FORNAME(Class.class, "forName", HintType.REFLECTION,
			invocation -> {
				String className = invocation.getArgument(0);
				return reflection().onType(TypeReference.of(className));
			}),

	/**
	 * {@link Class#getClasses()}.
	 */
	CLASS_GETCLASSES(Class.class, "getClasses", HintType.REFLECTION,
			invocation -> {
				Class<?> thisClass = invocation.getInstance();
				return reflection().onType(TypeReference.of(thisClass))
						.withAnyMemberCategory(MemberCategory.DECLARED_CLASSES, MemberCategory.PUBLIC_CLASSES);
			}
	),

	/**
	 * {@link Class#getConstructor(Class[])}.
	 */
	CLASS_GETCONSTRUCTOR(Class.class, "getConstructor", HintType.REFLECTION,
			invocation -> {
				Constructor<?> constructor = invocation.getReturnValue();
				if (constructor == null) {
					return runtimeHints -> false;
				}
				return reflection().onConstructor(constructor).introspect();
			}
	),

	/**
	 * {@link Class#getConstructors()}.
	 */
	CLASS_GETCONSTRUCTORS(Class.class, "getConstructors", HintType.REFLECTION,
			invocation -> {
				Class<?> thisClass = invocation.getInstance();
				return reflection().onType(TypeReference.of(thisClass)).withAnyMemberCategory(
						MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS, MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
			}
	),

	/**
	 * {@link Class#getDeclaredClasses()}.
	 */
	CLASS_GETDECLAREDCLASSES(Class.class, "getDeclaredClasses", HintType.REFLECTION,
			invocation -> {
				Class<?> thisClass = invocation.getInstance();
				return reflection().onType(TypeReference.of(thisClass)).withMemberCategory(MemberCategory.DECLARED_CLASSES);
			}
	),

	/**
	 * {@link Class#getDeclaredConstructor(Class[])}.
	 */
	CLASS_GETDECLAREDCONSTRUCTOR(Class.class, "getDeclaredConstructor", HintType.REFLECTION,
			invocation -> {
				Constructor<?> constructor = invocation.getReturnValue();
				if (constructor == null) {
					return runtimeHints -> false;
				}
				TypeReference thisType = invocation.getInstanceTypeReference();
				return reflection().onType(thisType).withMemberCategory(MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS)
						.or(reflection().onConstructor(constructor).introspect());
			}
	),

	/**
	 * {@link Class#getDeclaredConstructors()}.
	 */
	CLASS_GETDECLAREDCONSTRUCTORS(Class.class, "getDeclaredConstructors", HintType.REFLECTION,
			invocation -> {
				Class<?> thisClass = invocation.getInstance();
				return reflection().onType(TypeReference.of(thisClass))
						.withAnyMemberCategory(MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
			}),

	/**
	 * {@link Class#getDeclaredField(String)}.
	 */
	CLASS_GETDECLAREDFIELD(Class.class, "getDeclaredField", HintType.REFLECTION,
			invocation -> {
				Field field = invocation.getReturnValue();
				if (field == null) {
					return runtimeHints -> false;
				}
				TypeReference thisType = invocation.getInstanceTypeReference();
				return reflection().onType(thisType).withMemberCategory(MemberCategory.DECLARED_FIELDS)
						.or(reflection().onField(field));
			}
	),

	/**
	 * {@link Class#getDeclaredFields()}.
	 */
	CLASS_GETDECLAREDFIELDS(Class.class, "getDeclaredFields", HintType.REFLECTION,
			invocation -> {
				Class<?> thisClass = invocation.getInstance();
				return reflection().onType(TypeReference.of(thisClass)).withMemberCategory(MemberCategory.DECLARED_FIELDS);
			}
	),

	/**
	 * {@link Class#getDeclaredMethod(String, Class[])}.
	 */
	CLASS_GETDECLAREDMETHOD(Class.class, "getDeclaredMethod", HintType.REFLECTION,
			invocation -> {
				Method method = invocation.getReturnValue();
				if (method == null) {
					return runtimeHints -> false;
				}
				TypeReference thisType = invocation.getInstanceTypeReference();
				return reflection().onType(thisType)
						.withAnyMemberCategory(MemberCategory.INTROSPECT_DECLARED_METHODS, MemberCategory.INVOKE_DECLARED_METHODS)
						.or(reflection().onMethod(method).introspect());
			}
	),

	/**
	 * {@link Class#getDeclaredMethods()}.
	 */
	CLASS_GETDECLAREDMETHODS(Class.class, "getDeclaredMethods", HintType.REFLECTION,
			invocation -> {
				Class<?> thisClass = invocation.getInstance();
				return reflection().onType(TypeReference.of(thisClass))
						.withAnyMemberCategory(MemberCategory.INTROSPECT_DECLARED_METHODS, MemberCategory.INVOKE_DECLARED_METHODS);
			}
	),

	/**
	 * {@link Class#getField(String)}.
	 */
	CLASS_GETFIELD(Class.class, "getField", HintType.REFLECTION,
			invocation -> {
				Field field = invocation.getReturnValue();
				if (field == null) {
					return runtimeHints -> false;
				}
				TypeReference thisType = invocation.getInstanceTypeReference();
				return reflection().onType(thisType).withMemberCategory(MemberCategory.PUBLIC_FIELDS)
						.and(runtimeHints -> Modifier.isPublic(field.getModifiers()))
						.or(reflection().onType(thisType).withMemberCategory(MemberCategory.DECLARED_FIELDS))
						.or(reflection().onField(invocation.getReturnValue()));
			}),

	/**
	 * {@link Class#getFields()}.
	 */
	CLASS_GETFIELDS(Class.class, "getFields", HintType.REFLECTION,
			invocation -> {
				Class<?> thisClass = invocation.getInstance();
				return reflection().onType(TypeReference.of(thisClass))
						.withAnyMemberCategory(MemberCategory.PUBLIC_FIELDS, MemberCategory.DECLARED_FIELDS);
			}
	),

	/**
	 * {@link Class#getMethod(String, Class[])}.
	 */
	CLASS_GETMETHOD(Class.class, "getMethod", HintType.REFLECTION,
			invocation -> {
				Method method = invocation.getReturnValue();
				if (method == null) {
					return runtimeHints -> false;
				}
				TypeReference thisType = invocation.getInstanceTypeReference();
				return reflection().onType(thisType).withAnyMemberCategory(MemberCategory.INTROSPECT_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS)
						.and(runtimeHints -> Modifier.isPublic(method.getModifiers()))
						.or(reflection().onType(thisType).withAnyMemberCategory(MemberCategory.INTROSPECT_DECLARED_METHODS, MemberCategory.INVOKE_DECLARED_METHODS))
						.or(reflection().onMethod(method).introspect())
						.or(reflection().onMethod(method).invoke());
			}
	),

	/**
	 * {@link Class#getMethods()}.
	 */
	CLASS_GETMETHODS(Class.class, "getMethods", HintType.REFLECTION,
			invocation -> {
				Class<?> thisClass = invocation.getInstance();
				return reflection().onType(TypeReference.of(thisClass)).withAnyMemberCategory(
						MemberCategory.INTROSPECT_PUBLIC_METHODS, MemberCategory.INTROSPECT_DECLARED_METHODS,
						MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_DECLARED_METHODS);
			}
	),

	/**
	 * {@link ClassLoader#loadClass(String)}.
	 */
	CLASSLOADER_LOADCLASS(ClassLoader.class, "loadClass", HintType.REFLECTION,
			invocation -> {
				Class<?> klass = invocation.getReturnValue();
				if (klass == null) {
					return runtimeHints -> false;
				}
				return reflection().onType(klass);
			}),

	/**
	 * {@link Constructor#newInstance(Object...)}.
	 */
	CONSTRUCTOR_NEWINSTANCE(Constructor.class, "newInstance", HintType.REFLECTION,
			invocation -> reflection().onConstructor(invocation.getInstance()).invoke()),

	/**
	 * {@link Method#invoke(Object, Object...)}.
	 */
	METHOD_INVOKE(Method.class, "invoke", HintType.REFLECTION,
			invocation -> reflection().onMethod(invocation.getInstance()).invoke()),

	/**
	 * {@link Field#get(Object)}.
	 */
	FIELD_GET(Field.class, "get", HintType.REFLECTION,
			invocation -> reflection().onField(invocation.getInstance())),

	/**
	 * {@link Field#set(Object, Object)}.
	 */
	FIELD_SET(Field.class, "set", HintType.REFLECTION,
			invocation -> reflection().onField(invocation.getInstance())),


	/*
	 * Resource bundle calls
	 */

	/**
	 * {@link java.util.ResourceBundle#getBundle(String)}.
	 */
	RESOURCEBUNDLE_GETBUNDLE(ResourceBundle.class, "getBundle", HintType.RESOURCE_BUNDLE,
			invocation -> {
				String bundleName = invocation.getArgument(0);
				return resource().forBundle(bundleName);
			}),

	/*
	 * Resource pattern calls
	 */

	/**
	 * {@link Class#getResource(String)}.
	 */
	CLASS_GETRESOURCE(Class.class, "getResource", HintType.RESOURCE_PATTERN,
			invocation -> {
				Class<?> thisClass = invocation.getInstance();
				String resourceName = invocation.getArgument(0);
				return resource().forResource(TypeReference.of(thisClass), resourceName);
			}),

	/**
	 * {@link Class#getResourceAsStream(String)}.
	 */
	CLASS_GETRESOURCEASSTREAM(Class.class, "getResourceAsStream", HintType.RESOURCE_PATTERN,
			CLASS_GETRESOURCE.hintsMatcherGenerator),


	/**
	 * {@link java.lang.ClassLoader#getResource(String)}.
	 */
	CLASSLOADER_GETRESOURCE(ClassLoader.class, "getResource", HintType.RESOURCE_PATTERN,
			invocation -> {
				String resourceName = invocation.getArgument(0);
				return resource().forResource(resourceName);
			}),

	/**
	 * {@link java.lang.ClassLoader#getResourceAsStream(String)}.
	 */
	CLASSLOADER_GETRESOURCEASSTREAM(ClassLoader.class, "getResourceAsStream", HintType.RESOURCE_PATTERN,
			CLASSLOADER_GETRESOURCE.hintsMatcherGenerator),

	/**
	 * {@link java.lang.ClassLoader#getResources(String)}.
	 */
	CLASSLOADER_GETRESOURCES(ClassLoader.class, "getResources", HintType.RESOURCE_PATTERN,
			CLASSLOADER_GETRESOURCE.hintsMatcherGenerator),

	/**
	 * {@link java.lang.Module#getResourceAsStream(String)}.
	 */
	MODULE_GETRESOURCEASSTREAM(Module.class, "getResourceAsStream", HintType.RESOURCE_PATTERN,
			CLASSLOADER_GETRESOURCE.hintsMatcherGenerator),

	/**
	 * {@link java.lang.ClassLoader#resources(String)}.
	 */
	CLASSLOADER_RESOURCES(ClassLoader.class, "resources", HintType.RESOURCE_PATTERN,
			CLASSLOADER_GETRESOURCE.hintsMatcherGenerator),

	/*
	 * JDK Proxy calls
	 */

	/**
	 * {@link Proxy#newProxyInstance(ClassLoader, Class[], InvocationHandler)}.
	 */
	PROXY_NEWPROXYINSTANCE(Proxy.class, "newProxyInstance", HintType.JDK_PROXIES,
			invocation -> {
				Class<?>[] classes = invocation.getArgument(1);
				return RuntimeHintsPredicates.proxies().forInterfaces(classes);
			});


	private final MethodReference methodReference;

	private final HintType hintType;

	private final Function<RecordedInvocation, Predicate<RuntimeHints>> hintsMatcherGenerator;

	InstrumentedMethod(Class<?> klass, String methodName, HintType hintType, Function<RecordedInvocation, Predicate<RuntimeHints>> hintsMatcherGenerator) {
		this.methodReference = MethodReference.of(klass, methodName);
		this.hintType = hintType;
		this.hintsMatcherGenerator = hintsMatcherGenerator;
	}

	/**
	 * Return a {@link MethodReference reference} to the method being instrumented.
	 */
	MethodReference methodReference() {
		return this.methodReference;
	}

	/**
	 * Return the type of {@link RuntimeHints hint} needed ofr the current instrumented method.
	 */
	HintType getHintType() {
		return this.hintType;
	}

	/**
	 * Return a predicate that matches if the current invocation is covered by the given hints.
	 * <p>A runtime invocation for reflection, resources, etc. can be backed by different hints.
	 * For example, {@code MyClass.class.getMethod("sample", null)} can be backed by a reflection
	 * hint on this method only, or a reflection hint on all public/declared methods of the class.
	 * @param invocation the current invocation of the instrumented method
	 */
	Predicate<RuntimeHints> matcher(RecordedInvocation invocation) {
		return this.hintsMatcherGenerator.apply(invocation);
	}

}
