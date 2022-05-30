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

import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.JdkProxyHint.Builder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JdkProxyHint}.
 *
 * @author Stephane Nicoll
 */
class JdkProxyHintTests {

	@Test
	void equalsWithWithSameInstanceIsTrue() {
		JdkProxyHint hint = new Builder().proxiedInterfaces(Function.class, Consumer.class).build();
		assertThat(hint).isEqualTo(hint);
	}

	@Test
	void equalsWithWithSameProxiedInterfacesIsTrue() {
		JdkProxyHint first = new Builder().proxiedInterfaces(Function.class, Consumer.class).build();
		JdkProxyHint second = new Builder().proxiedInterfaces(TypeReference.of(Function.class.getName()),
				TypeReference.of(Consumer.class)).build();
		assertThat(first).isEqualTo(second);
	}

	@Test
	void equalsWithWithSameProxiedInterfacesDifferentOrderIsFalse() {
		JdkProxyHint first = new Builder().proxiedInterfaces(Function.class, Consumer.class).build();
		JdkProxyHint second = new Builder().proxiedInterfaces(TypeReference.of(Consumer.class),
				TypeReference.of(Function.class.getName())).build();
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalsWithWithDifferentProxiedInterfacesIsFalse() {
		JdkProxyHint first = new Builder().proxiedInterfaces(Function.class).build();
		JdkProxyHint second = new Builder().proxiedInterfaces(TypeReference.of(Function.class.getName()),
				TypeReference.of(Consumer.class)).build();
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalsWithNonJdkProxyHintIsFalse() {
		JdkProxyHint first = new Builder().proxiedInterfaces(Function.class).build();
		TypeReference second = TypeReference.of(Function.class);
		assertThat(first).isNotEqualTo(second);
	}

}
