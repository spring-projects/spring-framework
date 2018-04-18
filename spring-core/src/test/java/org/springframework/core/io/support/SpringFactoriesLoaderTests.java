/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.io.support;

import java.lang.reflect.Modifier;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link SpringFactoriesLoader}.
 *
 * @author Arjen Poutsma
 * @author Phillip Webb
 */
public class SpringFactoriesLoaderTests {

	@Test
	public void loadFactoriesInCorrectOrder() {
		List<DummyFactory> factories = SpringFactoriesLoader
				.loadFactories(DummyFactory.class, null);
		assertEquals(2, factories.size());
		assertTrue(factories.get(0) instanceof MyDummyFactory1);
		assertTrue(factories.get(1) instanceof MyDummyFactory2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void loadInvalid() {
		SpringFactoriesLoader.loadFactories(String.class, null);
	}

	@Test
	public void loadPackagePrivateFactory() throws Exception {
		List<DummyPackagePrivateFactory> factories = SpringFactoriesLoader
				.loadFactories(DummyPackagePrivateFactory.class, null);
		assertEquals(1, factories.size());
		assertTrue((factories.get(0).getClass().getModifiers() & Modifier.PUBLIC) == 0);
	}

}
