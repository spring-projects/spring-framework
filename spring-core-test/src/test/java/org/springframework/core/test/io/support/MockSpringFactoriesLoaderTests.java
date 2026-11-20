/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.test.io.support;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockSpringFactoriesLoader}.
 *
 * @author Phillip Webb
 */
class MockSpringFactoriesLoaderTests {

	@Test
	void addWithClassesAddsFactories() {
		MockSpringFactoriesLoader loader = new MockSpringFactoriesLoader();
		loader.add(TestFactoryType.class, TestFactoryOne.class, TestFactoryTwo.class);
		assertThatLoaderHasTestFactories(loader);
	}

	@Test
	void addWithClassNamesAddsFactories() {
		MockSpringFactoriesLoader loader = new MockSpringFactoriesLoader();
		loader.add(TestFactoryType.class.getName(), TestFactoryOne.class.getName(), TestFactoryTwo.class.getName());
		assertThatLoaderHasTestFactories(loader);
	}

	@Test
	void addWithClassAndInstancesAddsFactories() {
		MockSpringFactoriesLoader loader = new MockSpringFactoriesLoader();
		loader.addInstance(TestFactoryType.class, new TestFactoryOne(), new TestFactoryTwo());
		assertThatLoaderHasTestFactories(loader);
	}

	@Test
	void addWithClassNameAndInstancesAddsFactories() {
		MockSpringFactoriesLoader loader = new MockSpringFactoriesLoader();
		loader.addInstance(TestFactoryType.class.getName(), new TestFactoryOne(), new TestFactoryTwo());
		assertThatLoaderHasTestFactories(loader);
	}

	private void assertThatLoaderHasTestFactories(MockSpringFactoriesLoader loader) {
		List<TestFactoryType> factories = loader.load(TestFactoryType.class);
		assertThat(factories).satisfiesExactly(
				zero -> assertThat(zero).isInstanceOf(TestFactoryOne.class),
				one -> assertThat(one).isInstanceOf(TestFactoryTwo.class));
	}

	interface TestFactoryType {

	}

	@Order(1)
	static class TestFactoryOne implements TestFactoryType {

	}

	@Order(2)
	static class TestFactoryTwo implements TestFactoryType {

	}

}
