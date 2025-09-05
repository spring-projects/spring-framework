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

package org.springframework.cache.annotation;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for annotation-based caching methods that use reactive operators.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 6.1
 */
class ReactiveCachingTests {

	@ParameterizedTest
	@ValueSource(classes = {EarlyCacheHitDeterminationConfig.class,
			EarlyCacheHitDeterminationWithoutNullValuesConfig.class,
			LateCacheHitDeterminationConfig.class,
			LateCacheHitDeterminationWithValueWrapperConfig.class})
	void cacheHitDetermination(Class<?> configClass) {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				configClass, ReactiveCacheableService.class);
		ReactiveCacheableService service = ctx.getBean(ReactiveCacheableService.class);

		Object key = new Object();

		Long r1 = service.cacheFuture(key).join();
		Long r2 = service.cacheFuture(key).join();
		Long r3 = service.cacheFuture(key).join();

		assertThat(r1).isNotNull();
		assertThat(r1).as("cacheFuture").isSameAs(r2).isSameAs(r3);

		key = new Object();

		r1 = service.cacheMono(key).block();
		r2 = service.cacheMono(key).block();
		r3 = service.cacheMono(key).block();

		assertThat(r1).isNotNull();
		assertThat(r1).as("cacheMono").isSameAs(r2).isSameAs(r3);

		key = new Object();

		r1 = service.cacheFlux(key).blockFirst();
		r2 = service.cacheFlux(key).blockFirst();
		r3 = service.cacheFlux(key).blockFirst();

		assertThat(r1).isNotNull();
		assertThat(r1).as("cacheFlux blockFirst").isSameAs(r2).isSameAs(r3);

		key = new Object();

		List<Long> l1 = service.cacheFlux(key).collectList().block();
		List<Long> l2 = service.cacheFlux(key).collectList().block();
		List<Long> l3 = service.cacheFlux(key).collectList().block();

		assertThat(l1).isNotNull();
		assertThat(l1).as("cacheFlux collectList").isEqualTo(l2).isEqualTo(l3);

		key = new Object();

		r1 = service.cacheMono(key).block();
		r2 = service.cacheMono(key).block();
		r3 = service.cacheMono(key).block();

		assertThat(r1).isNotNull();
		assertThat(r1).as("cacheMono common key").isSameAs(r2).isSameAs(r3);

		// Same key as for Mono, reusing its cached value

		r1 = service.cacheFlux(key).blockFirst();
		r2 = service.cacheFlux(key).blockFirst();
		r3 = service.cacheFlux(key).blockFirst();

		assertThat(r1).isNotNull();
		assertThat(r1).as("cacheFlux blockFirst common key").isSameAs(r2).isSameAs(r3);

		ctx.close();
	}

	@ParameterizedTest
	@ValueSource(classes = {EarlyCacheHitDeterminationConfig.class,
			EarlyCacheHitDeterminationWithoutNullValuesConfig.class,
			LateCacheHitDeterminationConfig.class,
			LateCacheHitDeterminationWithValueWrapperConfig.class})
	void fluxCacheDoesntDependOnFirstRequest(Class<?> configClass) {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				configClass, ReactiveCacheableService.class);
		ReactiveCacheableService service = ctx.getBean(ReactiveCacheableService.class);

		Object key = new Object();

		List<Long> l1 = service.cacheFlux(key).take(1L, true).collectList().block();
		List<Long> l2 = service.cacheFlux(key).take(3L, true).collectList().block();
		List<Long> l3 = service.cacheFlux(key).collectList().block();

		Long first = l1.get(0);

		assertThat(l1).as("l1").containsExactly(first);
		assertThat(l2).as("l2").containsExactly(first, 0L, -1L);
		assertThat(l3).as("l3").containsExactly(first, 0L, -1L, -2L, -3L);

		ctx.close();
	}

	@Test
	void cacheErrorHandlerWithSimpleCacheErrorHandler() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				ExceptionCacheManager.class, ReactiveCacheableService.class);
		ReactiveCacheableService service = ctx.getBean(ReactiveCacheableService.class);

		assertThatExceptionOfType(CompletionException.class)
				.isThrownBy(() -> service.cacheFuture(new Object()).join())
				.withCauseInstanceOf(UnsupportedOperationException.class);

		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> service.cacheMono(new Object()).block());

		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> service.cacheFlux(new Object()).blockFirst());
	}

	@Test
	void cacheErrorHandlerWithSimpleCacheErrorHandlerAndSync() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				ExceptionCacheManager.class, ReactiveSyncCacheableService.class);
		ReactiveSyncCacheableService service = ctx.getBean(ReactiveSyncCacheableService.class);

		assertThatExceptionOfType(CompletionException.class)
				.isThrownBy(() -> service.cacheFuture(new Object()).join())
				.withCauseInstanceOf(UnsupportedOperationException.class);

		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> service.cacheMono(new Object()).block());

		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> service.cacheFlux(new Object()).blockFirst());
	}

	@Test
	void cacheErrorHandlerWithLoggingCacheErrorHandler() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				ExceptionCacheManager.class, ReactiveCacheableService.class, ErrorHandlerCachingConfiguration.class);
		ReactiveCacheableService service = ctx.getBean(ReactiveCacheableService.class);

		Long r1 = service.cacheFuture(new Object()).join();
		assertThat(r1).isNotNull();
		assertThat(r1).as("cacheFuture").isEqualTo(0L);

		r1 = service.cacheMono(new Object()).block();
		assertThat(r1).isNotNull();
		assertThat(r1).as("cacheMono").isEqualTo(1L);

		r1 = service.cacheFlux(new Object()).blockFirst();
		assertThat(r1).isNotNull();
		assertThat(r1).as("cacheFlux blockFirst").isEqualTo(2L);
	}

	@Test
	void cacheErrorHandlerWithLoggingCacheErrorHandlerAndSync() {
		AnnotationConfigApplicationContext ctx =
				new AnnotationConfigApplicationContext(ExceptionCacheManager.class, ReactiveSyncCacheableService.class, ErrorHandlerCachingConfiguration.class);
		ReactiveSyncCacheableService service = ctx.getBean(ReactiveSyncCacheableService.class);

		Long r1 = service.cacheFuture(new Object()).join();
		assertThat(r1).isNotNull();
		assertThat(r1).as("cacheFuture").isEqualTo(0L);

		r1 = service.cacheMono(new Object()).block();
		assertThat(r1).isNotNull();
		assertThat(r1).as("cacheMono").isEqualTo(1L);

		r1 = service.cacheFlux(new Object()).blockFirst();
		assertThat(r1).isNotNull();
		assertThat(r1).as("cacheFlux blockFirst").isEqualTo(2L);
	}

	@Test
	void cacheErrorHandlerWithLoggingCacheErrorHandlerAndOperationException() {
		AnnotationConfigApplicationContext ctx =
				new AnnotationConfigApplicationContext(EarlyCacheHitDeterminationConfig.class, ReactiveFailureCacheableService.class, ErrorHandlerCachingConfiguration.class);
		ReactiveFailureCacheableService service = ctx.getBean(ReactiveFailureCacheableService.class);

		assertThatExceptionOfType(CompletionException.class).isThrownBy(() -> service.cacheFuture(new Object()).join())
				.withMessage(IllegalStateException.class.getName() + ": future service error");

		StepVerifier.create(service.cacheMono(new Object()))
				.expectErrorMessage("mono service error")
				.verify();

		StepVerifier.create(service.cacheFlux(new Object()))
				.expectErrorMessage("flux service error")
				.verify();
	}

	@Test
	void cacheErrorHandlerWithLoggingCacheErrorHandlerAndOperationExceptionAndSync() {
		AnnotationConfigApplicationContext ctx =
				new AnnotationConfigApplicationContext(EarlyCacheHitDeterminationConfig.class, ReactiveSyncFailureCacheableService.class, ErrorHandlerCachingConfiguration.class);
		ReactiveSyncFailureCacheableService service = ctx.getBean(ReactiveSyncFailureCacheableService.class);

		assertThatExceptionOfType(CompletionException.class).isThrownBy(() -> service.cacheFuture(new Object()).join())
				.withMessage(IllegalStateException.class.getName() + ": future service error");

		StepVerifier.create(service.cacheMono(new Object()))
				.expectErrorMessage("mono service error")
				.verify();

		StepVerifier.create(service.cacheFlux(new Object()))
				.expectErrorMessage("flux service error")
				.verify();
	}


	@CacheConfig("first")
	static class ReactiveCacheableService {

		private final AtomicLong counter = new AtomicLong();

		@Cacheable
		CompletableFuture<Long> cacheFuture(Object arg) {
			return CompletableFuture.completedFuture(this.counter.getAndIncrement());
		}

		@Cacheable
		Mono<Long> cacheMono(Object arg) {
			// here counter not only reflects invocations of cacheMono but subscriptions to
			// the returned Mono as well. See https://github.com/spring-projects/spring-framework/issues/32370
			return Mono.defer(() -> Mono.just(this.counter.getAndIncrement()));
		}

		@Cacheable
		Flux<Long> cacheFlux(Object arg) {
			// here counter not only reflects invocations of cacheFlux but subscriptions to
			// the returned Flux as well. See https://github.com/spring-projects/spring-framework/issues/32370
			return Flux.defer(() -> Flux.just(this.counter.getAndIncrement(), 0L, -1L, -2L, -3L));
		}
	}


	@CacheConfig("first")
	static class ReactiveSyncCacheableService {

		private final AtomicLong counter = new AtomicLong();

		@Cacheable(sync = true)
		CompletableFuture<Long> cacheFuture(Object arg) {
			return CompletableFuture.completedFuture(this.counter.getAndIncrement());
		}

		@Cacheable(sync = true)
		Mono<Long> cacheMono(Object arg) {
			return Mono.defer(() -> Mono.just(this.counter.getAndIncrement()));
		}

		@Cacheable(sync = true)
		Flux<Long> cacheFlux(Object arg) {
			return Flux.defer(() -> Flux.just(this.counter.getAndIncrement(), 0L, -1L, -2L, -3L));
		}
	}


	@CacheConfig("first")
	static class ReactiveFailureCacheableService {

		private final AtomicBoolean cacheFutureInvoked = new AtomicBoolean();

		private final AtomicBoolean cacheMonoInvoked = new AtomicBoolean();

		private final AtomicBoolean cacheFluxInvoked = new AtomicBoolean();

		@Cacheable
		CompletableFuture<Long> cacheFuture(Object arg) {
			if (!this.cacheFutureInvoked.compareAndSet(false, true)) {
				return CompletableFuture.failedFuture(new IllegalStateException("future service invoked twice"));
			}
			return CompletableFuture.failedFuture(new IllegalStateException("future service error"));
		}

		@Cacheable
		Mono<Long> cacheMono(Object arg) {
			if (!this.cacheMonoInvoked.compareAndSet(false, true)) {
				return Mono.error(new IllegalStateException("mono service invoked twice"));
			}
			return Mono.error(new IllegalStateException("mono service error"));
		}

		@Cacheable
		Flux<Long> cacheFlux(Object arg) {
			if (!this.cacheFluxInvoked.compareAndSet(false, true)) {
				return Flux.error(new IllegalStateException("flux service invoked twice"));
			}
			return Flux.error(new IllegalStateException("flux service error"));
		}
	}


	@CacheConfig(cacheNames = "first")
	static class ReactiveSyncFailureCacheableService {

		private final AtomicBoolean cacheFutureInvoked = new AtomicBoolean();

		private final AtomicBoolean cacheMonoInvoked = new AtomicBoolean();

		private final AtomicBoolean cacheFluxInvoked = new AtomicBoolean();

		@Cacheable(sync = true)
		CompletableFuture<Long> cacheFuture(Object arg) {
			if (!this.cacheFutureInvoked.compareAndSet(false, true)) {
				return CompletableFuture.failedFuture(new IllegalStateException("future service invoked twice"));
			}
			return CompletableFuture.failedFuture(new IllegalStateException("future service error"));
		}

		@Cacheable(sync = true)
		Mono<Long> cacheMono(Object arg) {
			if (!this.cacheMonoInvoked.compareAndSet(false, true)) {
				return Mono.error(new IllegalStateException("mono service invoked twice"));
			}
			return Mono.error(new IllegalStateException("mono service error"));
		}

		@Cacheable(sync = true)
		Flux<Long> cacheFlux(Object arg) {
			if (!this.cacheFluxInvoked.compareAndSet(false, true)) {
				return Flux.error(new IllegalStateException("flux service invoked twice"));
			}
			return Flux.error(new IllegalStateException("flux service error"));
		}
	}


	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class EarlyCacheHitDeterminationConfig {

		@Bean
		CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("first");
		}
	}


	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class EarlyCacheHitDeterminationWithoutNullValuesConfig {

		@Bean
		CacheManager cacheManager() {
			ConcurrentMapCacheManager cm = new ConcurrentMapCacheManager("first");
			cm.setAllowNullValues(false);
			return cm;
		}
	}


	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class LateCacheHitDeterminationConfig {

		@Bean
		CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("first") {
				@Override
				protected Cache createConcurrentMapCache(String name) {
					return new ConcurrentMapCache(name, isAllowNullValues()) {
						@Override
						public CompletableFuture<?> retrieve(Object key) {
							return CompletableFuture.completedFuture(lookup(key));
						}
						@Override
						public void put(Object key, @Nullable Object value) {
							assertThat(get(key)).as("Double put").isNull();
							super.put(key, value);
						}
					};
				}
			};
		}
	}


	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class LateCacheHitDeterminationWithValueWrapperConfig {

		@Bean
		CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("first") {
				@Override
				protected Cache createConcurrentMapCache(String name) {
					return new ConcurrentMapCache(name, isAllowNullValues()) {
						@Override
						public CompletableFuture<?> retrieve(Object key) {
							Object value = lookup(key);
							return CompletableFuture.completedFuture(value != null ? toValueWrapper(value) : null);
						}
						@Override
						public void put(Object key, @Nullable Object value) {
							assertThat(get(key)).as("Double put").isNull();
							super.put(key, value);
						}
					};
				}
			};
		}
	}


	@Configuration
	static class ErrorHandlerCachingConfiguration implements CachingConfigurer {

		@Bean
		@Override
		public CacheErrorHandler errorHandler() {
			return new LoggingCacheErrorHandler();
		}
	}


	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class ExceptionCacheManager {

		@Bean
		CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("first") {
				@Override
				protected Cache createConcurrentMapCache(String name) {
					return new ConcurrentMapCache(name, isAllowNullValues()) {
						@Override
						public CompletableFuture<?> retrieve(Object key) {
							return CompletableFuture.failedFuture(new UnsupportedOperationException("Test exception on retrieve"));
						}
						@Override
						public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
							return CompletableFuture.failedFuture(new UnsupportedOperationException("Test exception on retrieve"));
						}
						@Override
						public void put(Object key, @Nullable Object value) {
							throw new UnsupportedOperationException("Test exception on put");
						}
					};
				}
			};
		}
	}

}
