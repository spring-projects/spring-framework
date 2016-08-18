/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *			http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rx.Observable;
import rx.Single;
import rx.observers.TestSubscriber;

import static org.junit.Assert.*;

/**
 * Tests to check RxJava support.
 *
 * @author Pablo Diaz-Lopez
 */
public class CacheRxJavaTests {
	private AnnotationConfigApplicationContext context;
	private TestService bean;

	@Before
	public void before() {
		context = new AnnotationConfigApplicationContext(BasicTestConfig.class);
		bean = context.getBean(TestService.class);
	}

	@Test
	public void single() {
		Cache cache = context.getBean(CacheManager.class).getCache("single");
		Single<Object> single = bean.single();
		TestSubscriber<Object> testSubscriber = TestSubscriber.create();

		single.subscribe(testSubscriber);
		testSubscriber.awaitTerminalEvent();

		Object cachedValues = cache.get(SimpleKey.EMPTY).get();

		assertTrue("Expected to cache value not wrapper", cachedValues instanceof TestBean);
		testSubscriber.assertValues(cachedValues);
	}

	@Test
	@Ignore("I think there is a problem with Single->Mono and/or Mono->Single")
	public void nullValue() {
		Cache cache = context.getBean(CacheManager.class).getCache("nullValue");
		Single<Object> nullValue = bean.nullValue();
		TestSubscriber<Object> testSubscriber = TestSubscriber.create();

		nullValue.subscribe(testSubscriber);
		testSubscriber.awaitTerminalEvent();

		Object cachedValues = cache.get(SimpleKey.EMPTY).get();

		assertNull("Null should be cached", cachedValues);
		testSubscriber.assertValue(null);
	}

	@Test
	public void throwable() {
		Cache cache = context.getBean(CacheManager.class).getCache("throwable");
		Single<Object> throwable = bean.nullValue();
		TestSubscriber<Object> testSubscriber = TestSubscriber.create();

		throwable.subscribe(testSubscriber);
		testSubscriber.awaitTerminalEvent();

		Cache.ValueWrapper valueWrapper = cache.get(SimpleKey.EMPTY);

		assertNull(valueWrapper);
		testSubscriber.assertError(RuntimeException.class);
	}

	@Test(timeout = 1000L)
	public void neverFinish() {
		Cache cache = context.getBean(CacheManager.class).getCache("neverFinish");
		bean.neverFinish();

		Cache.ValueWrapper valueWrapper = cache.get(SimpleKey.EMPTY);

		assertNull(valueWrapper);
	}

	@Configuration
	@EnableCaching
	public static class BasicTestConfig {

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager();
		}

		@Bean
		public TestService service() {
			return new TestServiceImpl();
		}
	}


	public interface TestService {
		Single<Object> single();

		Single<Object> nullValue();

		Single<Object> throwable();

		Single<Object> neverFinish();
	}


	public static class TestServiceImpl implements TestService {
		@Cacheable("single")
		@Override
		public Single<Object> single() {
			return createObservable(new TestBean(1));
		}

		@Cacheable("nullValue")
		@Override
		public Single<Object> nullValue() {
			return createObservable((Object)null);
		}

		@Cacheable("throwable")
		@Override
		public Single<Object> throwable() {
			return Single.error(new RuntimeException());
		}

		@Cacheable("neverFinish")
		@Override
		public Single<Object> neverFinish() {
			return Observable.never().toSingle();
		}

		private Single<Object> createObservable(Object value) {
			return Single.defer(() -> Single.just(value));
		}
	}


	static class TestBean {
		private Integer value;

		public TestBean(Integer value) {
			this.value = value;
		}
	}
}
