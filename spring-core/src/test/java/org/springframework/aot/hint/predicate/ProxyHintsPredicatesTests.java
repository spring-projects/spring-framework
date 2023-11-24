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
