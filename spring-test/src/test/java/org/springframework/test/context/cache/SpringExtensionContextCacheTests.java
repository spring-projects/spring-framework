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
 * Unit tests which verify correct {@link ContextCache
 * application context caching} in conjunction with the
 * {@link SpringExtension} and the {@link DirtiesContext
 * &#064;DirtiesContext} annotation at the method level.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see ContextCacheTests
 * @see LruContextCacheTests
 */
@SpringJUnitConfig(locations = "../junit4/SpringJUnit4ClassRunnerAppCtxTests-context.xml")
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
		assertContextCacheStatistics("BeforeClass", 0, 0, 0);
	}

	@AfterAll
	static void verifyFinalCacheState() {
		assertContextCacheStatistics("AfterClass", 1, 1, 2);
	}

	@Test
	@DirtiesContext
	@Order(1)
	void dirtyContext() {
		assertContextCacheStatistics("dirtyContext()", 1, 0, 1);
		assertThat(this.applicationContext).as("The application context should have been autowired.").isNotNull();
		SpringExtensionContextCacheTests.dirtiedApplicationContext = this.applicationContext;
	}

	@Test
	@Order(2)
	void verifyContextDirty() {
		assertContextCacheStatistics("verifyContextWasDirtied()", 1, 0, 2);
		assertThat(this.applicationContext).as("The application context should have been autowired.").isNotNull();
		assertThat(this.applicationContext).as("The application context should have been 'dirtied'.").isNotSameAs(SpringExtensionContextCacheTests.dirtiedApplicationContext);
		SpringExtensionContextCacheTests.dirtiedApplicationContext = this.applicationContext;
	}

	@Test
	@Order(3)
	void verifyContextNotDirty() {
		assertContextCacheStatistics("verifyContextWasNotDirtied()", 1, 1, 2);
		assertThat(this.applicationContext).as("The application context should have been autowired.").isNotNull();
		assertThat(this.applicationContext).as("The application context should NOT have been 'dirtied'.").isSameAs(SpringExtensionContextCacheTests.dirtiedApplicationContext);
	}

}
