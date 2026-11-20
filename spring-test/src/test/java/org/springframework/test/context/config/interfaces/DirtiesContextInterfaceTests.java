/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config.interfaces;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.springframework.test.context.cache.ContextCacheTestUtils.assertContextCacheStatistics;
import static org.springframework.test.context.cache.ContextCacheTestUtils.resetContextCache;

/**
 * @author Sam Brannen
 * @since 4.3
 */
class DirtiesContextInterfaceTests {

	private static final AtomicInteger cacheHits = new AtomicInteger();
	private static final AtomicInteger cacheMisses = new AtomicInteger();


	@BeforeAll
	static void verifyInitialCacheState() {
		resetContextCache();
		// Reset static counters in case tests are run multiple times in a test suite --
		// for example, via JUnit's @Suite.
		cacheHits.set(0);
		cacheMisses.set(0);
		assertContextCacheStatistics("BeforeClass", 0, 0, cacheHits.get(), cacheMisses.get());
	}

	@AfterAll
	static void verifyFinalCacheState() {
		assertContextCacheStatistics("AfterClass", 0, 0, cacheHits.get(), cacheMisses.get());
	}

	@Test
	void verifyDirtiesContextBehavior() throws Exception {
		runTestClassAndAssertStats(ClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase.class, 1);
		assertContextCacheStatistics("after class-level @DirtiesContext with clean test method and default class mode",
			0, 0, cacheHits.get(), cacheMisses.incrementAndGet());
	}

	private void runTestClassAndAssertStats(Class<?> testClass, int expectedTestCount) throws Exception {
		EngineTestKit.engine("junit-jupiter")
			.selectors(selectClass(testClass))
			.execute()
			.testEvents()
			.assertStatistics(stats -> stats.started(expectedTestCount).succeeded(expectedTestCount).failed(0));
	}

	@ExtendWith(SpringExtension.class)
	// Ensure that we do not include the EventPublishingTestExecutionListener
	// since it will access the ApplicationContext for each method in the
	// TestExecutionListener API, thus distorting our cache hit/miss results.
	@TestExecutionListeners({
		DirtiesContextBeforeModesTestExecutionListener.class,
		DependencyInjectionTestExecutionListener.class,
		DirtiesContextTestExecutionListener.class
	})
	public static class ClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase
			implements DirtiesContextTestInterface {

		@Autowired
		ApplicationContext applicationContext;


		@Test
		void verifyContextWasAutowired() {
			assertThat(this.applicationContext).as("The application context should have been autowired.").isNotNull();
		}


		@Configuration(proxyBeanMethods = false)
		static class Config {
			/* no beans */
		}

	}

}
