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
import org.junit.Test;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.functions.Func0;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

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
		Observable<Object> single = bean.single();
		TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();

		single.subscribe(testSubscriber);
		testSubscriber.awaitTerminalEvent();

		Object cachedValues = cache.get(SimpleKey.EMPTY).get();
		List<Object> resultValues = testSubscriber.getOnNextEvents();

		assertNotSame(cachedValues, resultValues);
		assertEquals(cachedValues, resultValues);
	}

	@Test
	public void multiple() {
		Cache cache = context.getBean(CacheManager.class).getCache("multiple");
		Observable<Object> multiple = bean.multiple();
		TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();

		multiple.subscribe(testSubscriber);
		testSubscriber.awaitTerminalEvent();

		Object cachedValues = cache.get(SimpleKey.EMPTY).get();
		List<Object> resultValues = testSubscriber.getOnNextEvents();

		assertNotSame(cachedValues, resultValues);
		assertEquals(cachedValues, resultValues);
	}

	@Test
	public void empty() {
		Cache cache = context.getBean(CacheManager.class).getCache("empty");
		Observable<Object> empty = bean.empty();
		TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();

		empty.subscribe(testSubscriber);
		testSubscriber.awaitTerminalEvent();

		Object cachedValues = cache.get(SimpleKey.EMPTY).get();
		List<Object> resultValues = testSubscriber.getOnNextEvents();

		assertNotSame(cachedValues, resultValues);
		assertEquals(cachedValues, resultValues);
	}

	@Test
	public void nullValue() {
		Cache cache = context.getBean(CacheManager.class).getCache("nullValue");
		Observable<Object> nullValue = bean.nullValue();
		TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();

		nullValue.subscribe(testSubscriber);
		testSubscriber.awaitTerminalEvent();

		Object cachedValues = cache.get(SimpleKey.EMPTY).get();
		List<Object> resultValues = testSubscriber.getOnNextEvents();

		assertNotSame(cachedValues, resultValues);
		assertEquals(cachedValues, resultValues);
	}

	@Test
	public void nullObservable() {
		Cache cache = context.getBean(CacheManager.class).getCache("nullObservable");
		Observable<Object> nullObservable = bean.nullObservable();

		Object cachedValues = cache.get(SimpleKey.EMPTY).get();

		assertNull("Value is cached as null", cachedValues);
	}

	@Test
	public void throwable() {
		Cache cache = context.getBean(CacheManager.class).getCache("throwable");
		Observable<Object> throwable = bean.nullValue();
		TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();

		throwable.subscribe(testSubscriber);
		testSubscriber.awaitTerminalEvent();

		Cache.ValueWrapper valueWrapper = cache.get(SimpleKey.EMPTY);

		assertNull(valueWrapper);
	}

	@Test
	public void partialReturnAndError() {
		Cache cache = context.getBean(CacheManager.class).getCache("partialReturnAndError");
		Observable<Object> partialReturnAndError = bean.partialReturnAndError();
		TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();

		partialReturnAndError.subscribe(testSubscriber);
		testSubscriber.awaitTerminalEvent();

		Cache.ValueWrapper valueWrapper = cache.get(SimpleKey.EMPTY);

		assertNull(valueWrapper);

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
		Observable<Object> single();

		Observable<Object> multiple();

		Observable<Object> empty();

		Observable<Object> nullValue();

		Observable<Object> nullObservable();

		Observable<Object> throwable();

		Observable<Object> partialReturnAndError();

		Observable<Object> neverFinish();

	}


	public static class TestServiceImpl implements TestService {
		@Cacheable("single")
		@Override
		public Observable<Object> single() {
			return createObservable(new TestBean(1));
		}

		@Cacheable("multiple")
		@Override
		public Observable<Object> multiple() {
			return createObservable(new TestBean(1), new TestBean(2));
		}

		@Cacheable("empty")
		@Override
		public Observable<Object> empty() {
			return Observable.empty();
		}

		@Cacheable("nullValue")
		@Override
		public Observable<Object> nullValue() {
			return createObservable((Object)null);
		}

		@Cacheable("nullObservable")
		@Override
		public Observable<Object> nullObservable() {
			return null;
		}

		@Cacheable("throwable")
		@Override
		public Observable<Object> throwable() {
			return Observable.fromCallable(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					throw new RuntimeException();
				}
			});
		}

		@Cacheable("partialReturnAndError")
		@Override
		public Observable<Object> partialReturnAndError() {
			return createObservable(new TestBean(1), new TestBean(2))
					.concatWith(Observable.error(new RuntimeException()));
		}

		@Cacheable("neverFinish")
		@Override
		public Observable<Object> neverFinish() {
			return Observable.never();
		}

		private Observable<Object> createObservable(Object... values) {
			return Observable.defer(new Func0<Observable<Object>>() {
				@Override
				public Observable<Object> call() {
					return Observable.from(values);
				}
			});
		}
	}


	static class TestBean {
		private Integer value;

		public TestBean(Integer value) {
			this.value = value;
		}
	}
}
