/*
 * Copyright 2002-2023 the original author or authors.
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.springframework.test.context.cache.ContextCacheTestUtils.assertContextCacheStatistics;
import static org.springframework.test.context.cache.ContextCacheTestUtils.resetContextCache;

/**
 * @author Sam Brannen
 * @since 6.1
 */
@SpringJUnitConfig
@TestMethodOrder(OrderAnnotation.class)
@Disabled
class ContextFailureThresholdTests {

	@BeforeAll
	static void verifyInitialCacheState() {
		resetContextCache();
		assertContextCacheStatistics("BeforeAll", 0, 0, 0);
	}

	@AfterAll
	static void verifyFinalCacheState() {
		assertContextCacheStatistics("AfterAll", 0, 0, 3);
		resetContextCache();
	}

	@Test
	@Order(1)
	void test1() {
	}

	@Test
	@Order(2)
	void test2() {
	}

	@Test
	@Order(3)
	void test3() {
	}


	@Configuration
	static class FailingConfig {

		@Bean
		String explosiveString() {
			throw new RuntimeException("Boom!");
		}
	}

}
