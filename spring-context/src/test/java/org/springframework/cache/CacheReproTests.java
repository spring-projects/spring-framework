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

package org.springframework.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.AbstractCacheResolver;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests to reproduce raised caching issues.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
class CacheReproTests {

	@Test
	void spr11124MultipleAnnotations() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr11124Config.class);
		Spr11124Service bean = context.getBean(Spr11124Service.class);
		bean.single(2);
		bean.single(2);
		bean.multiple(2);
		bean.multiple(2);
		context.close();
	}

	@Test
	void spr11249PrimitiveVarargs() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr11249Config.class);
		Spr11249Service bean = context.getBean(Spr11249Service.class);
		Object result = bean.doSomething("op", 2, 3);
		assertThat(bean.doSomething("op", 2, 3)).isSameAs(result);
		context.close();
	}

	@Test
	void spr11592GetSimple() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr11592Config.class);
		Spr11592Service bean = context.getBean(Spr11592Service.class);
		Cache cache = context.getBean("cache", Cache.class);

		String key = "1";
		Object result = bean.getSimple("1");
		verify(cache, times(1)).get(key);  // first call: cache miss

		Object cachedResult = bean.getSimple("1");
		assertThat(cachedResult).isSameAs(result);
		verify(cache, times(2)).get(key);  // second call: cache hit

		context.close();
	}

	@Test
	void spr11592GetNeverCache() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr11592Config.class);
		Spr11592Service bean = context.getBean(Spr11592Service.class);
		Cache cache = context.getBean("cache", Cache.class);

		String key = "1";
		Object result = bean.getNeverCache("1");
		verify(cache, times(0)).get(key);  // no cache hit at all, caching disabled

		Object cachedResult = bean.getNeverCache("1");
		assertThat(cachedResult).isNotSameAs(result);
		verify(cache, times(0)).get(key);  // caching disabled

		context.close();
	}

	@Test
	void spr13081ConfigNoCacheNameIsRequired() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr13081Config.class);
		MyCacheResolver cacheResolver = context.getBean(MyCacheResolver.class);
		Spr13081Service bean = context.getBean(Spr13081Service.class);

		assertThat(cacheResolver.getCache("foo").get("foo")).isNull();
		Object result = bean.getSimple("foo");  // cache name = id
		assertThat(cacheResolver.getCache("foo").get("foo").get()).isEqualTo(result);

		context.close();
	}

	@Test
	void spr13081ConfigFailIfCacheResolverReturnsNullCacheName() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr13081Config.class);
		Spr13081Service bean = context.getBean(Spr13081Service.class);

		assertThatIllegalStateException().isThrownBy(() -> bean.getSimple(null))
				.withMessageContaining(MyCacheResolver.class.getName());
		context.close();
	}

	@Test
	void spr14230AdaptsToOptional() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr14230Config.class);
		Spr14230Service bean = context.getBean(Spr14230Service.class);
		Cache cache = context.getBean(CacheManager.class).getCache("itemCache");

		TestBean tb = new TestBean("tb1");
		bean.insertItem(tb);
		assertThat(bean.findById("tb1")).containsSame(tb);
		assertThat(cache.get("tb1").get()).isSameAs(tb);

		cache.clear();
		TestBean tb2 = bean.findById("tb1").get();
		assertThat(tb2).isNotSameAs(tb);
		assertThat(cache.get("tb1").get()).isSameAs(tb2);

		context.close();
	}

	@Test
	void spr14853AdaptsToOptionalWithSync() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr14853Config.class);
		Spr14853Service bean = context.getBean(Spr14853Service.class);
		Cache cache = context.getBean(CacheManager.class).getCache("itemCache");

		TestBean tb = new TestBean("tb1");
		bean.insertItem(tb);
		assertThat(bean.findById("tb1").get()).isSameAs(tb);
		assertThat(cache.get("tb1").get()).isSameAs(tb);

		cache.clear();
		TestBean tb2 = bean.findById("tb1").get();
		assertThat(tb2).isNotSameAs(tb);
		assertThat(cache.get("tb1").get()).isSameAs(tb2);

		context.close();
	}

	@Test
	void spr14235AdaptsToCompletableFuture() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(Spr14235Config.class, Spr14235FutureService.class);
		Spr14235FutureService bean = context.getBean(Spr14235FutureService.class);
		Cache cache = context.getBean(CacheManager.class).getCache("itemCache");

		TestBean tb = bean.findById("tb1").join();
		assertThat(tb).isNotNull();
		assertThat(bean.findById("tb1").join()).isSameAs(tb);
		assertThat(cache.get("tb1").get()).isSameAs(tb);

		bean.clear().join();
		TestBean tb2 = bean.findById("tb1").join();
		assertThat(tb2).isNotNull();
		assertThat(tb2).isNotSameAs(tb);
		assertThat(cache.get("tb1").get()).isSameAs(tb2);

		bean.clear().join();
		bean.insertItem(tb).join();
		assertThat(bean.findById("tb1").join()).isSameAs(tb);
		assertThat(cache.get("tb1").get()).isSameAs(tb);

		tb = bean.findById("tb2").join();
		assertThat(tb).isNotNull();
		assertThat(bean.findById("tb2").join()).isNotSameAs(tb);
		assertThat(cache.get("tb2")).isNull();

		assertThat(bean.findByIdEmpty("").join()).isNull();
		assertThat(cache.get("").get()).isNull();
		assertThat(bean.findByIdEmpty("").join()).isNull();

		context.close();
	}

	@Test
	void spr14235AdaptsToCompletableFutureWithSync() throws Exception {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(Spr14235Config.class, Spr14235FutureServiceSync.class);
		Spr14235FutureServiceSync bean = context.getBean(Spr14235FutureServiceSync.class);
		Cache cache = context.getBean(CacheManager.class).getCache("itemCache");

		TestBean tb = bean.findById("tb1").get();
		assertThat(bean.findById("tb1").get()).isSameAs(tb);
		assertThat(cache.get("tb1").get()).isSameAs(tb);

		cache.clear();
		TestBean tb2 = bean.findById("tb1").get();
		assertThat(tb2).isNotSameAs(tb);
		assertThat(cache.get("tb1").get()).isSameAs(tb2);

		cache.clear();
		bean.insertItem(tb);
		assertThat(bean.findById("tb1").get()).isSameAs(tb);
		assertThat(cache.get("tb1").get()).isSameAs(tb);

		assertThat(bean.findById("").join()).isNull();
		assertThat(cache.get("").get()).isNull();
		assertThat(bean.findById("").join()).isNull();

		context.close();
	}

	@Test
	void spr14235AdaptsToReactorMono() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(Spr14235Config.class, Spr14235MonoService.class);
		Spr14235MonoService bean = context.getBean(Spr14235MonoService.class);
		Cache cache = context.getBean(CacheManager.class).getCache("itemCache");

		TestBean tb = bean.findById("tb1").block();
		assertThat(tb).isNotNull();
		assertThat(bean.findById("tb1").block()).isSameAs(tb);
		assertThat(cache.get("tb1").get()).isSameAs(tb);

		bean.clear().block();
		TestBean tb2 = bean.findById("tb1").block();
		assertThat(tb2).isNotNull();
		assertThat(tb2).isNotSameAs(tb);
		assertThat(cache.get("tb1").get()).isSameAs(tb2);

		bean.clear().block();
		bean.insertItem(tb).block();
		assertThat(bean.findById("tb1").block()).isSameAs(tb);
		assertThat(cache.get("tb1").get()).isSameAs(tb);

		tb = bean.findById("tb2").block();
		assertThat(tb).isNotNull();
		assertThat(bean.findById("tb2").block()).isNotSameAs(tb);
		assertThat(cache.get("tb2")).isNull();

		assertThat(bean.findByIdEmpty("").block()).isNull();
		assertThat(cache.get("").get()).isNull();
		assertThat(bean.findByIdEmpty("").block()).isNull();

		context.close();
	}

	@Test
	void spr14235AdaptsToReactorMonoWithSync() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(Spr14235Config.class, Spr14235MonoServiceSync.class);
		Spr14235MonoServiceSync bean = context.getBean(Spr14235MonoServiceSync.class);
		Cache cache = context.getBean(CacheManager.class).getCache("itemCache");

		TestBean tb = bean.findById("tb1").block();
		assertThat(bean.findById("tb1").block()).isSameAs(tb);
		assertThat(cache.get("tb1").get()).isSameAs(tb);

		cache.clear();
		TestBean tb2 = bean.findById("tb1").block();
		assertThat(tb2).isNotSameAs(tb);
		assertThat(cache.get("tb1").get()).isSameAs(tb2);

		cache.clear();
		bean.insertItem(tb);
		assertThat(bean.findById("tb1").block()).isSameAs(tb);
		assertThat(cache.get("tb1").get()).isSameAs(tb);

		assertThat(bean.findById("").block()).isNull();
		assertThat(cache.get("").get()).isNull();
		assertThat(bean.findById("").block()).isNull();

		context.close();
	}

	@Test
	void spr14235AdaptsToReactorFlux() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(Spr14235Config.class, Spr14235FluxService.class);
		Spr14235FluxService bean = context.getBean(Spr14235FluxService.class);
		Cache cache = context.getBean(CacheManager.class).getCache("itemCache");

		List<TestBean> tb = bean.findById("tb1").collectList().block();
		assertThat(tb).isNotEmpty();
		assertThat(bean.findById("tb1").collectList().block()).isEqualTo(tb);
		assertThat(cache.get("tb1").get()).isEqualTo(tb);

		bean.clear().blockLast();
		List<TestBean> tb2 = bean.findById("tb1").collectList().block();
		assertThat(tb2).isNotEmpty();
		assertThat(tb2).isNotEqualTo(tb);
		assertThat(cache.get("tb1").get()).isEqualTo(tb2);

		bean.clear().blockLast();
		bean.insertItem("tb1", tb).blockLast();
		assertThat(bean.findById("tb1").collectList().block()).isEqualTo(tb);
		assertThat(cache.get("tb1").get()).isEqualTo(tb);

		tb = bean.findById("tb2").collectList().block();
		assertThat(tb).isNotEmpty();
		assertThat(bean.findById("tb2").collectList().block()).isNotEqualTo(tb);
		assertThat(cache.get("tb2")).isNull();

		assertThat(bean.findByIdEmpty("").collectList().block()).isEmpty();
		assertThat(cache.get("").get()).isEqualTo(Collections.emptyList());
		assertThat(bean.findByIdEmpty("").collectList().block()).isEmpty();

		context.close();
	}

	@Test
	void spr14235AdaptsToReactorFluxWithSync() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(Spr14235Config.class, Spr14235FluxServiceSync.class);
		Spr14235FluxServiceSync bean = context.getBean(Spr14235FluxServiceSync.class);
		Cache cache = context.getBean(CacheManager.class).getCache("itemCache");

		List<TestBean> tb = bean.findById("tb1").collectList().block();
		assertThat(bean.findById("tb1").collectList().block()).isEqualTo(tb);
		assertThat(cache.get("tb1").get()).isEqualTo(tb);

		cache.clear();
		List<TestBean> tb2 = bean.findById("tb1").collectList().block();
		assertThat(tb2).isNotEqualTo(tb);
		assertThat(cache.get("tb1").get()).isEqualTo(tb2);

		cache.clear();
		bean.insertItem("tb1", tb);
		assertThat(bean.findById("tb1").collectList().block()).isEqualTo(tb);
		assertThat(cache.get("tb1").get()).isEqualTo(tb);

		assertThat(bean.findById("").collectList().block()).isEmpty();
		assertThat(cache.get("").get()).isEqualTo(Collections.emptyList());
		assertThat(bean.findById("").collectList().block()).isEmpty();

		context.close();
	}

	@Test
	void spr15271FindsOnInterfaceWithInterfaceProxy() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr15271ConfigA.class);
		Spr15271Interface bean = context.getBean(Spr15271Interface.class);
		Cache cache = context.getBean(CacheManager.class).getCache("itemCache");

		TestBean tb = new TestBean("tb1");
		bean.insertItem(tb);
		assertThat(bean.findById("tb1").get()).isSameAs(tb);
		assertThat(cache.get("tb1").get()).isSameAs(tb);

		context.close();
	}

	@Test
	void spr15271FindsOnInterfaceWithCglibProxy() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr15271ConfigB.class);
		Spr15271Interface bean = context.getBean(Spr15271Interface.class);
		Cache cache = context.getBean(CacheManager.class).getCache("itemCache");

		TestBean tb = new TestBean("tb1");
		bean.insertItem(tb);
		assertThat(bean.findById("tb1").get()).isSameAs(tb);
		assertThat(cache.get("tb1").get()).isSameAs(tb);

		context.close();
	}


	@Configuration
	@EnableCaching
	public static class Spr11124Config {

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager();
		}

		@Bean
		public Spr11124Service service() {
			return new Spr11124ServiceImpl();
		}
	}


	public interface Spr11124Service {

		List<String> single(int id);

		List<String> multiple(int id);
	}


	public static class Spr11124ServiceImpl implements Spr11124Service {

		private int multipleCount = 0;

		@Override
		@Cacheable("smallCache")
		public List<String> single(int id) {
			if (this.multipleCount > 0) {
				throw new AssertionError("Called too many times");
			}
			this.multipleCount++;
			return Collections.emptyList();
		}

		@Override
		@Caching(cacheable = {
				@Cacheable(cacheNames = "bigCache", unless = "#result.size() < 4"),
				@Cacheable(cacheNames = "smallCache", unless = "#result.size() > 3")})
		public List<String> multiple(int id) {
			if (this.multipleCount > 0) {
				throw new AssertionError("Called too many times");
			}
			this.multipleCount++;
			return Collections.emptyList();
		}
	}


	@Configuration
	@EnableCaching
	public static class Spr11249Config {

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager();
		}

		@Bean
		public Spr11249Service service() {
			return new Spr11249Service();
		}
	}


	public static class Spr11249Service {

		@Cacheable("smallCache")
		public Object doSomething(String name, int... values) {
			return new Object();
		}
	}


	@Configuration
	@EnableCaching
	public static class Spr11592Config {

		@Bean
		public CacheManager cacheManager() {
			SimpleCacheManager cacheManager = new SimpleCacheManager();
			cacheManager.setCaches(Collections.singletonList(cache()));
			return cacheManager;
		}

		@Bean
		public Cache cache() {
			Cache cache = new ConcurrentMapCache("cache");
			return Mockito.spy(cache);
		}

		@Bean
		public Spr11592Service service() {
			return new Spr11592Service();
		}
	}


	public static class Spr11592Service {

		@Cacheable("cache")
		public Object getSimple(String key) {
			return new Object();
		}

		@Cacheable(cacheNames = "cache", condition = "false")
		public Object getNeverCache(String key) {
			return new Object();
		}
	}


	@Configuration
	@EnableCaching
	public static class Spr13081Config implements CachingConfigurer {

		@Bean
		@Override
		public CacheResolver cacheResolver() {
			return new MyCacheResolver();
		}

		@Bean
		public Spr13081Service service() {
			return new Spr13081Service();
		}
	}


	public static class MyCacheResolver extends AbstractCacheResolver {

		public MyCacheResolver() {
			super(new ConcurrentMapCacheManager());
		}

		@Override
		protected @Nullable Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
			String cacheName = (String) context.getArgs()[0];
			if (cacheName != null) {
				return Collections.singleton(cacheName);
			}
			return null;
		}

		public Cache getCache(String name) {
			return getCacheManager().getCache(name);
		}
	}


	public static class Spr13081Service {

		@Cacheable
		public Object getSimple(String cacheName) {
			return new Object();
		}
	}


	public static class Spr14230Service {

		@Cacheable("itemCache")
		public Optional<TestBean> findById(String id) {
			return Optional.of(new TestBean(id));
		}

		@CachePut(cacheNames = "itemCache", key = "#item.name")
		public TestBean insertItem(TestBean item) {
			return item;
		}
	}


	@Configuration
	@EnableCaching
	public static class Spr14230Config {

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager();
		}

		@Bean
		public Spr14230Service service() {
			return new Spr14230Service();
		}
	}


	public static class Spr14235FutureService {

		private boolean emptyCalled;

		@Cacheable(value = "itemCache", unless = "#result.name == 'tb2'")
		public CompletableFuture<TestBean> findById(String id) {
			return CompletableFuture.completedFuture(new TestBean(id));
		}

		@Cacheable(value = "itemCache")
		public CompletableFuture<TestBean> findByIdEmpty(String id) {
			assertThat(emptyCalled).isFalse();
			emptyCalled = true;
			return CompletableFuture.completedFuture(null);
		}

		@CachePut(cacheNames = "itemCache", key = "#item.name")
		public CompletableFuture<TestBean> insertItem(TestBean item) {
			return CompletableFuture.completedFuture(item);
		}

		@CacheEvict(cacheNames = "itemCache", allEntries = true, condition = "#result > 0")
		public CompletableFuture<Integer> clear() {
			return CompletableFuture.completedFuture(1);
		}
	}


	public static class Spr14235FutureServiceSync {

		private boolean emptyCalled;

		@Cacheable(value = "itemCache", sync = true)
		public CompletableFuture<TestBean> findById(String id) {
			if (id.isEmpty()) {
				assertThat(emptyCalled).isFalse();
				emptyCalled = true;
				return CompletableFuture.completedFuture(null);
			}
			return CompletableFuture.completedFuture(new TestBean(id));
		}

		@CachePut(cacheNames = "itemCache", key = "#item.name")
		public TestBean insertItem(TestBean item) {
			return item;
		}
	}


	public static class Spr14235MonoService {

		private boolean emptyCalled;

		@Cacheable(value = "itemCache", unless = "#result.name == 'tb2'")
		public Mono<TestBean> findById(String id) {
			return Mono.just(new TestBean(id));
		}

		@Cacheable(value = "itemCache")
		public Mono<TestBean> findByIdEmpty(String id) {
			assertThat(emptyCalled).isFalse();
			emptyCalled = true;
			return Mono.empty();
		}

		@CachePut(cacheNames = "itemCache", key = "#item.name")
		public Mono<TestBean> insertItem(TestBean item) {
			return Mono.just(item);
		}

		@CacheEvict(cacheNames = "itemCache", allEntries = true, condition = "#result > 0")
		public Mono<Integer> clear() {
			return Mono.just(1);
		}
	}


	public static class Spr14235MonoServiceSync {

		private boolean emptyCalled;

		@Cacheable(value = "itemCache", sync = true)
		public Mono<TestBean> findById(String id) {
			if (id.isEmpty()) {
				assertThat(emptyCalled).isFalse();
				emptyCalled = true;
				return Mono.empty();
			}
			return Mono.just(new TestBean(id));
		}

		@CachePut(cacheNames = "itemCache", key = "#item.name")
		public TestBean insertItem(TestBean item) {
			return item;
		}
	}


	public static class Spr14235FluxService {

		private int counter = 0;

		private boolean emptyCalled;

		@Cacheable(value = "itemCache", unless = "#result[0].name == 'tb2'")
		public Flux<TestBean> findById(String id) {
			return Flux.just(new TestBean(id), new TestBean(id + (counter++)));
		}

		@Cacheable(value = "itemCache")
		public Flux<TestBean> findByIdEmpty(String id) {
			assertThat(emptyCalled).isFalse();
			emptyCalled = true;
			return Flux.empty();
		}

		@CachePut(cacheNames = "itemCache", key = "#id")
		public Flux<TestBean> insertItem(String id, List<TestBean> item) {
			return Flux.fromIterable(item);
		}

		@CacheEvict(cacheNames = "itemCache", allEntries = true, condition = "#result > 0")
		public Flux<Integer> clear() {
			return Flux.just(1);
		}
	}


	public static class Spr14235FluxServiceSync {

		private int counter = 0;

		private boolean emptyCalled;

		@Cacheable(value = "itemCache", sync = true)
		public Flux<TestBean> findById(String id) {
			if (id.isEmpty()) {
				assertThat(emptyCalled).isFalse();
				emptyCalled = true;
				return Flux.empty();
			}
			return Flux.just(new TestBean(id), new TestBean(id + (counter++)));
		}

		@CachePut(cacheNames = "itemCache", key = "#id")
		public List<TestBean> insertItem(String id, List<TestBean> item) {
			return item;
		}
	}


	@Configuration
	@EnableCaching
	public static class Spr14235Config {

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager();
		}
	}


	public static class Spr14853Service {

		@Cacheable(value = "itemCache", sync = true)
		public Optional<TestBean> findById(String id) {
			return Optional.of(new TestBean(id));
		}

		@CachePut(cacheNames = "itemCache", key = "#item.name")
		public TestBean insertItem(TestBean item) {
			return item;
		}
	}


	@Configuration
	@EnableCaching
	public static class Spr14853Config {

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager();
		}

		@Bean
		public Spr14853Service service() {
			return new Spr14853Service();
		}
	}


	public interface Spr15271Interface {

		@Cacheable(value = "itemCache", sync = true)
		Optional<TestBean> findById(String id);

		@CachePut(cacheNames = "itemCache", key = "#item.name")
		TestBean insertItem(TestBean item);
	}


	public static class Spr15271Service implements Spr15271Interface {

		@Override
		public Optional<TestBean> findById(String id) {
			return Optional.of(new TestBean(id));
		}

		@Override
		public TestBean insertItem(TestBean item) {
			return item;
		}
	}


	@Configuration
	@EnableCaching
	public static class Spr15271ConfigA {

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager();
		}

		@Bean
		public Spr15271Interface service() {
			return new Spr15271Service();
		}
	}


	@Configuration
	@EnableCaching(proxyTargetClass = true)
	public static class Spr15271ConfigB {

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager();
		}

		@Bean
		public Spr15271Interface service() {
			return new Spr15271Service();
		}
	}

}
