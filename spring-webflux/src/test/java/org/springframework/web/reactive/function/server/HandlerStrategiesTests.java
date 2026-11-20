/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class HandlerStrategiesTests {

	@Test
	void empty() {
		HandlerStrategies strategies = HandlerStrategies.empty().build();
		assertThat(strategies.messageReaders()).isEmpty();
		assertThat(strategies.messageWriters()).isEmpty();
		assertThat(strategies.viewResolvers()).isEmpty();
	}

	@Test
	void withDefaults() {
		HandlerStrategies strategies = HandlerStrategies.withDefaults();
		assertThat(strategies.messageReaders()).isNotEmpty();
		assertThat(strategies.messageWriters()).isNotEmpty();
		assertThat(strategies.viewResolvers()).isEmpty();
	}

}

