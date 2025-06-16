/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.cache.interceptor;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.Cache;
import org.springframework.util.function.SingletonSupplier;

/**
 * A base component for invoking {@link Cache} operations and using a
 * configurable {@link CacheErrorHandler} when an exception occurs.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author Simon Baslé
 * @since 4.1
 * @see org.springframework.cache.interceptor.CacheErrorHandler
 */
public abstract class AbstractCacheInvoker {

	protected SingletonSupplier<CacheErrorHandler> errorHandler;


	protected AbstractCacheInvoker() {
		this.errorHandler = SingletonSupplier.of(SimpleCacheErrorHandler::new);
	}

	protected AbstractCacheInvoker(CacheErrorHandler errorHandler) {
		this.errorHandler = SingletonSupplier.of(errorHandler);
	}


	/**
	 * Set the {@link CacheErrorHandler} instance to use to handle errors
	 * thrown by the cache provider. By default, a {@link SimpleCacheErrorHandler}
	 * is used who throws any exception as is.
	 */
	public void setErrorHandler(CacheErrorHandler errorHandler) {
		this.errorHandler = SingletonSupplier.of(errorHandler);
	}

	/**
	 * Return the {@link CacheErrorHandler} to use.
	 */
	public CacheErrorHandler getErrorHandler() {
		return this.errorHandler.obtain();
	}


	/**
	 * Execute {@link Cache#get(Object)} on the specified {@link Cache} and
	 * invoke the error handler if an exception occurs. Return {@code null}
	 * if the handler does not throw any exception, which simulates a cache
	 * miss in case of error.
	 * @see Cache#get(Object)
	 */
	protected Cache.@Nullable ValueWrapper doGet(Cache cache, Object key) {
		try {
			return cache.get(key);
		}
		catch (RuntimeException ex) {
			getErrorHandler().handleCacheGetError(ex, cache, key);
			return null;  // If the exception is handled, return a cache miss
		}
	}

	/**
	 * Execute {@link Cache#get(Object, Callable)} on the specified
	 * {@link Cache} and invoke the error handler if an exception occurs.
	 * Invokes the {@code valueLoader} if the handler does not throw any
	 * exception, which simulates a cache read-through in case of error.
	 * @since 6.2
	 * @see Cache#get(Object, Callable)
	 */
	protected <T> @Nullable T doGet(Cache cache, Object key, Callable<T> valueLoader) {
		try {
			return cache.get(key, valueLoader);
		}
		catch (Cache.ValueRetrievalException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			getErrorHandler().handleCacheGetError(ex, cache, key);
			try {
				return valueLoader.call();
			}
			catch (Exception ex2) {
				throw new Cache.ValueRetrievalException(key, valueLoader, ex);
			}
		}
	}


	/**
	 * Execute {@link Cache#retrieve(Object)} on the specified {@link Cache}
	 * and invoke the error handler if an exception occurs.
	 * Returns {@code null} if the handler does not throw any exception, which
	 * simulates a cache miss in case of error.
	 * @since 6.2
	 * @see Cache#retrieve(Object)
	 */
	protected @Nullable CompletableFuture<?> doRetrieve(Cache cache, Object key) {
		try {
			return cache.retrieve(key);
		}
		catch (RuntimeException ex) {
			getErrorHandler().handleCacheGetError(ex, cache, key);
			return null;
		}
	}

	/**
	 * Execute {@link Cache#retrieve(Object, Supplier)} on the specified
	 * {@link Cache} and invoke the error handler if an exception occurs.
	 * Invokes the {@code valueLoader} if the handler does not throw any
	 * exception, which simulates a cache read-through in case of error.
	 * @since 6.2
	 * @see Cache#retrieve(Object, Supplier)
	 */
	protected <T> CompletableFuture<T> doRetrieve(Cache cache, Object key, Supplier<CompletableFuture<T>> valueLoader) {
		try {
			return cache.retrieve(key, valueLoader);
		}
		catch (RuntimeException ex) {
			getErrorHandler().handleCacheGetError(ex, cache, key);
			return valueLoader.get();
		}
	}

	/**
	 * Execute {@link Cache#put(Object, Object)} on the specified {@link Cache}
	 * and invoke the error handler if an exception occurs.
	 */
	protected void doPut(Cache cache, Object key, @Nullable Object value) {
		try {
			cache.put(key, value);
		}
		catch (RuntimeException ex) {
			getErrorHandler().handleCachePutError(ex, cache, key, value);
		}
	}

	/**
	 * Execute {@link Cache#evict(Object)}/{@link Cache#evictIfPresent(Object)} on the
	 * specified {@link Cache} and invoke the error handler if an exception occurs.
	 */
	protected void doEvict(Cache cache, Object key, boolean immediate) {
		try {
			if (immediate) {
				cache.evictIfPresent(key);
			}
			else {
				cache.evict(key);
			}
		}
		catch (RuntimeException ex) {
			getErrorHandler().handleCacheEvictError(ex, cache, key);
		}
	}

	/**
	 * Execute {@link Cache#clear()} on the specified {@link Cache} and
	 * invoke the error handler if an exception occurs.
	 */
	protected void doClear(Cache cache, boolean immediate) {
		try {
			if (immediate) {
				cache.invalidate();
			}
			else {
				cache.clear();
			}
		}
		catch (RuntimeException ex) {
			getErrorHandler().handleCacheClearError(ex, cache);
		}
	}

}
