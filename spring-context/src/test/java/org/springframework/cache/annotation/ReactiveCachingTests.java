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

package org.springframework.cache.annotation;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for annotation-based caching methods that use reactive operators.
 *
 * @author Stephane Nicoll
 */
public class ReactiveCachingTests {

	private final ConfigurableApplicationContext ctx;

	private final ReactiveCacheableService service;

	public ReactiveCachingTests() {
		this.ctx = new AnnotationConfigApplicationContext(TestConfig.class);
		this.service = this.ctx.getBean(ReactiveCacheableService.class);
	}

	@Test
	void cache() {
		Object key = new Object();
		Long r1 = this.service.cache(key).block();
		Long r2 = this.service.cache(key).block();
		Long r3 = this.service.cache(key).block();
		assertThat(r1).isSameAs(r2).isSameAs(r3);
	}


	@CacheConfig(cacheNames = "first")
	static class ReactiveCacheableService {

		private final AtomicLong counter = new AtomicLong();

		@Cacheable
		Mono<Long> cache(Object arg1) {
			return Mono.just(this.counter.getAndIncrement());
		}

	}


	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class TestConfig {

		@Bean
		CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("first");
		}

		@Bean
		ReactiveCacheableService reactiveCacheableService() {
			return new ReactiveCacheableService();
		}

	}

}
