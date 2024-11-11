/*
 * Copyright 2002-2024 the original author or authors.
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
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactories;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test which verifies support for {@link ContextCustomizerFactory}
 * and {@link ContextCustomizer} when a custom factory is registered declaratively
 * via {@link ContextCustomizerFactories @ContextCustomizerFactories}.
 *
 * @author Sam Brannen
 * @since 6.1
 */
@SpringJUnitConfig
@CustomizeWithFruit
@CustomizeWithFoo
@ContextCustomizerFactories(EnigmaContextCustomizerFactory.class)
@CustomizeWithBar
class ContextCustomizerDeclarativeRegistrationTests {

	// GlobalFruitContextCustomizerFactory is registered via spring.factories
	@Autowired(required = false)
	@Qualifier("global$fruit")
	String fruit;

	@Autowired
	Integer enigma;

	@Autowired(required = false)
	@Qualifier("foo")
	String foo;

	@Autowired(required = false)
	@Qualifier("bar")
	String bar;


	@Test
	void injectedBean() {
		// registered globally via spring.factories
		assertThat(fruit).isEqualTo("apple, banana, cherry");

		// From local @ContextCustomizerFactories
		assertThat(enigma).isEqualTo(42);

		// @ContextCustomizerFactories is not currently supported as a repeatable annotation,
		// and a directly present @ContextCustomizerFactories annotation overrides
		// @ContextCustomizerFactories meta-annotations.
		assertThat(foo).isNull();
		assertThat(bar).isNull();
	}

}
