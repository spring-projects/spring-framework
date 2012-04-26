/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.context;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.JUnitCoreUtils;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;

/**
 * Test to verify {@link DirtiesContext} parent=true closes parent context as well as child contexts which share the same
 * parent context.
 *
 * @author Tadaya Tsuyukubo
 * @since 3.2
 */
public class DirtiesContextParentTests {

	private static Map<Class<? extends BaseTestClass>, ApplicationContext> map =
			new HashMap<Class<? extends BaseTestClass>, ApplicationContext>();

	/**
	 * {@link @Before} annotation is required. Gradle test task also runs test classes defined as static in this class.
	 * so before running the actual tests, need to cleanup the map.
	 */
	@Before
	@After
	public void cleanUp() {
		map.clear(); // release hard reference to the app context
	}

	@Test
	public void testClassLevelDirtiesContextWithParentTrue() {

		JUnitCore jUnitCore = new JUnitCore();
		// expects test class runs in this order...
		Result result = jUnitCore.run(ClassLevelTestFoo.class, ClassLevelTestBar.class, ClassLevelTestBaz.class);
		JUnitCoreUtils.verifyResult(result);

		assertThat(map.size(), is(3));
		assertThat(map.keySet(), hasItems(ClassLevelTestFoo.class, ClassLevelTestBar.class, ClassLevelTestBaz.class));

		ConfigurableApplicationContext fooContext = (ConfigurableApplicationContext) map.get(ClassLevelTestFoo.class);
		ConfigurableApplicationContext barContext = (ConfigurableApplicationContext) map.get(ClassLevelTestBar.class);
		ConfigurableApplicationContext bazContext = (ConfigurableApplicationContext) map.get(ClassLevelTestBaz.class);

		verifyAppContext(fooContext, true, true);
		verifyAppContext(barContext, true, true);
		verifyAppContext(bazContext, false, false);

	}

	@Test
	public void testMethodLevelDirtiesContextWithParentTrue() {

		JUnitCore jUnitCore = new JUnitCore();
		// expects test class runs in this order...
		Result result = jUnitCore.run(MethodLevelTestFoo.class, MethodLevelTestBar.class, MethodLevelTestBaz.class);
		JUnitCoreUtils.verifyResult(result);

		assertThat(map.size(), is(3));
		assertThat(map.keySet(),
				hasItems(MethodLevelTestFoo.class, MethodLevelTestBar.class, MethodLevelTestBaz.class));

		ConfigurableApplicationContext fooContext = (ConfigurableApplicationContext) map.get(MethodLevelTestFoo.class);
		ConfigurableApplicationContext barContext = (ConfigurableApplicationContext) map.get(MethodLevelTestBar.class);
		ConfigurableApplicationContext bazContext = (ConfigurableApplicationContext) map.get(MethodLevelTestBaz.class);

		verifyAppContext(fooContext, true, true);
		verifyAppContext(barContext, true, true);
		verifyAppContext(bazContext, false, false);
	}

	private void verifyAppContext(ConfigurableApplicationContext context, boolean isClosed, boolean isParentClosed) {
		final boolean isActive = !isClosed;
		final boolean isParentActive = !isParentClosed;

		assertThat("context is null.", context, notNullValue());

		ConfigurableApplicationContext parentContext = (ConfigurableApplicationContext) context.getParent();
		assertThat("context is null.", parentContext, notNullValue());

		assertThat("child context is active(not closed)", context.isActive(), equalTo(isActive));
		assertThat("parent context is active(not closed)", parentContext.isActive(), equalTo(isParentActive));

	}

	@RunWith(SpringJUnit4ClassRunner.class)
	@ParentContextConfiguration(classes = {Config.class})
	@ContextConfiguration(classes = {Config.class})
	public static abstract class BaseTestClass {

		@Autowired
		protected ApplicationContext applicationContext;

	}

	/** share the same parent context. */
	public static class ClassLevelTestFoo extends BaseTestClass {

		@Test
		public void testFoo() {
			map.put(getClass(), applicationContext);
		}
	}

	/**
	 * share the same parent context.
	 *
	 * DirtiesContext is set with parent=true. After running this test class, child contexts sharing the parent context and
	 * parent context will be closed.
	 */
	@DirtiesContext(parent = true)
	public static class ClassLevelTestBar extends BaseTestClass {

		@Test
		public void testBar() {
			map.put(getClass(), applicationContext);
		}

	}

	/** share the same parent context. */
	public static class ClassLevelTestBaz extends BaseTestClass {

		@Test
		public void testBaz() {
			map.put(getClass(), applicationContext);
		}
	}

	/** share the same parent context. */
	public static class MethodLevelTestFoo extends BaseTestClass {

		@Test
		public void testFoo() {
			map.put(getClass(), applicationContext);
		}
	}

	/**
	 * share the same parent context.
	 *
	 * DirtiesContext is set with parent=true on the method.
	 */
	public static class MethodLevelTestBar extends BaseTestClass {

		@Test
		@DirtiesContext(parent = true)
		public void testBar() {
			map.put(getClass(), applicationContext);
		}

	}

	/** share the same parent context. */
	public static class MethodLevelTestBaz extends BaseTestClass {

		@Test
		public void testBaz() {
			map.put(getClass(), applicationContext);
		}
	}

	@Configuration
	public static class Config {

		@Bean
		public String bean() {
			return "bean";
		}
	}
}
