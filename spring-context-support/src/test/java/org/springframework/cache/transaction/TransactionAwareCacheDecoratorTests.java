/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache.transaction;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.tests.transaction.CallCountingTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * @author Stephane Nicoll
 */
public class TransactionAwareCacheDecoratorTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final PlatformTransactionManager txManager = new CallCountingTransactionManager();

	@Test
	public void createWithNullTarget() {
		thrown.expect(IllegalArgumentException.class);
		new TransactionAwareCacheDecorator(null);
	}

	@Test
	public void regularOperationsOnTarget() {
		Cache target = new ConcurrentMapCache("testCache");
		Cache cache = new TransactionAwareCacheDecorator(target);
		assertEquals(target.getName(), cache.getName());
		assertEquals(target.getNativeCache(), cache.getNativeCache());

		Object key = new Object();
		target.put(key, "123");
		assertEquals("123", cache.get(key).get());
		assertEquals("123", cache.get(key, String.class));

		cache.clear();
		assertNull(target.get(key));
	}

	@Test
	public void putNonTransactional() {
		Cache target = new ConcurrentMapCache("testCache");
		Cache cache = new TransactionAwareCacheDecorator(target);

		Object key = new Object();
		cache.put(key, "123");
		assertEquals("123", target.get(key, String.class));
	}

	@Test
	public void putTransactional() {
		Cache target = new ConcurrentMapCache("testCache");
		Cache cache = new TransactionAwareCacheDecorator(target);

		TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute(
				TransactionDefinition.PROPAGATION_REQUIRED));

		Object key = new Object();
		cache.put(key, "123");
		assertNull(target.get(key));
		txManager.commit(status);

		assertEquals("123", target.get(key, String.class));
	}

	@Test
	public void evictNonTransactional() {
		Cache target = new ConcurrentMapCache("testCache");
		Cache cache = new TransactionAwareCacheDecorator(target);
		Object key = new Object();
		cache.put(key, "123");

		cache.evict(key);
		assertNull(target.get(key));
	}

	@Test
	public void evictTransactional() {
		Cache target = new ConcurrentMapCache("testCache");
		Cache cache = new TransactionAwareCacheDecorator(target);
		Object key = new Object();
		cache.put(key, "123");


		TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute(
				TransactionDefinition.PROPAGATION_REQUIRED));
		cache.evict(key);
		assertEquals("123", target.get(key, String.class));
		txManager.commit(status);

		assertNull(target.get(key));
	}

}
