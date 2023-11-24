/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.customizers;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextCustomizerFactories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 6.1
 */
@ContextCustomizerFactories(factories = {BarContextCustomizerFactory.class, EnigmaContextCustomizerFactory.class},
		inheritFactories = false)
class OverriddenContextCustomizerRegistrationTests extends LocalContextCustomizerRegistrationTests {

	@Autowired
	@Qualifier("bar")
	String bar;

	@Autowired
	Integer enigma;


	@Override
	@Test
	void injectedBean() {
		// globally registered via spring.factories
		assertThat(fruit).isEqualTo("apple, banana, cherry");

		// Overridden by this subclass (inheritFactories = false)
		assertThat(foo).isNull();

		// Local to this subclass
		assertThat(bar).isEqualTo("baz");
		assertThat(enigma).isEqualTo(42);
	}

}
