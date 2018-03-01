/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueRetrievalException;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.lang.Nullable;
import org.springframework.tests.transaction.CallCountingTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Tests for the {@link TransactionalCacheDecorator}.
 *
 * @author William Hoyle
 */
public class TransactionalCacheDecoratorTests {

	private final PlatformTransactionManager txManager = new CallCountingTransactionManager();
	private final TransactionTemplate transactionTemplate =
			new TransactionTemplate(txManager, new DefaultTransactionDefinition(
					TransactionDefinition.PROPAGATION_REQUIRES_NEW)
			);

	private final Cache targetCache = new ConcurrentMapCache("cache.test");
	private final TransactionalCacheDecorator transactionalCacheDecorator =
			new TransactionalCacheDecorator(targetCache);

	@Before
	public void init() {
		targetCache.clear();
	}

	@Test
	public void get() {
		targetCache.put("test", "value");
		assertEquals("value", targetCache.get("test", String.class));
		transactionTemplate.execute(status -> {
			assertEquals("value", transactionalCacheDecorator.get("test", String.class));
			return null;
		});
	}

	@Test
	public void getNull() {
		targetCache.put("test", null);
		transactionTemplate.execute(status -> {
			assertWrappedValueNull(transactionalCacheDecorator.get("test"));
			return null;
		});
	}

	@Test
	public void getNoTransaction() {
		targetCache.put("test", "value");
		assertEquals("value", transactionalCacheDecorator.get("test", String.class));
		assertWrappedValueEquals("value", transactionalCacheDecorator.get("test"));
	}

	@Test
	public void getNativeCache() {
		assertNull(transactionalCacheDecorator.getNativeCache());
	}

	@Test
	public void getWithValueLoader() {
		assertNull(targetCache.get("test"));
		transactionTemplate.execute(status -> {
			assertNull(transactionalCacheDecorator.get("test", String.class));
			assertEquals("value", transactionalCacheDecorator.get("test", () -> "value"));
			// Value should have been loaded
			assertEquals("value", transactionalCacheDecorator.get("test", String.class));
			// Value should not have been written to the target cache
			assertNull(targetCache.get("test"));
			return null;
		});

		// Value should have been written to the target cache
		assertEquals("value", targetCache.get("test", String.class));
	}

	@Test
	public void getWithValueLoaderNoTransaction() {
		assertNull(targetCache.get("test"));
		assertNull(transactionalCacheDecorator.get("test", String.class));
		assertEquals("value", transactionalCacheDecorator.get("test", () -> "value"));
		// Value should have been written to the target cache
		assertEquals("value", targetCache.get("test", String.class));
	}

	@Test
	public void getNullWithValueLoader() {
		transactionTemplate.execute(status -> {
			assertNull(transactionalCacheDecorator.get("test", () -> null));
			// Value should have been loaded
			assertWrappedValueNull(transactionalCacheDecorator.get("test"));
			return null;
		});

		// Null value should have been written to the target cache
		assertWrappedValueNull(targetCache.get("test"));
	}

	@Test
	public void getExistingValueWithValueLoader() {
		targetCache.put("test", "present");
		transactionTemplate.execute(status -> {
			// Get with loader should return the existing value
			assertEquals("present", transactionalCacheDecorator.get("test", () -> "value"));
			// Value should not have been reloaded
			assertEquals("present", transactionalCacheDecorator.get("test", String.class));
			return null;
		});

		// Value should not have been updated in the target cache
		assertEquals("present", targetCache.get("test", String.class));
	}

	@Test
	public void getExistingNullValueWithValueLoader() {
		targetCache.put("test", null);
		transactionTemplate.execute(status -> {
			// Get with loader should return the existing null mapping
			assertNull(transactionalCacheDecorator.get("test", () -> "value"));
			// Null value should not have been overwritten
			assertWrappedValueNull(transactionalCacheDecorator.get("test"));
			return null;
		});

		// Null value should not have been overwritten in the target cache
		assertWrappedValueNull(targetCache.get("test"));
	}

	@Test
	public void getValueRetrievalException() {
		assertNull(targetCache.get("test"));
		transactionTemplate.execute(status -> {
			Callable<Object> loader = () -> {
				throw new IllegalStateException();
			};
			try {
				transactionalCacheDecorator.get("test", loader);
				fail("Expected ValueRetrievalException");
			}
			catch (ValueRetrievalException e) {
				assertEquals("test", e.getKey());
				assertThat(e.getCause(), is(instanceOf(IllegalStateException.class)));
			}
			// No value should have been loaded
			assertNull(transactionalCacheDecorator.get("test"));
			// Value should not have been written to the target cache
			assertNull(targetCache.get("test"));
			return null;
		});

		// No value should have been written to the target cache
		assertNull(targetCache.get("test"));
	}

	@Test
	public void put() {
		assertNull(targetCache.get("test"));
		transactionTemplate.execute(status -> {
			assertNull(transactionalCacheDecorator.get("test"));
			transactionalCacheDecorator.put("test", "value");
			// Value should not have been written to the target cache
			assertNull(targetCache.get("test"));
			return null;
		});

		// Value should have been written to the target cache
		assertEquals("value", targetCache.get("test", String.class));
	}

	@Test
	public void putNull() {
		assertNull(targetCache.get("test"));
		transactionTemplate.execute(status -> {
			transactionalCacheDecorator.put("test", null);
			// Value should not have been written to the target cache
			assertNull(targetCache.get("test"));
			return null;
		});

		// Null value should have been written to the target cache
		assertWrappedValueNull(targetCache.get("test"));
	}

	@Test
	public void putNoTransaction() {
		assertNull(targetCache.get("test"));
		transactionalCacheDecorator.put("test", "value");
		assertEquals("value", targetCache.get("test", String.class));
	}

	@Test
	public void rollbackPut() {
		assertNull(targetCache.get("test"));
		try {
			transactionTemplate.execute(status -> {
				transactionalCacheDecorator.put("test", "value");
				throw new CacheTestException("rollback");
			});
			fail("Expected CacheTestException");
		}
		catch (CacheTestException e) {
			// expected
		}

		// Value should not have been written to the target cache
		assertNull(targetCache.get("test"));
	}

	@Test
	public void rollbackPutNull() {
		assertNull(targetCache.get("test"));
		try {
			transactionTemplate.execute(status -> {
				transactionalCacheDecorator.put("test", null);
				throw new CacheTestException("rollback");
			});
			fail("Expected CacheTestException");
		}
		catch (CacheTestException e) {
			// expected
		}

		// Value should not have been written to the target cache
		assertNull(targetCache.get("test"));
	}

	@Test
	public void putIfAbsent() {
		assertNull(targetCache.get("test"));
		transactionTemplate.execute(status -> {
			assertNull(transactionalCacheDecorator.putIfAbsent("test", "value"));
			// Value should not have been written to the target cache
			assertNull(targetCache.get("test"));
			return null;
		});

		// Value should have been written to the target cache
		assertEquals("value", targetCache.get("test", String.class));
	}

	@Test
	public void putIfAbsentNoTransaction() {
		assertNull(targetCache.get("test"));
		assertNull(transactionalCacheDecorator.putIfAbsent("test", "value"));
		// Value should have been written to the target cache
		assertEquals("value", targetCache.get("test", String.class));
	}

	@Test
	public void rollbackPutIfAbsent() {
		assertNull(targetCache.get("test"));
		try {
			transactionTemplate.execute(status -> {
				assertNull(transactionalCacheDecorator.putIfAbsent("test", "value"));
				// Value should have been written to the transactional cache
				assertEquals("value", transactionalCacheDecorator.get("test", String.class));
				// Value should not have been written to the target cache
				assertNull(targetCache.get("test"));
				throw new CacheTestException("rollback");
			});
			fail("Expected CacheTestException");
		}
		catch (CacheTestException e) {
			// expected
		}

		// Value should not have been written to the target cache
		assertNull(targetCache.get("test", String.class));
	}

	@Test
	public void putIfAbsentWithExistingValue() {
		targetCache.put("test", "present");
		transactionTemplate.execute(status -> {
			// Put if absent should return the existing value if it is already present in the target cache
			assertWrappedValueEquals("present", transactionalCacheDecorator.putIfAbsent("test", "value"));
			// Value should not have been updated
			assertEquals("present", transactionalCacheDecorator.get("test", String.class));
			return null;
		});

		// Value in the target cache should no have been overwritten
		assertEquals("present", targetCache.get("test", String.class));
	}

	@Test
	public void putNullIfAbsent() {
		assertNull(targetCache.get("test"));
		transactionTemplate.execute(status -> {
			assertNull(transactionalCacheDecorator.putIfAbsent("test", null));
			// Value should not have been written to the target cache
			assertNull(targetCache.get("test"));
			return null;
		});

		// Null value should have been written to the target cache
		assertWrappedValueNull(targetCache.get("test"));
	}

	@Test
	public void putIfAbsentWithExistingNullValue() {
		targetCache.put("test", null);
		transactionTemplate.execute(status -> {
			// Put if absent should not update if the value is already present in the target cache
			assertWrappedValueNull(transactionalCacheDecorator.putIfAbsent("test", "value"));
			// Null value should have been written to the target cache
			assertWrappedValueNull(targetCache.get("test"));
			return null;
		});

		// Value in the target cache should not have been overwritten
		assertWrappedValueNull(targetCache.get("test"));
	}

	@Test
	public void evict() {
		targetCache.put("test", "value");
		transactionTemplate.execute(status -> {
			assertEquals("value", transactionalCacheDecorator.get("test", String.class));
			transactionalCacheDecorator.evict("test");
			// Value should have been evicted from the transactional cache
			assertNull(transactionalCacheDecorator.get("test"));
			return null;
		});

		// Value should have been evicted from the target cache
		assertNull(targetCache.get("test", String.class));
	}

	@Test
	public void evictNoTransaction() {
		targetCache.put("test", "value");
		transactionalCacheDecorator.evict("test");
		// Value should have been evicted from the target cache
		assertNull(targetCache.get("test", String.class));
	}

	@Test
	public void rollbackEvict() {
		targetCache.put("test", "value");
		try {
			transactionTemplate.execute(status -> {
				transactionalCacheDecorator.evict("test");
				// Value should have been evicted from the transactional cache
				assertNull(transactionalCacheDecorator.get("test"));
				throw new CacheTestException("rollback");
			});
			fail("Expected CacheTestException");
		}
		catch (CacheTestException e) {
			// expected
		}

		// Value should not have been evicted from the target cache
		assertEquals("value", targetCache.get("test", String.class));
	}

	@Test
	public void clear() {
		targetCache.put("test", "value");
		targetCache.put("test2", "value2");
		transactionTemplate.execute(status -> {
			assertEquals("value", transactionalCacheDecorator.get("test", String.class));
			transactionalCacheDecorator.clear();
			// Values should have been cleared from the transactional cache
			assertNull(transactionalCacheDecorator.get("test"));
			assertNull(transactionalCacheDecorator.get("test2"));
			// But not the target cache
			assertEquals("value", targetCache.get("test", String.class));
			assertEquals("value2", targetCache.get("test2", String.class));
			return null;
		});

		// Values should have been cleared from the target cache
		assertNull(targetCache.get("test"));
		assertNull(targetCache.get("test2"));
	}

	@Test
	public void clearNoTransaction() {
		targetCache.put("test", "value");
		targetCache.put("test2", "value2");
		transactionalCacheDecorator.clear();
		// Values should have been cleared from the target cache
		assertNull(targetCache.get("test"));
		assertNull(targetCache.get("test2"));
	}

	@Test
	public void rollbackClear() {
		targetCache.put("test", "value");
		targetCache.put("test2", "value2");
		try {
			transactionTemplate.execute(status -> {
				transactionalCacheDecorator.clear();
				// Values should have been evicted from the transactional cache
				assertNull(transactionalCacheDecorator.get("test"));
				assertNull(transactionalCacheDecorator.get("test2"));
				throw new CacheTestException("rollback");
			});
			fail("Expected CacheTestException");
		}
		catch (CacheTestException e) {
			// expected
		}

		// Values should not have been cleared from the target cache
		assertEquals("value", targetCache.get("test", String.class));
		assertEquals("value2", targetCache.get("test2", String.class));
	}

	@Test
	public void evictThenPut() {
		targetCache.put("test", "value");
		transactionTemplate.execute(status -> {
			assertEquals("value", transactionalCacheDecorator.get("test", String.class));
			transactionalCacheDecorator.evict("test");
			transactionalCacheDecorator.put("test", "updated");
			// Value should have been overwritten in the transactional cache
			assertEquals("updated", transactionalCacheDecorator.get("test", String.class));
			// But not the target cache
			assertEquals("value", targetCache.get("test", String.class));
			return null;
		});

		// Value should have been overwritten in the target cache
		assertEquals("updated", targetCache.get("test", String.class));
	}

	@Test
	public void clearThenPut() {
		targetCache.put("test", "value");
		targetCache.put("test2", "value2");
		transactionTemplate.execute(status -> {
			assertEquals("value", transactionalCacheDecorator.get("test", String.class));
			transactionalCacheDecorator.clear();
			transactionalCacheDecorator.put("test", "updated");
			// Value should have been overwritten in the transactional cache
			assertEquals("updated", transactionalCacheDecorator.get("test", String.class));
			// But not the target cache
			assertEquals("value", targetCache.get("test", String.class));
			return null;
		});

		// Value should have been overwritten in the target cache
		assertEquals("updated", targetCache.get("test", String.class));
		// Second value should have been cleared
		assertNull(targetCache.get("test2"));
	}

	@Test
	public void threadLocalVisibility() throws InterruptedException, BrokenBarrierException, ExecutionException {
		assertNull(targetCache.get("test", String.class));

		ExecutorService executor = Executors.newCachedThreadPool();
		CyclicBarrier barrier = new CyclicBarrier(2);

		// Write thread
		Future<Boolean> pass = executor.submit(() -> transactionTemplate.execute(status -> {
			try {
				// Populate the cache
				transactionalCacheDecorator.put("test", "value");
				// Put has populated the cache decorator
				assertEquals("value", transactionalCacheDecorator.get("test", String.class));
				// But not the target cache
				assertNull(targetCache.get("test"));
				// Sync 1. with the main thread signals that the above write is complete
				barrier.await();
				// Sync 2. with the main thread before exit
				barrier.await();
				return true;
			}
			catch (InterruptedException | BrokenBarrierException e) {
				throw new AssertionError("Exception waiting for read thread", e);
			}
		}));

		transactionTemplate.execute(status -> {
			try {
				// Sync 1. with the writer thread after it completes its write
				barrier.await();
				// The put from the write thread is not visible in this thread, even
				// though it is visible in the write thread at this point in time
				assertNull(transactionalCacheDecorator.get("test"));
				return null;
			}
			catch (InterruptedException | BrokenBarrierException e) {
				throw new AssertionError("Exception waiting for write thread", e);
			}
		});

		// The writer thread will wait on the barrier before the end of its transaction

		// The value should not have been written to the target cache
		assertNull(targetCache.get("test", String.class));

		// Sync 2. with the writer thread allows it to continue to complete its transaction
		barrier.await();

		// Join the writer thread
		assertTrue(pass.get());
		executor.shutdownNow();

		// Value should have been committed to the target cache
		assertEquals("value", targetCache.get("test", String.class));
	}

	@Test
	public void suspendAndResume() {
		assertNull(targetCache.get("test"));
		transactionTemplate.execute(s1 -> {
			assertNull(transactionalCacheDecorator.get("test"));
			transactionalCacheDecorator.put("test", "value");
			// Value should not have been written to the target cache
			assertNull(targetCache.get("test"));

			// The template is set to "PROPAGATION_REQUIRES_NEW", so running a new
			// transaction should suspend the existing one.
			transactionTemplate.execute(s2 -> {
				// The value written in the outer transaction should not be visible here
				assertNull(transactionalCacheDecorator.get("test"));
				transactionalCacheDecorator.put("test", "inner");
				// Value should not have been written to the target cache
				assertNull(targetCache.get("test"));

				// The synchronisation strategy can handle multiple suspended transactions
				transactionTemplate.execute(s3 -> {
					// The value written in the outer transaction should not be visible here
					assertNull(transactionalCacheDecorator.get("test"));
					transactionalCacheDecorator.put("test", "inner2");
					return null;
				});

				// Value in the target cache should have been updated by the nested
				// transaction when it committed
				assertEquals("inner2", targetCache.get("test", String.class));
				// But the value visible in this transaction is the one we wrote in this transaction
				assertEquals("inner", transactionalCacheDecorator.get("test", String.class));

				return null;
			});

			// Value in the target cache should have been updated by the nested
			// transaction when it committed
			assertEquals("inner", targetCache.get("test", String.class));
			// But the value visible in this transaction is the one we wrote in this transaction
			assertEquals("value", transactionalCacheDecorator.get("test", String.class));
			return null;
		});

		// Value from the outer transaction should have been written to the target cache
		assertEquals("value", targetCache.get("test", String.class));
	}

	@Test
	public void rollbackSuspendAndResume() {
		assertNull(targetCache.get("test"));
		try {
			transactionTemplate.execute(status -> {
				assertNull(transactionalCacheDecorator.get("test"));
				transactionalCacheDecorator.put("test", "value");

				// Begin a new transaction
				transactionTemplate.execute(status2 -> {
					// The value written in the outer transaction should not be visible here
					assertNull(transactionalCacheDecorator.get("test"));
					transactionalCacheDecorator.put("test", "inner");

					// The synchronisation strategy can handle multiple suspended transactions
					transactionTemplate.execute(status3 -> {
						// The value written in the outer transaction should not be visible here
						assertNull(transactionalCacheDecorator.get("test"));
						transactionalCacheDecorator.put("test", "inner2");

						throw new CacheTestException("rollback");
					});

					fail("Expected CacheTestException");
					return null;
				});

				fail("Expected CacheTestException");
				return null;
			});

			fail("Expected CacheTestException");
		}
		catch (CacheTestException e) {
			// expected
		}

		// Value should not have been written to the target cache
		assertNull(targetCache.get("test"));
	}

	/**
	 * Assert that a {@code ValueWrapper} is non-null and that the wrapped value is null.
	 */
	private static void assertWrappedValueNull(@Nullable ValueWrapper wrapper) {
		assertWrappedValueEquals(null, wrapper);
	}

	/**
	 * Assert that a {@code ValueWrapper} is non-null and the wrapped value equals the expected value.
	 */
	private static void assertWrappedValueEquals(@Nullable Object expected, @Nullable ValueWrapper wrapper) {
		assertNotNull(wrapper);
		assertEquals(expected, wrapper.get());
	}

	private static class CacheTestException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public CacheTestException(String message) {
			super(message);
		}
	}
}
