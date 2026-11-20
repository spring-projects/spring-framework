/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class ExchangeStrategiesTests {

	@Test
	void empty() {
		ExchangeStrategies strategies = ExchangeStrategies.empty().build();
		assertThat(strategies.messageReaders()).isEmpty();
		assertThat(strategies.messageWriters()).isEmpty();
	}

	@Test
	void withDefaults() {
		ExchangeStrategies strategies = ExchangeStrategies.withDefaults();
		assertThat(strategies.messageReaders()).isNotEmpty();
		assertThat(strategies.messageWriters()).isNotEmpty();
	}

	@Test
	@SuppressWarnings("deprecation")
	void mutate() {
		ExchangeStrategies strategies = ExchangeStrategies.empty().build();
		assertThat(strategies.messageReaders()).isEmpty();
		assertThat(strategies.messageWriters()).isEmpty();

		ExchangeStrategies mutated = strategies.mutate().codecs(codecs -> codecs.registerDefaults(true)).build();
		assertThat(mutated.messageReaders()).isNotEmpty();
		assertThat(mutated.messageWriters()).isNotEmpty();
	}

}
