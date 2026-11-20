/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
					MemberCategory.ACCESS_DECLARED_FIELDS);

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
