/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint.predicate;

import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ProxyHintsPredicates}.
 *
 * @author Brian Clozel
 */
class ProxyHintsPredicatesTests {

	private final ProxyHintsPredicates proxy = new ProxyHintsPredicates();

	private RuntimeHints runtimeHints;

	@BeforeEach
	void setup() {
		this.runtimeHints = new RuntimeHints();
	}

	@Test
	void shouldFailForEmptyInterfacesArray() {
		assertThatThrownBy(() -> this.proxy.forInterfaces(new Class<?>[] {})).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void proxyForInterfacesMatchesProxyHint() {
		this.runtimeHints.proxies().registerJdkProxy(FirstTestInterface.class, SecondTestInterface.class);
		assertPredicateMatches(this.proxy.forInterfaces(FirstTestInterface.class, SecondTestInterface.class));
	}

	@Test
	void proxyForInterfacesDoesNotMatchProxyHintDifferentOrder() {
		this.runtimeHints.proxies().registerJdkProxy(SecondTestInterface.class, FirstTestInterface.class);
		assertPredicateDoesNotMatch(this.proxy.forInterfaces(FirstTestInterface.class, SecondTestInterface.class));
	}

	interface FirstTestInterface {

	}

	interface SecondTestInterface {

	}

	private void assertPredicateMatches(Predicate<RuntimeHints> predicate) {
		assertThat(predicate.test(this.runtimeHints)).isTrue();
	}

	private void assertPredicateDoesNotMatch(Predicate<RuntimeHints> predicate) {
		assertThat(predicate.test(this.runtimeHints)).isFalse();
	}

}
