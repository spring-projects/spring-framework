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

package org.springframework.test.context.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextTestUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.context.cache.ContextCacheTestUtils.assertContextCacheStatistics;

/**
 * Tests for the LRU eviction policy in {@link DefaultContextCache}.
 *
 * @author Sam Brannen
 * @since 4.3
 * @see ContextCacheTests
 */
class LruContextCacheTests {

	private static final MergedContextConfiguration abcConfig = config(Abc.class);
	private static final MergedContextConfiguration fooConfig = config(Foo.class);
	private static final MergedContextConfiguration barConfig = config(Bar.class);
	private static final MergedContextConfiguration bazConfig = config(Baz.class);


	private final ConfigurableApplicationContext abcContext = mock();
	private final ConfigurableApplicationContext fooContext = mock();
	private final ConfigurableApplicationContext barContext = mock();
	private final ConfigurableApplicationContext bazContext = mock();


	@Test
	void maxCacheSizeNegativeOne() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DefaultContextCache(-1));
	}

	@Test
	void maxCacheSizeZero() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DefaultContextCache(0));
	}


	@Nested
	class PutUnitTests {

		@Test
		void maxCacheSizeOne() {
			DefaultContextCache cache = new DefaultContextCache(1);
			assertThat(cache.size()).isEqualTo(0);
			assertThat(cache.getMaxSize()).isEqualTo(1);

			cache.put(fooConfig, fooContext);
			assertCacheContents(cache, "Foo");

			cache.put(fooConfig, fooContext);
			assertCacheContents(cache, "Foo");

			cache.put(barConfig, barContext);
			assertCacheContents(cache, "Bar");

			cache.put(fooConfig, fooContext);
			assertCacheContents(cache, "Foo");
		}

		@Test
		void maxCacheSizeThree() {
			DefaultContextCache cache = new DefaultContextCache(3);
			assertThat(cache.size()).isEqualTo(0);
			assertThat(cache.getMaxSize()).isEqualTo(3);

			cache.put(fooConfig, fooContext);
			assertCacheContents(cache, "Foo");

			cache.put(fooConfig, fooContext);
			assertCacheContents(cache, "Foo");

			cache.put(barConfig, barContext);
			assertCacheContents(cache, "Foo", "Bar");

			cache.put(bazConfig, bazContext);
			assertCacheContents(cache, "Foo", "Bar", "Baz");

			cache.put(abcConfig, abcContext);
			assertCacheContents(cache, "Bar", "Baz", "Abc");
		}

		@Test
		void ensureLruOrderingIsUpdated() {
			DefaultContextCache cache = new DefaultContextCache(3);

			// Note: when a new entry is added it is considered the MRU entry and inserted at the tail.
			cache.put(fooConfig, fooContext);
			cache.put(barConfig, barContext);
			cache.put(bazConfig, bazContext);
			assertCacheContents(cache, "Foo", "Bar", "Baz");

			// Note: the MRU entry is moved to the tail when accessed.
			cache.get(fooConfig);
			assertCacheContents(cache, "Bar", "Baz", "Foo");

			cache.get(barConfig);
			assertCacheContents(cache, "Baz", "Foo", "Bar");

			cache.get(bazConfig);
			assertCacheContents(cache, "Foo", "Bar", "Baz");

			cache.get(barConfig);
			assertCacheContents(cache, "Foo", "Baz", "Bar");
		}

		@Test
		void ensureEvictedContextsAreClosed() {
			DefaultContextCache cache = new DefaultContextCache(2);

			cache.put(fooConfig, fooContext);
			cache.put(barConfig, barContext);
			assertCacheContents(cache, "Foo", "Bar");

			cache.put(bazConfig, bazContext);
			assertCacheContents(cache, "Bar", "Baz");
			verify(fooContext, times(1)).close();

			cache.put(abcConfig, abcContext);
			assertCacheContents(cache, "Baz", "Abc");
			verify(barContext, times(1)).close();

			verify(abcContext, never()).close();
			verify(bazContext, never()).close();
		}
	}

	/**
	 * @since 7.0
	 */
	@Nested
	class PutWithLoadFunctionUnitTests {

		@Test
		void maxCacheSizeOne() {
			DefaultContextCache cache = new DefaultContextCache(1);
			assertThat(cache.size()).isEqualTo(0);
			assertThat(cache.getMaxSize()).isEqualTo(1);

			cache.put(fooConfig, key -> fooContext);
			assertCacheContents(cache, "Foo");

			cache.put(fooConfig, key -> fooContext);
			assertCacheContents(cache, "Foo");

			cache.put(barConfig, key -> barContext);
			assertCacheContents(cache, "Bar");

			cache.put(fooConfig, key -> fooContext);
			assertCacheContents(cache, "Foo");
		}

		@Test
		void maxCacheSizeThree() {
			DefaultContextCache cache = new DefaultContextCache(3);
			assertThat(cache.size()).isEqualTo(0);
			assertThat(cache.getMaxSize()).isEqualTo(3);

			cache.put(fooConfig, key -> fooContext);
			assertCacheContents(cache, "Foo");

			cache.put(fooConfig, key -> fooContext);
			assertCacheContents(cache, "Foo");

			cache.put(barConfig, key -> barContext);
			assertCacheContents(cache, "Foo", "Bar");

			cache.put(bazConfig, key -> bazContext);
			assertCacheContents(cache, "Foo", "Bar", "Baz");

			cache.put(abcConfig, key -> abcContext);
			assertCacheContents(cache, "Bar", "Baz", "Abc");
		}

		@Test
		void ensureLruOrderingIsUpdated() {
			DefaultContextCache cache = new DefaultContextCache(3);

			// Note: when a new entry is added it is considered the MRU entry and inserted at the tail.
			cache.put(fooConfig, key -> fooContext);
			cache.put(barConfig, key -> barContext);
			cache.put(bazConfig, key -> bazContext);
			assertCacheContents(cache, "Foo", "Bar", "Baz");

			// Note: the MRU entry is moved to the tail when accessed.
			cache.get(fooConfig);
			assertCacheContents(cache, "Bar", "Baz", "Foo");

			cache.get(barConfig);
			assertCacheContents(cache, "Baz", "Foo", "Bar");

			cache.get(bazConfig);
			assertCacheContents(cache, "Foo", "Bar", "Baz");

			cache.get(barConfig);
			assertCacheContents(cache, "Foo", "Baz", "Bar");
		}

		@Test
		void ensureEvictedContextsAreClosed() {
			DefaultContextCache cache = new DefaultContextCache(2);

			cache.put(fooConfig, key -> fooContext);
			cache.put(barConfig, key -> barContext);
			assertCacheContents(cache, "Foo", "Bar");

			cache.put(bazConfig, key -> bazContext);
			assertCacheContents(cache, "Bar", "Baz");
			verify(fooContext, times(1)).close();

			cache.put(abcConfig, key -> abcContext);
			assertCacheContents(cache, "Baz", "Abc");
			verify(barContext, times(1)).close();

			verify(abcContext, never()).close();
			verify(bazContext, never()).close();
		}
	}

	/**
	 * @since 7.0
	 */
	@Nested
	class PutWithLoadFunctionIntegrationTests {

		/**
		 * Mimics a database shared across application contexts.
		 */
		private static final Set<String> database = new HashSet<>();

		private static final List<String> events = new ArrayList<>();


		@BeforeEach
		@AfterEach
		void resetTracking() {
			resetEvents();
			DatabaseInitializer.counter.set(0);
			database.clear();
		}

		@Test
		void maxCacheSizeOne() {
			DefaultContextCache contextCache = new DefaultContextCache(1);

			// -----------------------------------------------------------------

			// Get ApplicationContext for TestCase1.
			Class<?> testClass1 = TestCase1.class;
			TestContext testContext1 = TestContextTestUtils.buildTestContext(testClass1, contextCache);
			testContext1.getApplicationContext();
			assertContextCacheStatistics(contextCache, testClass1.getSimpleName(), 1, 1, 0, 1);
			assertCacheContents(contextCache, "Config1");
			assertThat(database).containsExactly("enigma1");
			assertThat(events).containsExactly("START 1");
			resetEvents();

			// -----------------------------------------------------------------

			// Get ApplicationContext for TestCase2.
			Class<?> testClass2 = TestCase2.class;
			TestContext testContext2 = TestContextTestUtils.buildTestContext(testClass2, contextCache);
			testContext2.getApplicationContext();
			assertContextCacheStatistics(contextCache, testClass2.getSimpleName(), 1, 1, 0, 2);
			assertCacheContents(contextCache, "Config2");
			assertThat(database).containsExactly("enigma2");
			assertThat(events).containsExactly("CLOSE 1", "START 2");
			resetEvents();

			// -----------------------------------------------------------------

			// Get ApplicationContext for TestCase3.
			Class<?> testClass3 = TestCase3.class;
			TestContext testContext3 = TestContextTestUtils.buildTestContext(testClass3, contextCache);
			testContext3.getApplicationContext();
			assertContextCacheStatistics(contextCache, testClass3.getSimpleName(), 1, 1, 0, 3);
			assertCacheContents(contextCache, "Config3");
			assertThat(database).containsExactly("enigma3");
			assertThat(events).containsExactly("CLOSE 2", "START 3");
			resetEvents();

			// -----------------------------------------------------------------

			// Get ApplicationContext for TestCase1 again.
			testContext1.getApplicationContext();
			assertContextCacheStatistics(contextCache, testClass1.getSimpleName(), 1, 1, 0, 4);
			assertCacheContents(contextCache, "Config1");
			assertThat(database).containsExactly("enigma4");
			assertThat(events).containsExactly("CLOSE 3", "START 4");
			resetEvents();

			// -----------------------------------------------------------------

			testContext1.markApplicationContextDirty(HierarchyMode.EXHAUSTIVE);
			assertThat(events).containsExactly("CLOSE 4");
			assertThat(database).isEmpty();
			assertThat(contextCache.size()).isZero();
		}

		@Test
		void maxCacheSizeTwo() {
			DefaultContextCache contextCache = new DefaultContextCache(2);

			// -----------------------------------------------------------------

			// Get ApplicationContext for TestCase1.
			Class<?> testClass1 = TestCase1.class;
			TestContext testContext1 = TestContextTestUtils.buildTestContext(testClass1, contextCache);
			testContext1.getApplicationContext();
			assertContextCacheStatistics(contextCache, testClass1.getSimpleName(), 1, 1, 0, 1);
			testContext1.markApplicationContextUnused();
			assertContextCacheStatistics(contextCache, testClass1.getSimpleName(), 1, 0, 0, 1);
			assertCacheContents(contextCache, "Config1");
			assertThat(events).containsExactly("START 1");
			assertThat(database).containsExactly("enigma1");
			resetEvents();

			// -----------------------------------------------------------------

			// Get ApplicationContext for TestCase2.
			Class<?> testClass2 = TestCase2.class;
			TestContext testContext2 = TestContextTestUtils.buildTestContext(testClass2, contextCache);
			testContext2.getApplicationContext();
			assertContextCacheStatistics(contextCache, testClass2.getSimpleName(), 2, 1, 0, 2);
			testContext2.markApplicationContextUnused();
			assertContextCacheStatistics(contextCache, testClass2.getSimpleName(), 2, 0, 0, 2);
			assertCacheContents(contextCache, "Config1", "Config2");
			assertThat(events).containsExactly("START 2");
			assertThat(database).containsExactly("enigma1", "enigma2");
			resetEvents();

			// -----------------------------------------------------------------

			// Get ApplicationContext for TestCase3.
			Class<?> testClass3 = TestCase3.class;
			TestContext testContext3 = TestContextTestUtils.buildTestContext(testClass3, contextCache);
			testContext3.getApplicationContext();
			assertContextCacheStatistics(contextCache, testClass3.getSimpleName(), 2, 1, 0, 3);
			testContext3.markApplicationContextUnused();
			assertContextCacheStatistics(contextCache, testClass3.getSimpleName(), 2, 0, 0, 3);
			assertCacheContents(contextCache, "Config2", "Config3");
			assertThat(events).containsExactly("CLOSE 1", "START 3");
			// Closing App #1 removed "enigma1" and "enigma2" from the database.
			assertThat(database).containsExactly("enigma3");
			resetEvents();

			// -----------------------------------------------------------------

			// Get ApplicationContext for TestCase1 again.
			testContext1.getApplicationContext();
			assertContextCacheStatistics(contextCache, testClass1.getSimpleName(), 2, 1, 0, 4);
			testContext1.markApplicationContextUnused();
			assertContextCacheStatistics(contextCache, testClass1.getSimpleName(), 2, 0, 0, 4);
			assertCacheContents(contextCache, "Config3", "Config1");
			assertThat(events).containsExactly("CLOSE 2", "START 4");
			// Closing App #2 removed "enigma3" from the database.
			assertThat(database).containsExactly("enigma4");
			resetEvents();

			// -----------------------------------------------------------------

			testContext3.markApplicationContextDirty(HierarchyMode.EXHAUSTIVE);
			assertThat(events).containsExactly("CLOSE 3");
			resetEvents();

			testContext1.markApplicationContextDirty(HierarchyMode.EXHAUSTIVE);
			assertThat(events).containsExactly("CLOSE 4");
			assertThat(database).isEmpty();
			assertThat(contextCache.size()).isZero();
		}


		private static void resetEvents() {
			events.clear();
		}


		/**
		 * Mimics a Spring component that inserts data into the database when the
		 * application context is started and drops data from a database when the
		 * application context is closed.
		 *
		 * @see org.springframework.jdbc.datasource.init.DataSourceInitializer
		 */
		static class DatabaseInitializer implements InitializingBean, DisposableBean {

			static final AtomicInteger counter = new AtomicInteger();

			private final int count;


			DatabaseInitializer() {
				this.count = counter.incrementAndGet();
			}

			@Override
			public void afterPropertiesSet() {
				events.add("START " + this.count);
				database.add("enigma" + this.count);
			}

			@Override
			public void destroy() {
				events.add("CLOSE " + this.count);
				database.clear();
			}
		}

		@SpringJUnitConfig
		static class TestCase1 {

			@Configuration
			@Import(DatabaseInitializer.class)
			static class Config1 {
			}
		}

		@SpringJUnitConfig
		static class TestCase2 {

			@Configuration
			@Import(DatabaseInitializer.class)
			static class Config2 {
			}
		}

		@SpringJUnitConfig
		static class TestCase3 {

			@Configuration
			@Import(DatabaseInitializer.class)
			static class Config3 {
			}
		}
	}


	private static MergedContextConfiguration config(Class<?> clazz) {
		return new MergedContextConfiguration(null, null, new Class<?>[] { clazz }, null, null);
	}

	private static void assertCacheContents(DefaultContextCache cache, String... expectedNames) {
		assertThat(cache).extracting("contextMap", as(map(MergedContextConfiguration.class, ApplicationContext.class)))
				.satisfies(contextMap -> {
					List<String> actualNames = contextMap.keySet().stream()
							.map(MergedContextConfiguration::getClasses)
							.flatMap(Arrays::stream)
							.map(Class::getSimpleName)
							.toList();
					assertThat(actualNames).containsExactly(expectedNames);
				});
	}


	private static class Abc {}
	private static class Foo {}
	private static class Bar {}
	private static class Baz {}

}
