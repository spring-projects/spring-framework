/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.cache;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.cache.ContextCacheTestUtils.assertContextCacheStatistics;
import static org.springframework.test.context.cache.ContextCacheTestUtils.resetContextCache;

/**
 * JUnit based integration test which verifies correct {@linkplain ContextCache
 * application context caching} in conjunction with the {@link SpringExtension} and
 * {@link DirtiesContext @DirtiesContext} at the method level.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see ContextCacheTests
 * @see LruContextCacheTests
 */
@SpringJUnitConfig(locations = "../config/CoreContextConfigurationAppCtxTests-context.xml")
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SpringExtensionContextCacheTests {

	private static ApplicationContext dirtiedApplicationContext;

	@Autowired
	ApplicationContext applicationContext;

	@BeforeAll
	static void verifyInitialCacheState() {
		dirtiedApplicationContext = null;
		resetContextCache();
		assertContextCacheStatistics("BeforeClass", 0, 0, 0, 0);
	}

	@AfterAll
	static void verifyFinalCacheState() {
		assertContextCacheStatistics("AfterClass", 1, 1, 1, 2);
	}

	@Test
	@DirtiesContext
	@Order(1)
	void dirtyContext() {
		assertContextCacheStatistics("dirtyContext()", 1, 1, 0, 1);
		assertThat(this.applicationContext).as("The application context should have been autowired.").isNotNull();
		SpringExtensionContextCacheTests.dirtiedApplicationContext = this.applicationContext;
	}

	@Test
	@Order(2)
	void verifyContextDirty() {
		assertContextCacheStatistics("verifyContextWasDirtied()", 1, 1, 0, 2);
		assertThat(this.applicationContext).as("The application context should have been autowired.").isNotNull();
		assertThat(this.applicationContext).as("The application context should have been 'dirtied'.").isNotSameAs(SpringExtensionContextCacheTests.dirtiedApplicationContext);
		SpringExtensionContextCacheTests.dirtiedApplicationContext = this.applicationContext;
	}

	@Test
	@Order(3)
	void verifyContextNotDirty() {
		assertContextCacheStatistics("verifyContextWasNotDirtied()", 1, 1, 1, 2);
		assertThat(this.applicationContext).as("The application context should have been autowired.").isNotNull();
		assertThat(this.applicationContext).as("The application context should NOT have been 'dirtied'.").isSameAs(SpringExtensionContextCacheTests.dirtiedApplicationContext);
	}

}
