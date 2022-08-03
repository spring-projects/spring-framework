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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.aot.hint.ClassProxyHint.Builder;

/**
 * Gather the need for using proxies at runtime.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public class ProxyHints {

	private final Set<JdkProxyHint> jdkProxies = new LinkedHashSet<>();

	private final Set<ClassProxyHint> classProxies = new LinkedHashSet<>();


	/**
	 * Return the interface-based proxies that are required.
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
	 * @param jdkProxyHint the consumer of the hint builder
	 * @return {@code this}, to facilitate method chaining
	 */
	public ProxyHints registerJdkProxy(Consumer<JdkProxyHint.Builder> jdkProxyHint) {
		JdkProxyHint.Builder builder = new JdkProxyHint.Builder();
		jdkProxyHint.accept(builder);
		this.jdkProxies.add(builder.build());
		return this;
	}

	/**
	 * Register that a JDK proxy implementing the interfaces defined by the
	 * specified {@linkplain TypeReference type references} is required.
	 * @param proxiedInterfaces the type references for the interfaces the proxy
	 * should implement
	 * @return {@code this}, to facilitate method chaining
	 */
	public ProxyHints registerJdkProxy(TypeReference... proxiedInterfaces) {
		return registerJdkProxy(jdkProxyHint ->
				jdkProxyHint.proxiedInterfaces(proxiedInterfaces));
	}

	/**
	 * Register that a JDK proxy implementing the specified interfaces is
	 * required.
	 * <p>When registering a JDK proxy for Spring AOP, consider using
	 * {@link org.springframework.aop.framework.AopProxyUtils#completeJdkProxyInterfaces(Class...)
	 * AopProxyUtils.completeJdkProxyInterfaces()} for convenience.
	 * @param proxiedInterfaces the interfaces the proxy should implement
	 * @return {@code this}, to facilitate method chaining
	 */
	public ProxyHints registerJdkProxy(Class<?>... proxiedInterfaces) {
		return registerJdkProxy(jdkProxyHint ->
				jdkProxyHint.proxiedInterfaces(proxiedInterfaces));
	}

	/**
	 * Register that a class proxy is required for the class defined by the
	 * specified {@link TypeReference}.
	 * @param typeReference the type reference for the target class of the proxy
	 * @param classProxyHint a builder to further customize the hint for that proxy
	 * @return {@code this}, to facilitate method chaining
	 */
	public ProxyHints registerClassProxy(TypeReference typeReference, Consumer<Builder> classProxyHint) {
		return addClassProxyHint(ClassProxyHint.of(typeReference), classProxyHint);
	}

	/**
	 * Register that a class proxy is required for the specified class.
	 * @param targetClass the target class of the proxy
	 * @param classProxyHint a builder to further customize the hint for that proxy
	 * @return {@code this}, to facilitate method chaining
	 */
	public ProxyHints registerClassProxy(Class<?> targetClass, Consumer<Builder> classProxyHint) {
		return addClassProxyHint(ClassProxyHint.of(targetClass), classProxyHint);
	}

	private ProxyHints addClassProxyHint(ClassProxyHint.Builder builder, Consumer<ClassProxyHint.Builder> classProxyHint) {
		classProxyHint.accept(builder);
		this.classProxies.add(builder.build());
		return this;
	}

}
