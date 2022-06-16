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

import java.io.Closeable;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Properties;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.JdkProxyHint.Builder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ClassProxyHint}.
 *
 * @author Stephane Nicoll
 */
class ClassProxyHintTests {

	@Test
	void equalsWithWithSameInstanceIsTrue() {
		ClassProxyHint hint = ClassProxyHint.of(Properties.class).build();
		assertThat(hint).isEqualTo(hint);
	}

	@Test
	void equalsWithWithSameTargetClassIsTrue() {
		ClassProxyHint first = ClassProxyHint.of(Properties.class).build();
		ClassProxyHint second = ClassProxyHint.of(TypeReference.of(Properties.class)).build();
		assertThat(first).isEqualTo(second);
	}

	@Test
	void equalsWithWithSameProxiedInterfacesIsTrue() {
		ClassProxyHint first = ClassProxyHint.of(Properties.class)
				.proxiedInterfaces(Serializable.class).build();
		ClassProxyHint second = ClassProxyHint.of(Properties.class)
				.proxiedInterfaces(TypeReference.of(Serializable.class)).build();
		assertThat(first).isEqualTo(second);
	}

	@Test
	void equalsWithWithDifferentTargetClassIsFalse() {
		ClassProxyHint first = ClassProxyHint.of(Properties.class).build();
		ClassProxyHint second = ClassProxyHint.of(Hashtable.class).build();
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalsWithWithSameProxiedInterfacesDifferentOrderIsFalse() {
		ClassProxyHint first = ClassProxyHint.of(Properties.class)
				.proxiedInterfaces(Serializable.class, Closeable.class).build();
		ClassProxyHint second = ClassProxyHint.of(Properties.class)
				.proxiedInterfaces(TypeReference.of(Closeable.class), TypeReference.of(Serializable.class))
				.build();
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalsWithWithDifferentProxiedInterfacesIsFalse() {
		ClassProxyHint first = ClassProxyHint.of(Properties.class)
				.proxiedInterfaces(Serializable.class).build();
		ClassProxyHint second = ClassProxyHint.of(Properties.class)
				.proxiedInterfaces(TypeReference.of(Closeable.class)).build();
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalsWithNonClassProxyHintIsFalse() {
		ClassProxyHint first = ClassProxyHint.of(Properties.class).build();
		JdkProxyHint second = new Builder().proxiedInterfaces(Function.class).build();
		assertThat(first).isNotEqualTo(second);
	}

}
