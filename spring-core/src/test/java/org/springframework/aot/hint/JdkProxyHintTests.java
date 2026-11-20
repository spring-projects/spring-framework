/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void equalsWithSameInstanceIsTrue() {
		JdkProxyHint hint = new Builder().proxiedInterfaces(Function.class, Consumer.class).build();
		assertThat(hint).isEqualTo(hint);
	}

	@Test
	void equalsWithSameProxiedInterfacesIsTrue() {
		JdkProxyHint first = new Builder().proxiedInterfaces(Function.class, Consumer.class).build();
		JdkProxyHint second = new Builder().proxiedInterfaces(TypeReference.of(Function.class.getName()),
				TypeReference.of(Consumer.class)).build();
		assertThat(first).isEqualTo(second);
	}

	@Test
	void equalsWithSameProxiedInterfacesAndDifferentConditionIsFalse() {
		JdkProxyHint first = new Builder().proxiedInterfaces(Function.class, Consumer.class)
				.onReachableType(TypeReference.of(String.class)).build();
		JdkProxyHint second = new Builder().proxiedInterfaces(TypeReference.of(Function.class.getName()),
				TypeReference.of(Consumer.class)).onReachableType(TypeReference.of(Function.class)).build();
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalsWithSameProxiedInterfacesDifferentOrderIsFalse() {
		JdkProxyHint first = new Builder().proxiedInterfaces(Function.class, Consumer.class).build();
		JdkProxyHint second = new Builder().proxiedInterfaces(TypeReference.of(Consumer.class),
				TypeReference.of(Function.class.getName())).build();
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalsWithDifferentProxiedInterfacesIsFalse() {
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
