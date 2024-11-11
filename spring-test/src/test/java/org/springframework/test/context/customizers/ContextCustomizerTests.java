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

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactories;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.customizers.ContextCustomizerTests.EnigmaTestContextBootstrapper;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.AbstractTestContextBootstrapper;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test which verifies support for {@link ContextCustomizerFactory}
 * and {@link ContextCustomizer} when a custom factory is registered by overriding
 * {@link AbstractTestContextBootstrapper#getContextCustomizerFactories} and
 * additional factories are registered declaratively via
 * {@link ContextCustomizerFactories @ContextCustomizerFactories}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 4.3
 */
@SpringJUnitConfig
@CustomizeWithFoo
@BootstrapWith(EnigmaTestContextBootstrapper.class)
@CustomizeWithBar
class ContextCustomizerTests {

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
		// Local Bootstrapper overrides spring.factories lookup
		assertThat(fruit).isNull();

		// From local Bootstrapper
		assertThat(enigma).isEqualTo(42);

		// From local @ContextCustomizerFactories
		assertThat(foo).isEqualTo("bar");

		// @ContextCustomizerFactories is not currently supported as a repeatable annotation.
		assertThat(bar).isNull();
	}


	static class EnigmaTestContextBootstrapper extends DefaultTestContextBootstrapper {

		@Override
		protected List<ContextCustomizerFactory> getContextCustomizerFactories() {
			return List.of(new EnigmaContextCustomizerFactory());
		}
	}

}
