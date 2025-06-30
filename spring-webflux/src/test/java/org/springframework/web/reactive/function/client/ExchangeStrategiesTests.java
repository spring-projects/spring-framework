/*
 * Copyright 2002-present the original author or authors.
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
	public void mutate() {
		ExchangeStrategies strategies = ExchangeStrategies.empty().build();
		assertThat(strategies.messageReaders()).isEmpty();
		assertThat(strategies.messageWriters()).isEmpty();

		ExchangeStrategies mutated = strategies.mutate().codecs(codecs -> codecs.registerDefaults(true)).build();
		assertThat(mutated.messageReaders()).isNotEmpty();
		assertThat(mutated.messageWriters()).isNotEmpty();
	}

}
