/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class HandlerStrategiesTests {

	@Test
	public void empty() {
		HandlerStrategies strategies = HandlerStrategies.empty().build();
		assertThat(strategies.messageReaders().isEmpty()).isTrue();
		assertThat(strategies.messageWriters().isEmpty()).isTrue();
		assertThat(strategies.viewResolvers().isEmpty()).isTrue();
	}

	@Test
	public void withDefaults() {
		HandlerStrategies strategies = HandlerStrategies.withDefaults();
		assertThat(strategies.messageReaders().isEmpty()).isFalse();
		assertThat(strategies.messageWriters().isEmpty()).isFalse();
		assertThat(strategies.viewResolvers().isEmpty()).isTrue();
	}

}

