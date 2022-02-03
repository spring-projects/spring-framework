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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.JdkProxyHint.Builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ProxyHints}.
 *
 * @author Stephane Nicoll
 */
class ProxyHintsTests {

	private final ProxyHints proxyHints = new ProxyHints();


	@Test
	void registerJdkProxyWithInterfaceClass() {
		this.proxyHints.registerJdkProxy(Function.class);
		assertThat(this.proxyHints.jdkProxies()).singleElement().satisfies(proxiedInterfaces(Function.class));
	}

	@Test
	void registerJdkProxyWithConcreteClass() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.proxyHints.registerJdkProxy(String.class))
				.withMessageContaining(String.class.getName());
	}

	@Test
	void registerJdkProxyWithInterfaceClassNames() {
		this.proxyHints.registerJdkProxy(TypeReference.of(Function.class),
				TypeReference.of("com.example.Advised"));
		assertThat(this.proxyHints.jdkProxies()).singleElement().satisfies(proxiedInterfaces(
				Function.class.getName(), "com.example.Advised"));
	}

	@Test
	void registerJdkProxyWithSupplier() {
		this.proxyHints.registerJdkProxy(springProxy(TypeReference.of("com.example.Test")));
		assertThat(this.proxyHints.jdkProxies()).singleElement().satisfies(proxiedInterfaces(
				"org.springframework.aop.SpringProxy",
				"org.springframework.aop.framework.Advised",
				"org.springframework.core.DecoratingProxy",
				"com.example.Test"));
	}

	@Test
	void registerJdkProxyTwiceExposesOneHint() {
		this.proxyHints.registerJdkProxy(Function.class);
		this.proxyHints.registerJdkProxy(TypeReference.of(Function.class.getName()));
		assertThat(this.proxyHints.jdkProxies()).singleElement().satisfies(proxiedInterfaces(Function.class));
	}

	@Test
	void registerClassProxyWithTargetClass() {
		this.proxyHints.registerClassProxy(Properties.class, classProxyHint ->
				classProxyHint.proxiedInterfaces(Serializable.class));
		assertThat(this.proxyHints.classProxies()).singleElement().satisfies(classProxyHint -> {
			assertThat(classProxyHint.getTargetClass()).isEqualTo(TypeReference.of(Properties.class));
			assertThat(classProxyHint.getProxiedInterfaces()).containsOnly(TypeReference.of(Serializable.class));
		});
	}

	@Test
	void registerClassProxyWithTargetInterface() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.proxyHints.registerClassProxy(Serializable.class, classProxyHint -> {
		})).withMessageContaining(Serializable.class.getName());
	}

	private static Supplier<JdkProxyHint> springProxy(TypeReference proxiedInterface) {
		return () -> new Builder().proxiedInterfaces(Stream.of("org.springframework.aop.SpringProxy",
								"org.springframework.aop.framework.Advised", "org.springframework.core.DecoratingProxy")
						.map(TypeReference::of).toArray(TypeReference[]::new))
				.proxiedInterfaces(proxiedInterface).build();
	}

	private Consumer<JdkProxyHint> proxiedInterfaces(String... proxiedInterfaces) {
		return jdkProxyHint -> assertThat(jdkProxyHint.getProxiedInterfaces())
				.containsExactly(Arrays.stream(proxiedInterfaces)
						.map(TypeReference::of).toArray(TypeReference[]::new));
	}

	private Consumer<JdkProxyHint> proxiedInterfaces(Class<?>... proxiedInterfaces) {
		return jdkProxyHint -> assertThat(jdkProxyHint.getProxiedInterfaces())
				.containsExactly(Arrays.stream(proxiedInterfaces)
						.map(TypeReference::of).toArray(TypeReference[]::new));
	}

}
