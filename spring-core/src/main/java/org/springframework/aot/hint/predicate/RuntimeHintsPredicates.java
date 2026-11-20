/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint.predicate;

import org.springframework.aot.hint.ProxyHints;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.SerializationHints;

/**
 * Static generator of predicates that test whether the given {@link RuntimeHints}
 * instance matches the expected behavior for reflection, resource, serialization,
 * or proxy generation.
 *
 * <p>This utility class can be used by {@link RuntimeHintsRegistrar} to conditionally
 * register hints depending on what's present already. This can also be used as a
 * testing utility for checking proper registration of hints:
 * <pre class="code">
 * Predicate&lt;RuntimeHints&gt; predicate = RuntimeHintsPredicates.reflection().onMethod(MyClass.class, "someMethod").invoke();
 * assertThat(predicate).accepts(runtimeHints);
 * </pre>
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 6.0
 */
public abstract class RuntimeHintsPredicates {

	private static final ReflectionHintsPredicates reflection = new ReflectionHintsPredicates();

	private static final ResourceHintsPredicates resource = new ResourceHintsPredicates();

	@SuppressWarnings("removal")
	private static final SerializationHintsPredicates serialization = new SerializationHintsPredicates();

	private static final ProxyHintsPredicates proxies = new ProxyHintsPredicates();


	private RuntimeHintsPredicates() {
	}

	/**
	 * Return a predicate generator for {@link ReflectionHints reflection hints}.
	 * @return the predicate generator
	 */
	public static ReflectionHintsPredicates reflection() {
		return reflection;
	}

	/**
	 * Return a predicate generator for {@link ResourceHints resource hints}.
	 * @return the predicate generator
	 */
	public static ResourceHintsPredicates resource() {
		return resource;
	}

	/**
	 * Return a predicate generator for {@link SerializationHints serialization hints}.
	 * @return the predicate generator
	 * @deprecated in favor of {@link #reflection()}
	 */
	@Deprecated(since = "7.0.6", forRemoval = true)
	@SuppressWarnings("removal")
	public static SerializationHintsPredicates serialization() {
		return serialization;
	}

	/**
	 * Return a predicate generator for {@link ProxyHints proxy hints}.
	 * @return the predicate generator
	 */
	public static ProxyHintsPredicates proxies() {
		return proxies;
	}

}
