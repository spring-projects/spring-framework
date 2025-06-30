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

package org.springframework.test.context.customizers;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextCustomizerFactories;
import org.springframework.test.context.ContextCustomizerFactories.MergeMode;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 6.1
 */
@SpringJUnitConfig
@ContextCustomizerFactories(factories = {FooContextCustomizerFactory.class, BarContextCustomizerFactory.class},
	mergeMode = MergeMode.REPLACE_DEFAULTS)
class ReplaceDefaultsContextCustomizerTests {

	// GlobalFruitContextCustomizerFactory is registered via spring.factories
	@Autowired(required = false)
	@Qualifier("global$fruit")
	String fruit;

	@Autowired(required = false)
	@Qualifier("foo")
	String foo;

	@Autowired(required = false)
	@Qualifier("bar")
	String bar;


	@Test
	void injectedBean() {
		// MergeMode.REPLACE_DEFAULTS overrides spring.factories lookup
		assertThat(fruit).isNull();

		// From local @ContextCustomizerFactories
		assertThat(foo).isEqualTo("bar");
		assertThat(bar).isEqualTo("baz");
	}

}
