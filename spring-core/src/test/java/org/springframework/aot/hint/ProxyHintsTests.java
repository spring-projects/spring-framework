/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
		assertThat(this.proxyHints.jdkProxyHints()).singleElement().satisfies(proxiedInterfaces(Function.class));
	}

	@Test
	void registerJdkProxyWithTypeReferences() {
		this.proxyHints.registerJdkProxy(TypeReference.of(Function.class), TypeReference.of("com.example.Advised"));
		assertThat(this.proxyHints.jdkProxyHints()).singleElement()
				.satisfies(proxiedInterfaces(Function.class.getName(), "com.example.Advised"));
	}

	@Test
	void registerJdkProxyWithConsumer() {
		this.proxyHints.registerJdkProxy(springProxy("com.example.Test"));
		assertThat(this.proxyHints.jdkProxyHints()).singleElement().satisfies(proxiedInterfaces(
				"com.example.Test",
				"org.springframework.aop.SpringProxy",
				"org.springframework.aop.framework.Advised",
				"org.springframework.core.DecoratingProxy"));
	}

	@Test
	void registerJdkProxyTwiceExposesOneHint() {
		this.proxyHints.registerJdkProxy(Function.class);
		this.proxyHints.registerJdkProxy(TypeReference.of(Function.class.getName()));
		assertThat(this.proxyHints.jdkProxyHints()).singleElement().satisfies(proxiedInterfaces(Function.class));
	}

	@Test
	void registerJdkProxyWithJavaSerialization() {
		this.proxyHints.registerJdkProxy(hint -> {
			hint.proxiedInterfaces(TypeReference.of("com.example.Test"));
			hint.withJavaSerialization(true);
		});
		assertThat(this.proxyHints.jdkProxyHints()).singleElement().satisfies(hint -> {
			assertThat(hint.getProxiedInterfaces()).containsExactly(TypeReference.of("com.example.Test"));
			assertThat(hint.hasJavaSerialization()).isTrue();
		});
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
