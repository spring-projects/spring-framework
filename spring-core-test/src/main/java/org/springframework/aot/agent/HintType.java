/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.agent;

import org.springframework.aot.hint.JdkProxyHint;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.ResourceBundleHint;
import org.springframework.aot.hint.ResourcePatternHint;

/**
 * Main types of {@link org.springframework.aot.hint.RuntimeHints}.
 *
 * <p>This allows to sort {@linkplain RecordedInvocation recorded invocations}
 * into hint categories.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public enum HintType {

	/**
	 * Reflection hint, as described by {@link org.springframework.aot.hint.ReflectionHints}.
	 */
	REFLECTION(ReflectionHints.class),

	/**
	 * Resource pattern hint, as described by {@link org.springframework.aot.hint.ResourceHints#resourcePatternHints()}.
	 */
	RESOURCE_PATTERN(ResourcePatternHint.class),

	/**
	 * Resource bundle hint, as described by {@link org.springframework.aot.hint.ResourceHints#resourceBundleHints()}.
	 */
	RESOURCE_BUNDLE(ResourceBundleHint.class),

	/**
	 * Java serialization hint, as described by {@link org.springframework.aot.hint.JavaSerializationHint}.
	 */
	@Deprecated(since = "7.0.5", forRemoval = true)
	@SuppressWarnings("removal")
	JAVA_SERIALIZATION(org.springframework.aot.hint.JavaSerializationHint.class),

	/**
	 * JDK proxies hint, as described by {@link org.springframework.aot.hint.ProxyHints#jdkProxyHints()}.
	 */
	JDK_PROXIES(JdkProxyHint.class);


	private final Class<?> hintClass;

	HintType(Class<?> hintClass) {
		this.hintClass = hintClass;
	}

	public String hintClassName() {
		return this.hintClass.getSimpleName();
	}

}
