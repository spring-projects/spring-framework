/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.aot;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.mock.MockSpringFactoriesLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AotFactoriesLoader}.
 *
 * @author Phillip Webb
 */
class AotFactoriesLoaderTests {

	@Test
	void createWhenBeanFactoryIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new AotFactoriesLoader(null))
				.withMessage("'beanFactory' must not be null");
	}

	@Test
	void createWhenSpringFactoriesLoaderIsNullThrowsException() {
		ListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new AotFactoriesLoader(beanFactory, null))
				.withMessage("'factoriesLoader' must not be null");
	}

	@Test
	void loadLoadsFromBeanFactoryAndSpringFactoriesLoaderInOrder() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("b1", new TestFactoryImpl(0, "b1"));
		beanFactory.registerSingleton("b2", new TestFactoryImpl(2, "b2"));
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		springFactoriesLoader.addInstance(TestFactory.class, new TestFactoryImpl(1, "l1"));
		springFactoriesLoader.addInstance(TestFactory.class, new TestFactoryImpl(3, "l2"));
		AotFactoriesLoader loader = new AotFactoriesLoader(beanFactory, springFactoriesLoader);
		List<TestFactory> loaded = loader.load(TestFactory.class);
		assertThat(loaded).map(Object::toString).containsExactly("b1", "l1", "b2", "l2");
	}

	interface TestFactory {
	}

	static class TestFactoryImpl implements TestFactory, Ordered {

		private final int order;

		private final String name;

		TestFactoryImpl(int order, String name) {
			this.order = order;
			this.name = name;
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		@Override
		public String toString() {
			return this.name;
		}

	}

}
