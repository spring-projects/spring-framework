/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Gather the need for using proxies at runtime.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public class ProxyHints {

	private final Set<JdkProxyHint> jdkProxies = new LinkedHashSet<>();


	/**
	 * Return the interface-based proxies that are required.
	 * @return a stream of {@link JdkProxyHint}
	 */
	public Stream<JdkProxyHint> jdkProxyHints() {
		return this.jdkProxies.stream();
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

}
