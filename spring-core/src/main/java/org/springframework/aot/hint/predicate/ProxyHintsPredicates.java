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
		return hints -> hints.proxies().jdkProxies().anyMatch(proxyHint ->
				proxyHint.getProxiedInterfaces().equals(interfaceList));
	}

}
