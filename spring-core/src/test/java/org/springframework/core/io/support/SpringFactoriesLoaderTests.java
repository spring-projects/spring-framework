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

package org.springframework.core.io.support;

import java.lang.reflect.Modifier;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SpringFactoriesLoader}.
 *
 * @author Arjen Poutsma
 * @author Phillip Webb
 * @author Sam Brannen
 */
class SpringFactoriesLoaderTests {

	@Test
	void loadFactoriesInCorrectOrder() {
		List<DummyFactory> factories = SpringFactoriesLoader.loadFactories(DummyFactory.class, null);
		assertThat(factories.size()).isEqualTo(2);
		boolean condition1 = factories.get(0) instanceof MyDummyFactory1;
		assertThat(condition1).isTrue();
		boolean condition = factories.get(1) instanceof MyDummyFactory2;
		assertThat(condition).isTrue();
	}

	@Test
	void loadPackagePrivateFactory() {
		List<DummyPackagePrivateFactory> factories =
				SpringFactoriesLoader.loadFactories(DummyPackagePrivateFactory.class, null);
		assertThat(factories.size()).isEqualTo(1);
		assertThat(Modifier.isPublic(factories.get(0).getClass().getModifiers())).isFalse();
	}

	@Test
	void attemptToLoadFactoryOfIncompatibleType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				SpringFactoriesLoader.loadFactories(String.class, null))
			.withMessageContaining("Unable to instantiate factory class "
					+ "[org.springframework.core.io.support.MyDummyFactory1] for factory type [java.lang.String]");
	}

}
