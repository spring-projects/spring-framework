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

package org.springframework.cache.transaction;

import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
public class TransactionAwareCacheDecoratorTests {

	private final TransactionTemplate txTemplate = new TransactionTemplate(new CallCountingTransactionManager());


	@Test
	public void createWithNullTarget() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TransactionAwareCacheDecorator(null));
	}

	@Test
	public void getTargetCache() {
		Cache target = new ConcurrentMapCache("testCache");
		TransactionAwareCacheDecorator cache = new TransactionAwareCacheDecorator(target);
		assertThat(cache.getTargetCache()).isSameAs(target);
	}

	@Test
	public void regularOperationsOnTarget() {
		Cache target = new ConcurrentMapCache("testCache");
		Cache cache = new TransactionAwareCacheDecorator(target);
		assertThat(cache.getName()).isEqualTo(target.getName());
		assertThat(cache.getNativeCache()).isEqualTo(target.getNativeCache());

		Object key = new Object();
		target.put(key, "123");
		assertThat(cache.get(key).get()).isEqualTo("123");
		assertThat(cache.get(key, String.class)).isEqualTo("123");

		cache.clear();
		assertThat(target.get(key)).isNull();
	}

	@Test
	public void putNonTransactional() {
		Cache target = new ConcurrentMapCache("testCache");
		Cache cache = new TransactionAwareCacheDecorator(target);

		Object key = new Object();
		cache.put(key, "123");
		assertThat(target.get(key, String.class)).isEqualTo("123");
	}

	@Test
	public void putTransactional() {
		Cache target = new ConcurrentMapCache("testCache");
		Cache cache = new TransactionAwareCacheDecorator(target);
		Object key = new Object();

		txTemplate.executeWithoutResult(s -> {
			cache.put(key, "123");
			assertThat(target.get(key)).isNull();
		});

		assertThat(target.get(key, String.class)).isEqualTo("123");
	}

	@Test
	public void putIfAbsentNonTransactional() {
		Cache target = new ConcurrentMapCache("testCache");
		Cache cache = new TransactionAwareCacheDecorator(target);

		Object key = new Object();
		assertThat(cache.putIfAbsent(key, "123")).isNull();
		assertThat(target.get(key, String.class)).isEqualTo("123");
		assertThat(cache.putIfAbsent(key, "456").get()).isEqualTo("123");
		// unchanged
		assertThat(target.get(key, String.class)).isEqualTo("123");
	}

	@Test
	public void putIfAbsentTransactional() {  // no transactional support for putIfAbsent
		Cache target = new ConcurrentMapCache("testCache");
		Cache cache = new TransactionAwareCacheDecorator(target);
		Object key = new Object();

		txTemplate.executeWithoutResult(s -> {
			assertThat(cache.putIfAbsent(key, "123")).isNull();
			assertThat(target.get(key, String.class)).isEqualTo("123");
			assertThat(cache.putIfAbsent(key, "456").get()).isEqualTo("123");
			// unchanged
			assertThat(target.get(key, String.class)).isEqualTo("123");
		});

		assertThat(target.get(key, String.class)).isEqualTo("123");
	}

	@Test
	public void evictNonTransactional() {
		Cache target = new ConcurrentMapCache("testCache");
		Cache cache = new TransactionAwareCacheDecorator(target);
		Object key = new Object();
		cache.put(key, "123");

		cache.evict(key);
		assertThat(target.get(key)).isNull();
	}

	@Test
	public void evictTransactional() {
		Cache target = new ConcurrentMapCache("testCache");
		Cache cache = new TransactionAwareCacheDecorator(target);
		Object key = new Object();
		cache.put(key, "123");

		txTemplate.executeWithoutResult(s -> {
			cache.evict(key);
			assertThat(target.get(key, String.class)).isEqualTo("123");
		});

		assertThat(target.get(key)).isNull();
	}

	@Test
	public void evictIfPresentNonTransactional() {
		Cache target = new ConcurrentMapCache("testCache");
		Cache cache = new TransactionAwareCacheDecorator(target);
		Object key = new Object();
		cache.put(key, "123");

		cache.evictIfPresent(key);
		assertThat(target.get(key)).isNull();
	}

	@Test
	public void evictIfPresentTransactional() {  // no transactional support for evictIfPresent
		Cache target = new ConcurrentMapCache("testCache");
		Cache cache = new TransactionAwareCacheDecorator(target);
		Object key = new Object();
		cache.put(key, "123");

		txTemplate.executeWithoutResult(s -> {
			cache.evictIfPresent(key);
			assertThat(target.get(key)).isNull();
		});

		assertThat(target.get(key)).isNull();
	}

	@Test
	public void clearNonTransactional() {
		Cache target = new ConcurrentMapCache("testCache");
		Cache cache = new TransactionAwareCacheDecorator(target);
		Object key = new Object();
		cache.put(key, "123");

		cache.clear();
		assertThat(target.get(key)).isNull();
	}

	@Test
	public void clearTransactional() {
		Cache target = new ConcurrentMapCache("testCache");
		Cache cache = new TransactionAwareCacheDecorator(target);
		Object key = new Object();
		cache.put(key, "123");

		txTemplate.executeWithoutResult(s -> {
			cache.clear();
			assertThat(target.get(key, String.class)).isEqualTo("123");
		});

		assertThat(target.get(key)).isNull();
	}

	@Test
	public void invalidateNonTransactional() {
		Cache target = new ConcurrentMapCache("testCache");
		Cache cache = new TransactionAwareCacheDecorator(target);
		Object key = new Object();
		cache.put(key, "123");

		cache.invalidate();
		assertThat(target.get(key)).isNull();
	}

	@Test
	public void invalidateTransactional() {  // no transactional support for invalidate
		Cache target = new ConcurrentMapCache("testCache");
		Cache cache = new TransactionAwareCacheDecorator(target);
		Object key = new Object();
		cache.put(key, "123");

		txTemplate.executeWithoutResult(s -> {
			cache.invalidate();
			assertThat(target.get(key)).isNull();
		});

		assertThat(target.get(key)).isNull();
	}

}
