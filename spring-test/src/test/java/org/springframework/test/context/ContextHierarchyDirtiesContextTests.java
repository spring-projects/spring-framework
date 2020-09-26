/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify proper behavior of {@link DirtiesContext @DirtiesContext}
 * in conjunction with context hierarchies configured via {@link ContextHierarchy @ContextHierarchy}.
 *
 * @author Sam Brannen
 * @author Tadaya Tsuyukubo
 * @since 3.2.2
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class ContextHierarchyDirtiesContextTests {

	private static ApplicationContext context;

	private static String foo;

	private static String bar;

	private static String baz;


	@AfterEach
	void cleanUp() {
		ContextHierarchyDirtiesContextTests.context = null;
		ContextHierarchyDirtiesContextTests.foo = null;
		ContextHierarchyDirtiesContextTests.bar = null;
		ContextHierarchyDirtiesContextTests.baz = null;
	}

	@Test
	void classLevelDirtiesContextWithCurrentLevelHierarchyMode() {
		runTestAndVerifyHierarchies(ClassLevelDirtiesContextWithCurrentLevelModeTestCase.class, true, true, false);
	}

	@Test
	void classLevelDirtiesContextWithExhaustiveHierarchyMode() {
		runTestAndVerifyHierarchies(ClassLevelDirtiesContextWithExhaustiveModeTestCase.class, false, false, false);
	}

	@Test
	void methodLevelDirtiesContextWithCurrentLevelHierarchyMode() {
		runTestAndVerifyHierarchies(MethodLevelDirtiesContextWithCurrentLevelModeTestCase.class, true, true, false);
	}

	@Test
	void methodLevelDirtiesContextWithExhaustiveHierarchyMode() {
		runTestAndVerifyHierarchies(MethodLevelDirtiesContextWithExhaustiveModeTestCase.class, false, false, false);
	}

	private void runTestAndVerifyHierarchies(Class<? extends FooTestCase> testClass, boolean isFooContextActive,
			boolean isBarContextActive, boolean isBazContextActive) {

		JUnitCore jUnitCore = new JUnitCore();
		Result result = jUnitCore.run(testClass);
		assertThat(result.wasSuccessful()).as("all tests passed").isTrue();

		assertThat(ContextHierarchyDirtiesContextTests.context).isNotNull();

		ConfigurableApplicationContext bazContext = (ConfigurableApplicationContext) ContextHierarchyDirtiesContextTests.context;
		assertThat(ContextHierarchyDirtiesContextTests.baz).isEqualTo("baz");
		assertThat(bazContext.isActive()).isEqualTo(isBazContextActive);

		ConfigurableApplicationContext barContext = (ConfigurableApplicationContext) bazContext.getParent();
		assertThat(barContext).isNotNull();
		assertThat(ContextHierarchyDirtiesContextTests.bar).isEqualTo("bar");
		assertThat(barContext.isActive()).isEqualTo(isBarContextActive);

		ConfigurableApplicationContext fooContext = (ConfigurableApplicationContext) barContext.getParent();
		assertThat(fooContext).isNotNull();
		assertThat(ContextHierarchyDirtiesContextTests.foo).isEqualTo("foo");
		assertThat(fooContext.isActive()).isEqualTo(isFooContextActive);
	}


	// -------------------------------------------------------------------------

	@RunWith(SpringRunner.class)
	@ContextHierarchy(@ContextConfiguration(name = "foo"))
	static abstract class FooTestCase implements ApplicationContextAware {

		@Configuration
		static class Config {

			@Bean
			String bean() {
				return "foo";
			}
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			ContextHierarchyDirtiesContextTests.context = applicationContext;
			ContextHierarchyDirtiesContextTests.baz = applicationContext.getBean("bean", String.class);
			ContextHierarchyDirtiesContextTests.bar = applicationContext.getParent().getBean("bean", String.class);
			ContextHierarchyDirtiesContextTests.foo = applicationContext.getParent().getParent().getBean("bean", String.class);
		}
	}

	@ContextHierarchy(@ContextConfiguration(name = "bar"))
	static abstract class BarTestCase extends FooTestCase {

		@Configuration
		static class Config {

			@Bean
			String bean() {
				return "bar";
			}
		}
	}

	@ContextHierarchy(@ContextConfiguration(name = "baz"))
	static abstract class BazTestCase extends BarTestCase {

		@Configuration
		static class Config {

			@Bean
			String bean() {
				return "baz";
			}
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * {@link DirtiesContext} is declared at the class level, without specifying
	 * the {@link DirtiesContext.HierarchyMode}.
	 * <p>After running this test class, the context cache should be <em>exhaustively</em>
	 * cleared beginning from the current context hierarchy, upwards to the highest
	 * parent context, and then back down through all subhierarchies of the parent
	 * context.
	 */
	@DirtiesContext
	public static class ClassLevelDirtiesContextWithExhaustiveModeTestCase extends BazTestCase {

		@org.junit.Test
		public void test() {
		}
	}

	/**
	 * {@link DirtiesContext} is declared at the class level, specifying the
	 * {@link DirtiesContext.HierarchyMode#CURRENT_LEVEL CURRENT_LEVEL} hierarchy mode.
	 * <p>After running this test class, the context cache should be cleared
	 * beginning from the current context hierarchy and down through all subhierarchies.
	 */
	@DirtiesContext(hierarchyMode = HierarchyMode.CURRENT_LEVEL)
	public static class ClassLevelDirtiesContextWithCurrentLevelModeTestCase extends BazTestCase {

		@org.junit.Test
		public void test() {
		}
	}

	/**
	 * {@link DirtiesContext} is declared at the method level, without specifying
	 * the {@link DirtiesContext.HierarchyMode}.
	 * <p>After running this test class, the context cache should be <em>exhaustively</em>
	 * cleared beginning from the current context hierarchy, upwards to the highest
	 * parent context, and then back down through all subhierarchies of the parent
	 * context.
	 */
	public static class MethodLevelDirtiesContextWithExhaustiveModeTestCase extends BazTestCase {

		@org.junit.Test
		@DirtiesContext
		public void test() {
		}
	}

	/**
	 * {@link DirtiesContext} is declared at the method level, specifying the
	 * {@link DirtiesContext.HierarchyMode#CURRENT_LEVEL CURRENT_LEVEL} hierarchy mode.
	 * <p>After running this test class, the context cache should be cleared
	 * beginning from the current context hierarchy and down through all subhierarchies.
	 */
	public static class MethodLevelDirtiesContextWithCurrentLevelModeTestCase extends BazTestCase {

		@org.junit.Test
		@DirtiesContext(hierarchyMode = HierarchyMode.CURRENT_LEVEL)
		public void test() {
		}
	}

}
