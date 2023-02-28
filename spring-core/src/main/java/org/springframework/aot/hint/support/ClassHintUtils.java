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

package org.springframework.aot.hint.support;

import java.lang.reflect.Proxy;
import java.util.function.Consumer;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.util.ClassUtils;

/**
 * Utilities for core hint inference on Spring-managed classes,
 * specifically for proxy types such as interface-based JDK proxies
 * and CGLIB-generated subclasses which need proxy/reflection hints.
 *
 * <p>Note that this class does not take specifics of Spring AOP or
 * any other framework arrangement into account. It just operates
 * on the JDK and CGLIB proxy facilities and their core conventions.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 6.0.3
 * @see org.springframework.aot.hint.ProxyHints
 * @see org.springframework.aot.hint.ReflectionHints
 */
public abstract class ClassHintUtils {

	private static final Consumer<TypeHint.Builder> asClassBasedProxy = hint ->
			hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
					MemberCategory.INVOKE_DECLARED_METHODS,
					MemberCategory.DECLARED_FIELDS);

	private static final Consumer<TypeHint.Builder> asProxiedUserClass = hint ->
			hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS,
					MemberCategory.INVOKE_DECLARED_METHODS);


	/**
	 * Register a proxy hint for a JDK proxy or corresponding reflection hints
	 * for a CGLIB-generated subclass, if necessary.
	 * @param candidateClass the class to introspect
	 * @param runtimeHints the RuntimeHints instance to register the hints on
	 * @see Proxy#isProxyClass(Class)
	 * @see ClassUtils#getUserClass(Class)
	 */
	public static void registerProxyIfNecessary(Class<?> candidateClass, RuntimeHints runtimeHints) {
		if (Proxy.isProxyClass(candidateClass)) {
			// A JDK proxy class needs an explicit hint
			runtimeHints.proxies().registerJdkProxy(candidateClass.getInterfaces());
		}
		else {
			// Potentially a CGLIB-generated subclass with reflection hints
			Class<?> userClass = ClassUtils.getUserClass(candidateClass);
			if (userClass != candidateClass) {
				runtimeHints.reflection()
						.registerType(candidateClass, asClassBasedProxy)
						.registerType(userClass, asProxiedUserClass);
			}
		}
	}

}
