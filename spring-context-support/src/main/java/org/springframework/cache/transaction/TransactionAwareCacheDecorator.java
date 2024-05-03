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

package org.springframework.cache.transaction;

import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Cache decorator which synchronizes its {@link #put}, {@link #evict} and
 * {@link #clear} operations with Spring-managed transactions (through Spring's
 * {@link TransactionSynchronizationManager}), performing the actual cache
 * put/evict/clear operation only in the after-commit phase of a successful
 * transaction. Within a transaction this decorator provides consistency for all operations
 * performed in order and read-committed isolation from other transactions.
 * If no transaction is active, {@link #put}, {@link #evict} and
 * {@link #clear} operations will be performed immediately, as usual.
 *
 * <p><b>Note:</b> Use of immediate operations such as {@link #putIfAbsent} and
 * {@link #evictIfPresent} cannot be deferred to the after-commit phase of a
 * running transaction. Use these with care in a transactional environment.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Stas Volsky
 * @since 3.2
 * @see TransactionAwareCacheManagerProxy
 */
public class TransactionAwareCacheDecorator implements Cache {

	// Special value which marks a value as being evicted within the transaction state
	private static final Object EVICTED = new Object();

	private final Cache targetCache;

	// Thread local for storing the changes made within a transaction
	private final ThreadLocal<TransactionState> transactionState = new ThreadLocal<>();

	// Whether commits should be synchronized or not
	private final boolean synchronizeCommits;

	/**
	 * Create a new TransactionAwareCache for the given target Cache.
	 * <p/>
	 * Commits are not synchronized for efficiency, meaning concurrent clear/evict/put events in concurrent transactions could yield different results on the size of the cache
	 * after all transactions are done.
	 *
	 * @param targetCache the target Cache to decorate
	 */
	public TransactionAwareCacheDecorator(Cache targetCache) {
		this(targetCache, false);
	}

	/**
	 * Create a new TransactionAwareCache for the given target Cache, specifying the consistency needed for transaction commits.
	 * <p/>
	 * If synchronizedCommits is set to true, all commits of concurrent transactions are performed in a synchronized fashion, thereby yielding predictable results
	 * for the size of the cache after all operations are finished.
	 *
	 * @param targetCache the target Cache to decorate
	 * @param synchronizeCommits whether full consistency should be maintained for concurrent transaction commits, i.e. all commits are synchronized to the cache.
	 */
	public TransactionAwareCacheDecorator(Cache targetCache, boolean synchronizeCommits) {
		Assert.notNull(targetCache, "Target Cache must not be null");
		this.targetCache = targetCache;
		this.synchronizeCommits = synchronizeCommits;
	}


	/**
	 * Return the target Cache that this Cache should delegate to.
	 */
	public Cache getTargetCache() {
		return this.targetCache;
	}

	@Override
	public String getName() {
		return this.targetCache.getName();
	}

	@Override
	public Object getNativeCache() {
		return this.targetCache.getNativeCache();
	}

	@Override
	@Nullable
	public ValueWrapper get(Object key) {
		final var transactionState = getTransactionState();
		final var current = transactionState == null ? null : transactionState.get(key);
		if (current != null) {
			return convert(current);
		} else {
			return this.targetCache.get(key);
		}
	}

	@Override
	@Nullable
	public <T> T get(Object key, @Nullable Class<T> type) {
		final var transactionState = getTransactionState();
		final var wrapper = transactionState == null ? null : transactionState.get(key);
		if (wrapper != null) {
			// Unwrap
			final var value = convertValue(wrapper);
			return cast(value, type);
		} else {
			return this.targetCache.get(key, type);
		}
	}

	@Override
	@Nullable
	public <T> T get(Object key, Callable<T> valueLoader) {
		final var transactionState = getTransactionState();
		if (transactionState != null) {
			final var wrapper = transactionState.get(key);
			final boolean isEvicted = wrapper != null && wrapper.get() == EVICTED;
			if (isEvicted || (wrapper == null && this.targetCache.get(key) == null)) {
				// Compute value within transaction state
				final T value;
				try {
					value = valueLoader.call();
				} catch (Exception e) {
					throw new ValueRetrievalException(key, valueLoader, e);
				}
				transactionState.put(key, value);
				return value;
			} else if (wrapper != null) {
				return convertValue(wrapper);
			}
		}
		return this.targetCache.get(key, valueLoader);
	}

	@Override
	@Nullable
	public CompletableFuture<?> retrieve(Object key) {
		return this.targetCache.retrieve(key);
	}

	@Override
	public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
		return this.targetCache.retrieve(key, valueLoader);
	}

	@Override
	public void put(final Object key, @Nullable final Object value) {
		final var transactionState = getTransactionState();
		if (transactionState != null) {
			transactionState.put(key, value);
		} else {
			this.targetCache.put(key, value);
		}
	}

	@Override
	@Nullable
	public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
		final var transactionState = getTransactionState();
		if (transactionState != null) {
			// Remove any changes made to this key: immediately put.
			transactionState.revert(key);
		}
		return this.targetCache.putIfAbsent(key, value);
	}

	@Override
	public void evict(final Object key) {
		final var transactionState = getTransactionState();
		if (transactionState != null) {
			transactionState.evict(key);
		} else {
			this.targetCache.evict(key);
		}
	}

	@Override
	public boolean evictIfPresent(Object key) {
		final var transactionState = getTransactionState();
		if (transactionState != null) {
			// Remove any changes made to this key: immediately evict.
			transactionState.revert(key);
		}
		return this.targetCache.evictIfPresent(key);
	}

	@Override
	public void clear() {
		final var transactionState = getTransactionState();
		if (transactionState != null) {
			transactionState.clear();
		} else {
			this.targetCache.clear();
		}
	}

	@Override
	public boolean invalidate() {
		final var transactionState = getTransactionState();
		if (transactionState != null) {
			// Reset any changes made within the transaction state
			transactionState.reset();
		}
		return this.targetCache.invalidate();
	}

	/**
	 * Keeps track of the state changes which happened in the current transaction.
	 */
	private static class TransactionState {
		private final Map<Object, ValueWrapper> modifications = new LinkedHashMap<>();

		boolean clearCalled = false;

		@Nullable
		ValueWrapper get(Object key) {
			var result = modifications.get(key);
			if (result == null && clearCalled) {
				result = new SimpleValueWrapper(EVICTED);
			}
			return result;
		}


		void clear() {
			clearCalled = true;
			modifications.clear();
		}

		void reset() {
			clearCalled = false;
			modifications.clear();
		}

		void revert(Object key) {
			modifications.remove(key);
		}

		void evict(Object key) {
			modifications.put(key, new SimpleValueWrapper(EVICTED));
		}

		void put(Object key, @Nullable Object value) {
			modifications.put(key, new SimpleValueWrapper(value));
		}

		void commitTo(Cache cache) {
			if (clearCalled) {
				cache.clear();
			}
			modifications.forEach((key, valueWrapper) -> {
				final var value = valueWrapper.get();
				if (value == EVICTED) {
					cache.evict(key);
				} else {
					cache.put(key, value);
				}
			});
		}
	}

	/**
	 * Converts the wrapper to effective wrapper, i.e. returns null if the wrapper contains the special EVICTED value and the wrapper otherwise.
	 *
	 * @param wrapper The wrapper to convert
	 * @return The converted wrapper
	 */
	@Nullable
	private static ValueWrapper convert(@Nullable ValueWrapper wrapper) {
		if (wrapper != null && wrapper.get() == EVICTED) {
			return null;
		}
		return wrapper;
	}

	/**
	 * Converts the value of the specified wrapper, i.e. returns null if the wrapper contains the special EVICTED value or its value otherwise.
	 *
	 * @param wrapper The wrapper to extract the value from
	 * @return The converted value
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	private static <T> T convertValue(@Nullable ValueWrapper wrapper) {
		final var effectiveWrapper = convert(wrapper);
		return effectiveWrapper == null ? null : (T)effectiveWrapper.get();
	}

	/**
	 * Requires the specified value to be null or be an instance of the specified type.
	 * Throws an IllegalStateException otherwise.
	 *
	 * @param value The value
	 * @param type The type
	 * @return The cast value
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T cast(@Nullable Object value, @Nullable Class<T> type) {
		if (value != null && type != null && !type.isInstance(value)) {
			throw new IllegalStateException(
					"Cached value is not of required type [" + type.getName() + "]: " + value);
		}
		return (T)value;
	}

	/**
	 * Gets the current transaction state or null if no transaction is active.
	 * When first invoked within a transaction, a transaction synchronization is registered which will apply any changes in the current transaction on commit.
	 *
	 * @return The current transaction state
	 */
	@Nullable
	private TransactionState getTransactionState() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			var state = transactionState.get();
			if (state == null) {
				state = new TransactionState();
				transactionState.set(state);
				TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
					@Override
					public void afterCompletion(int status) {
						final var currentState = Objects.requireNonNull(transactionState.get());
						// Transfer any modifications to the underlying cache if the transaction committed
						if (status == STATUS_COMMITTED) {
							if (synchronizeCommits) {
								synchronized (targetCache) {
									currentState.commitTo(targetCache);
								}
							} else {
								currentState.commitTo(targetCache);
							}
						}
						transactionState.remove();
					}
				});
			}
			return state;
		} else {
			return null;
		}
	}
}
