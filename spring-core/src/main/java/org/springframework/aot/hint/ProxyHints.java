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

package org.springframework.aot.hint;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.aot.hint.ClassProxyHint.Builder;

/**
 * Gather the need of using proxies at runtime.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public class ProxyHints {

	private final Set<JdkProxyHint> jdkProxies = new LinkedHashSet<>();

	private final Set<ClassProxyHint> classProxies = new LinkedHashSet<>();


	/**
	 * Return the interfaces-based proxies that are required.
	 * @return a stream of {@link JdkProxyHint}
	 */
	public Stream<JdkProxyHint> jdkProxies() {
		return this.jdkProxies.stream();
	}

	/**
	 * Return the class-based proxies that are required.
	 * @return a stream of {@link ClassProxyHint}
	 */
	public Stream<ClassProxyHint> classProxies() {
		return this.classProxies.stream();
	}

	/**
	 * Register a {@link JdkProxyHint}.
	 * @param hint the supplier to the hint
	 * @return {@code this}, to facilitate method chaining
	 */
	public ProxyHints registerJdkProxy(Supplier<JdkProxyHint> hint) {
		this.jdkProxies.add(hint.get());
		return this;
	}

	/**
	 * Register that a JDK proxy implementing the interfaces defined by the
	 * specified {@link TypeReference type references} is required.
	 * @param proxiedInterfaces the interfaces the proxy should implement
	 * @return {@code this}, to facilitate method chaining
	 */
	public ProxyHints registerJdkProxy(TypeReference... proxiedInterfaces) {
		return registerJdkProxy(() -> new JdkProxyHint.Builder()
				.proxiedInterfaces(proxiedInterfaces).build());
	}

	/**
	 * Register that a JDK proxy implementing the specified interfaces is
	 * required.
	 * @param proxiedInterfaces the interfaces the proxy should implement
	 * @return {@code this}, to facilitate method chaining
	 */
	public ProxyHints registerJdkProxy(Class<?>... proxiedInterfaces) {
		List<String> concreteTypes = Arrays.stream(proxiedInterfaces)
				.filter(candidate -> !candidate.isInterface()).map(Class::getName).collect(Collectors.toList());
		if (!concreteTypes.isEmpty()) {
			throw new IllegalArgumentException("Not an interface: " + concreteTypes);
		}
		return registerJdkProxy(() -> new JdkProxyHint.Builder()
				.proxiedInterfaces(proxiedInterfaces).build());
	}

	/**
	 * Register that a class proxy is required for the class defined by the
	 * specified {@link TypeReference}.
	 * @param targetClass the target class of the proxy
	 * @param classProxyHint a builder to further customize the hint for that proxy
	 * @return {@code this}, to facilitate method chaining
	 */
	public ProxyHints registerClassProxy(TypeReference targetClass, Consumer<Builder> classProxyHint) {
		Builder builder = ClassProxyHint.of(targetClass);
		classProxyHint.accept(builder);
		this.classProxies.add(builder.build());
		return this;
	}

	/**
	 * Register that a class proxy is required for the specified class.
	 * @param targetClass the target class of the proxy
	 * @param classProxyHint a builder to further customize the hint for that proxy
	 * @return {@code this}, to facilitate method chaining
	 */
	public ProxyHints registerClassProxy(Class<?> targetClass, Consumer<Builder> classProxyHint) {
		if (targetClass.isInterface()) {
			throw new IllegalArgumentException("Should not be an interface: " + targetClass);
		}
		return registerClassProxy(TypeReference.of(targetClass), classProxyHint);
	}

}
