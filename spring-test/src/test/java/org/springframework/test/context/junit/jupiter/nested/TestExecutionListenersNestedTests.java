/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.nested;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.nested.TestExecutionListenersNestedTests.FooTestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link TestExecutionListeners @TestExecutionListeners} in conjunction with the
 * {@link SpringExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 5.3
 */
@ExtendWith(SpringExtension.class)
@TestExecutionListeners(FooTestExecutionListener.class)
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
@DisabledInAotMode("Does not load an ApplicationContext and thus not supported for AOT processing")
class TestExecutionListenersNestedTests {

	private static final String FOO = "foo";
	private static final String BAR = "bar";
	private static final String BAZ = "baz";
	private static final String QUX = "qux";

	private static final List<String> listeners = new ArrayList<>();


	@AfterEach
	void resetListeners() {
		listeners.clear();
	}

	@Test
	void test() {
		assertThat(listeners).containsExactly(FOO);
	}


	@Nested
	@NestedTestConfiguration(INHERIT)
	class InheritedConfigTests {

		@Test
		void test() {
			assertThat(listeners).containsExactly(FOO);
		}
	}

	@Nested
	@TestExecutionListeners(BarTestExecutionListener.class)
	@DisabledInAotMode("Does not load an ApplicationContext and thus not supported for AOT processing")
	class ConfigOverriddenByDefaultTests {

		@Test
		void test() {
			assertThat(listeners).containsExactly(BAR);
		}
	}

	@Nested
	@NestedTestConfiguration(INHERIT)
	@TestExecutionListeners(BarTestExecutionListener.class)
	class InheritedAndExtendedConfigTests {

		@Test
		void test() {
			assertThat(listeners).containsExactly(FOO, BAR);
		}


		@Nested
		@NestedTestConfiguration(OVERRIDE)
		@TestExecutionListeners(BazTestExecutionListener.class)
		@DisabledInAotMode("Does not load an ApplicationContext and thus not supported for AOT processing")
		class DoubleNestedWithOverriddenConfigTests {

			@Test
			void test() {
				assertThat(listeners).containsExactly(BAZ);
			}


			@Nested
			@NestedTestConfiguration(INHERIT)
			@TestExecutionListeners(listeners = BarTestExecutionListener.class, inheritListeners = false)
			class TripleNestedWithInheritedConfigButOverriddenListenersTests {

				@Test
				void test() {
					assertThat(listeners).containsExactly(BAR);
				}
			}

			@Nested
			@NestedTestConfiguration(INHERIT)
			class TripleNestedWithInheritedConfigAndTestInterfaceTests implements TestInterface {

				@Test
				void test() {
					assertThat(listeners).containsExactly(BAZ, QUX);
				}
			}
		}
	}

	// -------------------------------------------------------------------------

	private abstract static class BaseTestExecutionListener extends AbstractTestExecutionListener {

		protected abstract String name();

		@Override
		public final void beforeTestClass(TestContext testContext) {
			listeners.add(name());
		}
	}

	static class FooTestExecutionListener extends BaseTestExecutionListener {

		@Override
		protected String name() {
			return FOO;
		}
	}

	static class BarTestExecutionListener extends BaseTestExecutionListener {

		@Override
		protected String name() {
			return BAR;
		}
	}

	static class BazTestExecutionListener extends BaseTestExecutionListener {

		@Override
		protected String name() {
			return BAZ;
		}
	}

	static class QuxTestExecutionListener extends BaseTestExecutionListener {

		@Override
		protected String name() {
			return QUX;
		}
	}

	@TestExecutionListeners(QuxTestExecutionListener.class)
	interface TestInterface {
	}

}
