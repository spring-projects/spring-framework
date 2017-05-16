/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.function.server;

import java.util.Optional;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class HandlerStrategiesTests {

	@Test
	public void empty() {
		HandlerStrategies strategies = HandlerStrategies.empty().build();
		assertEquals(Optional.empty(), strategies.messageReaders().get().findFirst());
		assertEquals(Optional.empty(), strategies.messageWriters().get().findFirst());
		assertEquals(Optional.empty(), strategies.viewResolvers().get().findFirst());
		assertNull(strategies.localeResolver().get());
	}

	@Test
	public void withDefaults() {
		HandlerStrategies strategies = HandlerStrategies.withDefaults();
		assertNotEquals(Optional.empty(), strategies.messageReaders().get().findFirst());
		assertNotEquals(Optional.empty(), strategies.messageWriters().get().findFirst());
		assertEquals(Optional.empty(), strategies.viewResolvers().get().findFirst());
		assertNotNull(strategies.localeResolver().get());
	}

}

