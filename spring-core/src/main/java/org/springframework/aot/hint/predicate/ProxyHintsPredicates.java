/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint.predicate;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.aot.hint.ProxyHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.util.Assert;

/**
 * Generator of {@link ProxyHints} predicates, testing whether the given hints
 * match the expected behavior for proxies.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public class ProxyHintsPredicates {

	ProxyHintsPredicates() {
	}

	/**
	 * Return a predicate that checks whether a {@link org.springframework.aot.hint.JdkProxyHint}
	 * is registered for the given interfaces.
	 * <p>Note that the order in which interfaces are given matters.
	 * @param interfaces the proxied interfaces
	 * @return the {@link RuntimeHints} predicate
	 * @see java.lang.reflect.Proxy
	 */
	public Predicate<RuntimeHints> forInterfaces(Class<?>... interfaces) {
		Assert.notEmpty(interfaces, "'interfaces' should not be empty");
		return forInterfaces(Arrays.stream(interfaces).map(TypeReference::of).toArray(TypeReference[]::new));
	}

	/**
	 * Return a predicate that checks whether a {@link org.springframework.aot.hint.JdkProxyHint}
	 * is registered for the given interfaces.
	 * <p>Note that the order in which interfaces are given matters.
	 * @param interfaces the proxied interfaces as type references
	 * @return the {@link RuntimeHints} predicate
	 * @see java.lang.reflect.Proxy
	 */
	public Predicate<RuntimeHints> forInterfaces(TypeReference... interfaces) {
		Assert.notEmpty(interfaces, "'interfaces' should not be empty");
		List<TypeReference> interfaceList = Arrays.asList(interfaces);
		return hints -> hints.proxies().jdkProxyHints().anyMatch(proxyHint ->
				proxyHint.getProxiedInterfaces().equals(interfaceList));
	}

}
