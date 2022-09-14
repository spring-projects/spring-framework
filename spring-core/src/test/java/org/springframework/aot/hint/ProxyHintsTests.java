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
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ProxyHints}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class ProxyHintsTests {

	private final ProxyHints proxyHints = new ProxyHints();


	@Test
	void registerJdkProxyWithSealedInterface() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.proxyHints.registerJdkProxy(SealedInterface.class))
				.withMessageContaining(SealedInterface.class.getName());
	}

	@Test
	void registerJdkProxyWithConcreteClass() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.proxyHints.registerJdkProxy(String.class))
				.withMessageContaining(String.class.getName());
	}

	@Test
	void registerJdkProxyWithInterface() {
		this.proxyHints.registerJdkProxy(Function.class);
		assertThat(this.proxyHints.jdkProxies()).singleElement().satisfies(proxiedInterfaces(Function.class));
	}

	@Test
	void registerJdkProxyWithTypeReferences() {
		this.proxyHints.registerJdkProxy(TypeReference.of(Function.class), TypeReference.of("com.example.Advised"));
		assertThat(this.proxyHints.jdkProxies()).singleElement()
				.satisfies(proxiedInterfaces(Function.class.getName(), "com.example.Advised"));
	}

	@Test
	void registerJdkProxyWithConsumer() {
		this.proxyHints.registerJdkProxy(springProxy("com.example.Test"));
		assertThat(this.proxyHints.jdkProxies()).singleElement().satisfies(proxiedInterfaces(
				"com.example.Test",
				"org.springframework.aop.SpringProxy",
				"org.springframework.aop.framework.Advised",
				"org.springframework.core.DecoratingProxy"));
	}

	@Test
	void registerJdkProxyTwiceExposesOneHint() {
		this.proxyHints.registerJdkProxy(Function.class);
		this.proxyHints.registerJdkProxy(TypeReference.of(Function.class.getName()));
		assertThat(this.proxyHints.jdkProxies()).singleElement().satisfies(proxiedInterfaces(Function.class));
	}


	private static Consumer<JdkProxyHint.Builder> springProxy(String proxiedInterface) {
		return builder -> builder.proxiedInterfaces(toTypeReferences(
				proxiedInterface,
				"org.springframework.aop.SpringProxy",
				"org.springframework.aop.framework.Advised",
				"org.springframework.core.DecoratingProxy"));
	}

	private static Consumer<JdkProxyHint> proxiedInterfaces(String... proxiedInterfaces) {
		return jdkProxyHint -> assertThat(jdkProxyHint.getProxiedInterfaces())
				.containsExactly(toTypeReferences(proxiedInterfaces));
	}

	private static Consumer<JdkProxyHint> proxiedInterfaces(Class<?>... proxiedInterfaces) {
		return jdkProxyHint -> assertThat(jdkProxyHint.getProxiedInterfaces())
				.containsExactlyElementsOf(TypeReference.listOf(proxiedInterfaces));
	}

	private static TypeReference[] toTypeReferences(String... proxiedInterfaces) {
		return Arrays.stream(proxiedInterfaces).map(TypeReference::of).toArray(TypeReference[]::new);
	}


	sealed interface SealedInterface {
	}

	@SuppressWarnings("unused")
	static final class SealedClass implements SealedInterface {
	}

}
