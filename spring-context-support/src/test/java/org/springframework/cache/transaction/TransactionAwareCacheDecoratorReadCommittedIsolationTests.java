package org.springframework.cache.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED;
import static org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK;

public class TransactionAwareCacheDecoratorReadCommittedIsolationTests {

	private Cache cache;
	private ConcurrentHashMap<Object, Object> map;

	@BeforeEach
	void setUp() {
		map = new ConcurrentHashMap<>();
		cache = new TransactionAwareCacheDecorator(new ConcurrentMapCache("cacheTest", map, true), true);
	}


	@Test
	void getName() {
		assertEquals("cacheTest", cache.getName());
	}

	@Test
	void getNativeCache() {
		assertSame(map, cache.getNativeCache());
	}

	@Test
	void putAndGetObject() {
		assertNull(cache.get("foo"));
		cache.put("foo", "bar");
		assertEquals("bar", cache.get("foo", String.class));
		assertEqualWrapperContents("bar", cache.get("foo"));
		cache.put("foo", "baz");
		assertEquals("baz", cache.get("foo", String.class));
		assertEqualWrapperContents("baz", cache.get("foo"));
	}

	@Test
	void putAndGetObjectWithSuccessfulTransaction() {
		// First put a value there outside of the transaction
		cache.put("foo", "foo");
		withSimulatedTransaction(() -> {
			assertEquals("foo", cache.get("foo", String.class));
			cache.put("foo", "bar");

			// Should not be committed
			assertEquals("foo", map.get("foo"));
			assertEquals("foo", withOtherTransaction(() -> cache.get("foo", String.class)));

			assertEquals("bar", cache.get("foo", String.class));
			assertEqualWrapperContents("bar", cache.get("foo"));
			cache.put("foo", "baz");

			// Should not be committed
			assertEquals("foo", map.get("foo"));
			assertEquals("foo", withOtherTransaction(() -> cache.get("foo", String.class)));

			assertEquals("baz", cache.get("foo", String.class));
			assertEqualWrapperContents("baz", cache.get("foo"));
			return STATUS_COMMITTED;
		});

		// Now the value should be committed
		assertEquals("baz", map.get("foo"));
		assertEqualWrapperContents("baz", cache.get("foo"));
		assertEquals("baz", withOtherTransaction(() -> cache.get("foo", String.class)));
	}

	@Test
	void putAndGetObjectWithUnsuccessfulTransaction() {
		cache.put("foo", "foo");
		withSimulatedTransaction(() -> {
			assertEquals("foo", cache.get("foo", String.class));
			cache.put("foo", "bar");

			// Should not be committed
			assertEquals("foo", map.get("foo"));
			assertEquals("foo", withOtherTransaction(() -> cache.get("foo", String.class)));

			assertEquals("bar", cache.get("foo", String.class));
			assertEqualWrapperContents("bar", cache.get("foo"));
			cache.put("foo", "baz");

			// Should not be committed
			assertEquals("foo", map.get("foo"));
			assertEquals("foo", withOtherTransaction(() -> cache.get("foo", String.class)));

			assertEquals("baz", cache.get("foo", String.class));
			assertEqualWrapperContents("baz", cache.get("foo"));
			return STATUS_ROLLED_BACK;
		});

		// Now the value should not be committed
		assertEquals("foo", map.get("foo"));
		assertEqualWrapperContents("foo", cache.get("foo"));
		assertEquals("foo", withOtherTransaction(() -> cache.get("foo", String.class)));
	}

	@Test
	void computeObjectWithSuccessfulTransaction() {
		// First put a value there outside of the transaction
		cache.put("foo", "foo");
		withSimulatedTransaction(() -> {
			assertEquals("foo", cache.get("foo", String.class));
			cache.evict("foo");
			cache.get("foo", () -> "bar");

			// Should not be committed
			assertEquals("foo", map.get("foo"));
			assertEquals("foo", withOtherTransaction(() -> cache.get("foo", String.class)));

			assertEquals("bar", cache.get("foo", String.class));
			assertEqualWrapperContents("bar", cache.get("foo"));
			cache.evict("foo");
			cache.get("foo", () -> "baz");

			// Should not be committed
			assertEquals("foo", map.get("foo"));
			assertEquals("foo", withOtherTransaction(() -> cache.get("foo", String.class)));

			assertEquals("baz", cache.get("foo", String.class));
			assertEqualWrapperContents("baz", cache.get("foo"));
			return STATUS_COMMITTED;
		});

		// Now the value should be committed
		assertEquals("baz", map.get("foo"));
		assertEqualWrapperContents("baz", cache.get("foo"));
		assertEquals("baz", withOtherTransaction(() -> cache.get("foo", String.class)));
	}

	@Test
	void computeObjectWithUnsuccessfulTransaction() {
		cache.put("foo", "foo");
		withSimulatedTransaction(() -> {
			assertEquals("foo", cache.get("foo", String.class));
			cache.evict("foo");
			cache.get("foo", () -> "bar");

			// Should not be committed
			assertEquals("foo", map.get("foo"));
			assertEquals("foo", withOtherTransaction(() -> cache.get("foo", String.class)));

			assertEquals("bar", cache.get("foo", String.class));
			assertEqualWrapperContents("bar", cache.get("foo"));
			cache.evict("foo");
			cache.get("foo", () -> "baz");

			// Should not be committed
			assertEquals("foo", map.get("foo"));
			assertEquals("foo", withOtherTransaction(() -> cache.get("foo", String.class)));

			assertEquals("baz", cache.get("foo", String.class));
			assertEqualWrapperContents("baz", cache.get("foo"));
			return STATUS_ROLLED_BACK;
		});

		// Now the value should not be committed
		assertEquals("foo", map.get("foo"));
		assertEqualWrapperContents("foo", cache.get("foo"));
		assertEquals("foo", withOtherTransaction(() -> cache.get("foo", String.class)));
	}

	@Test
	void evictUnsuccessfulTransaction() {
		final var keys = Set.of("foo1", "foo2", "foo3", "foo4");
		for (var key: keys) {
			cache.put(key, key);
		}
		withSimulatedTransaction(() -> {
			for (var key: keys) {
				assertEquals(key, cache.get(key, String.class));
			}
			cache.evict("foo2");
			cache.evict("foo4");
			assertNull(cache.get("foo2"));
			assertNull(cache.get("foo4"));
			return STATUS_ROLLED_BACK;
		});

		assertEquals(4, map.size());
		for (var key: keys) {
			assertTrue(map.contains(key));
			assertEquals(key, cache.get(key, String.class));
		}
	}

	@Test
	void evictSuccessfulTransaction() {
		final var keys = Set.of("foo1", "foo2", "foo3", "foo4");
		for (var key: keys) {
			cache.put(key, key);
		}
		withSimulatedTransaction(() -> {
			for (var key: keys) {
				assertEquals(key, cache.get(key, String.class));
			}
			cache.evict("foo2");
			cache.evict("foo4");
			assertNull(cache.get("foo2"));
			assertNull(cache.get("foo4"));
			return STATUS_COMMITTED;
		});

		assertEquals(2, map.size());
		for (var key: Set.of("foo1", "foo3")) {
			assertTrue(map.contains(key));
			assertEquals(key, cache.get(key, String.class));
		}
	}

	@Test
	void clearUnsuccessfulTransaction() {
		final var keys = Set.of("foo1", "foo2", "foo3", "foo4");
		for (var key: keys) {
			cache.put(key, key);
		}
		withSimulatedTransaction(() -> {
			for (var key: keys) {
				assertEquals(key, cache.get(key, String.class));
			}
			cache.clear();
			cache.put("foo1", "bar1");
			assertNull(cache.get("foo2"));
			assertEquals("bar1", cache.get("foo1", String.class));
			return STATUS_ROLLED_BACK;
		});

		assertEquals(4, map.size());
		for (var key: keys) {
			assertTrue(map.contains(key));
			assertEquals(key, cache.get(key, String.class));
		}
	}

	@Test
	void clearSuccessfulTransaction() {
		final var keys = Set.of("foo1", "foo2", "foo3", "foo4");
		for (var key: keys) {
			cache.put(key, key);
		}
		withSimulatedTransaction(() -> {
			for (var key: keys) {
				assertEquals(key, cache.get(key, String.class));
			}
			cache.clear();
			cache.put("foo1", "bar1");
			assertNull(cache.get("foo2"));
			assertEquals("bar1", cache.get("foo1", String.class));
			return STATUS_COMMITTED;
		});

		assertEquals(1, map.size());
		assertEquals("bar1", cache.get("foo1", String.class));
	}

	@Test
	void invalidateSuccessfulTransaction() {
		final var keys = Set.of("foo1", "foo2", "foo3", "foo4");
		for (var key: keys) {
			cache.put(key, key);
		}
		withSimulatedTransaction(() -> {
			for (var key: keys) {
				assertEquals(key, cache.get(key, String.class));
			}
			assertTrue(cache.invalidate());

			// Should have committed
			assertEquals(Boolean.FALSE, withOtherTransaction(() -> cache.invalidate()));
			assertNull(withOtherTransaction(() -> cache.get("foo1")));
			assertNull(cache.get("foo1"));
			assertFalse(cache.invalidate());

			cache.put("foo1", "bar1");
			assertEquals("bar1", cache.get("foo1", String.class));
			return STATUS_COMMITTED;
		});

		assertEquals(1, map.size());
		assertEquals("bar1", cache.get("foo1", String.class));
	}

	@Test
	void invalidateUnsuccessfulTransaction() {
		final var keys = Set.of("foo1", "foo2", "foo3", "foo4");
		for (var key: keys) {
			cache.put(key, key);
		}
		withSimulatedTransaction(() -> {
			for (var key: keys) {
				assertEquals(key, cache.get(key, String.class));
			}
			assertTrue(cache.invalidate());

			// Should have committed
			assertFalse(withOtherTransaction(() -> cache.invalidate()));
			assertNull(withOtherTransaction(() -> cache.get("foo1")));
			assertNull(cache.get("foo1"));
			assertFalse(cache.invalidate());

			cache.put("foo1", "bar1");
			assertEquals("bar1", cache.get("foo1", String.class));
			return STATUS_ROLLED_BACK;
		});

		assertEquals(0, map.size());
		assertNull(cache.get("foo1"));
	}

	@Test
	void evictIfPresentSuccessfulTransaction() {
		final var keys = Set.of("foo1", "foo2", "foo3", "foo4");
		for (var key: keys) {
			cache.put(key, key);
		}
		withSimulatedTransaction(() -> {
			for (var key: keys) {
				assertEquals(key, cache.get(key, String.class));
			}
			assertTrue(cache.evictIfPresent("foo2"));
			assertTrue(cache.evictIfPresent("foo4"));
			assertNull(cache.get("foo2"));
			assertNull(cache.get("foo4"));
			// Should be immediately committed
			assertNull(map.get("foo2"));
			assertNull(map.get("foo4"));
			return STATUS_COMMITTED;
		});

		assertEquals(2, map.size());
		for (var key: Set.of("foo1", "foo3")) {
			assertTrue(map.contains(key));
			assertEquals(key, cache.get(key, String.class));
		}
	}

	@Test
	void evictIfPresentUnsuccessfulTransaction() {
		final var keys = Set.of("foo1", "foo2", "foo3", "foo4");
		for (var key: keys) {
			cache.put(key, key);
		}
		withSimulatedTransaction(() -> {
			for (var key: keys) {
				assertEquals(key, cache.get(key, String.class));
			}
			assertTrue(cache.evictIfPresent("foo2"));
			assertTrue(cache.evictIfPresent("foo4"));
			assertNull(cache.get("foo2"));
			assertNull(cache.get("foo4"));
			// Should be immediately committed
			assertNull(map.get("foo2"));
			assertNull(map.get("foo4"));
			return STATUS_ROLLED_BACK;
		});

		// Evict should commit even if rolled back
		assertEquals(2, map.size());
		for (var key: Set.of("foo1", "foo3")) {
			assertTrue(map.contains(key));
			assertEquals(key, cache.get(key, String.class));
		}
	}

	@Test
	void putIfAbsentWithSuccessfulTransaction() {
		// First put a value there outside of the transaction
		cache.put("foo", "foo");
		withSimulatedTransaction(() -> {
			assertEquals("foo", cache.get("foo", String.class));

			cache.evict("foo");

			// From the perspective of put if absent the cache should still contain "foo"
			assertEqualWrapperContents("foo", cache.putIfAbsent("foo", "bar"));

			// Put in transaction state
			cache.put("foo", "baz");

			assertEquals("baz", cache.get("foo", String.class));

			// Now remove from the committed cache, should also remove from the transaction cache
			cache.evictIfPresent("foo");

			assertNull(withOtherTransaction(() -> cache.get("foo")));

			assertNull(cache.get("foo"));

			// Now this should put the new value
			assertNull(cache.putIfAbsent("foo", "bar"));

			// Should also be visible in the transaction
			assertEquals("bar", cache.get("foo", String.class));

			// Should also be visible in other transaction
			assertEquals("bar", withOtherTransaction(() -> cache.get("foo", String.class)));

			return STATUS_COMMITTED;
		});

		// Now the value should be committed
		assertEquals("bar", map.get("foo"));
		assertEqualWrapperContents("bar", cache.get("foo"));
		assertEquals("bar", withOtherTransaction(() -> cache.get("foo", String.class)));
	}

	@Test
	void putIfAbsentWithUnsuccessfulTransaction() {
		// First put a value there outside of the transaction
		cache.put("foo", "foo");
		withSimulatedTransaction(() -> {
			assertEquals("foo", cache.get("foo", String.class));

			cache.evict("foo");

			// From the perspective of put if absent the cache should still contain "foo"
			assertEqualWrapperContents("foo", cache.putIfAbsent("foo", "bar"));

			// Put in transaction state
			cache.put("foo", "baz");

			assertEquals("baz", cache.get("foo", String.class));

			// Now remove from the committed cache, should also remove from the transaction cache
			cache.evictIfPresent("foo");

			assertNull(withOtherTransaction(() -> cache.get("foo")));

			assertNull(cache.get("foo"));

			// Now this should put the new value
			assertNull(cache.putIfAbsent("foo", "bar"));

			// Should also be visible in the transaction
			assertEquals("bar", cache.get("foo", String.class));

			// Should also be visible in other transaction
			assertEquals("bar", withOtherTransaction(() -> cache.get("foo", String.class)));

			// Put another value (this should not commit)
			cache.put("foo", "bla");

			return STATUS_ROLLED_BACK;
		});

		// Now the value should be committed even though the transaction rolled back
		assertEquals("bar", map.get("foo"));
		assertEqualWrapperContents("bar", cache.get("foo"));
		assertEquals("bar", withOtherTransaction(() -> cache.get("foo", String.class)));
	}

	@Test
	void putNull() {
		assertNull(cache.get("foo", String.class));
		assertNull(cache.get("foo"));
		cache.put("foo", null);
		assertNull(cache.get("foo", String.class));
		assertEqualWrapperContents(null, cache.get("foo"));
	}

	@Test
	void putAndGetWithValueLoader() {
		assertNull(cache.get("foo"));
		cache.get("foo", () -> "bar");
		assertEquals("bar", cache.get("foo", String.class));
		assertEqualWrapperContents("bar", cache.get("foo"));
		cache.get("foo", () -> "baz");
		assertEquals("bar", cache.get("foo", String.class));
		assertEqualWrapperContents("bar", cache.get("foo"));
	}


	@Test
	void putAndGetWithValueLoaderConcurrently() throws Exception {
		assertNull(cache.get("foo"));
		final AtomicReference<String> generatedValue = new AtomicReference<>();
		final AtomicInteger executeCount = new AtomicInteger(0);
		runConcurrently((i) -> {
			cache.get("foo", () -> {
				final var value = UUID.randomUUID().toString();
				generatedValue.set(value);
				executeCount.incrementAndGet();
				return value;
			});
		});
		assertEquals(1, executeCount.get());
		final var value = generatedValue.get();
		assertNotNull(value);
		assertEquals(value, cache.get("foo", String.class));
	}

	@Test
	void commitConcurrentTransactionsConsistency() {
		runConcurrently((threadIndex) -> {
			// Make random modifications. The cache value count should be consistent at the end.
			withSimulatedTransaction(() -> {
				int operationCount = 500;
				for (int i = 0; i < operationCount; ++i) {
					if (i % 100 == 0) {
						// Even operations are clear
						cache.clear();
					} else {
						// Odd operations add a key
						cache.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
					}
				}
				return STATUS_COMMITTED;
			});
		});
		// There should be exactly 99 puts after the last clear, transaction commits need to be synchronized.
		assertEquals(99, map.size());
	}

	@Test
	void clear() {
		cache.put("foo1", "bar");
		cache.put("foo2", "baz");
		cache.clear();
		assertNull(cache.get("foo1"));
		assertNull(cache.get("foo2"));
	}

	@Test
	void invalidate() {
		assertFalse(cache.invalidate());
		cache.put("foo1", "bar");
		cache.put("foo2", "baz");
		assertTrue(cache.invalidate());
		assertNull(cache.get("foo1"));
		assertNull(cache.get("foo2"));
	}

	@Test
	void evict() {
		cache.evict("foo");
		assertNull(cache.get("foo"));
		cache.put("foo", "bar");
		assertEquals("bar", cache.get("foo", String.class));
		cache.evict("foo");
		assertNull(cache.get("foo"));
	}

	@Test
	void evictIfPresent() {
		assertFalse(cache.evictIfPresent("foo"));
		assertNull(cache.get("foo"));
		cache.put("foo", "bar");
		assertEquals("bar", cache.get("foo", String.class));
		assertTrue(cache.evictIfPresent("foo"));
		assertNull(cache.get("foo"));
	}

	@Test
	void putIfAbsent() {
		assertNull(cache.putIfAbsent("foo", "bar"));
		assertEqualWrapperContents("bar", cache.putIfAbsent("foo", "baz"));
		assertEqualWrapperContents("bar", cache.get("foo"));
	}

	@Test
	void putIfAbsentConcurrently() throws Exception {
		assertNull(cache.get("foo"));
		final AtomicReference<String> putValue = new AtomicReference<>();
		final AtomicInteger executeCount = new AtomicInteger(0);
		runConcurrently((i) -> {
			final var value = UUID.randomUUID().toString();
			final var result = cache.putIfAbsent("foo", value);
			if (result == null) {
				executeCount.incrementAndGet();
				putValue.set(value);
			}
		});
		assertEquals(1, executeCount.get());
		final var value = putValue.get();
		assertNotNull(value);
		assertEquals(value, cache.get("foo", String.class));
	}

	private void withSimulatedTransaction(Supplier<Integer> supplier) {
		try {
			TransactionSynchronizationManager.initSynchronization();
			final var transactionStatus = supplier.get();
			for (var sync: TransactionSynchronizationManager.getSynchronizations()) {
				sync.afterCompletion(transactionStatus);
			}
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Nullable
	private <T> T withOtherTransaction(Supplier<T> supplier) {
		final var result = new AtomicReference<T>();
		final var t = new Thread(() -> {
			result.set(supplier.get());
		});
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			throw new RuntimeException("Thread was interrupted", e);
		}
		return result.get();
	}

	private void assertEqualWrapperContents(@Nullable Object expectedContents, @Nullable Cache.ValueWrapper valueWrapper) {
		assertNotNull(valueWrapper);
		assertEquals(expectedContents, valueWrapper.get());
	}

	private void runConcurrently(Consumer<Integer> runnable) {
		final int threadCount = 10;
		final var executor = new ThreadPoolExecutor(threadCount, threadCount, 1, TimeUnit.HOURS, new LinkedBlockingQueue<>());
		final AtomicInteger callCount = new AtomicInteger(0);

		for (int i = 0; i < threadCount; ++i) {
			final int threadIndex = i;
			executor.execute(() -> {
				callCount.incrementAndGet();
				runnable.accept(threadIndex);
			});
		}
		executor.shutdown();
		try {
			assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
		} catch (InterruptedException e) {
			fail("Got interrupted", e);
		}
		assertEquals(threadCount, callCount.get());
	}
}
