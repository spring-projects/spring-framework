/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A {@link CacheErrorHandler} implementation that logs error messages.
 *
 * <p>Can be used when underlying cache errors should be ignored.
 *
 * @author Adam Ostrožlík
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author Sam Brannen
 * @since 5.3.16
 */
public class LoggingCacheErrorHandler implements CacheErrorHandler {

	private final Log logger;

	private final boolean logStackTraces;


	/**
	 * Create a {@code LoggingCacheErrorHandler} that uses the default logging
	 * category and does not log stack traces.
	 * <p>The default logging category is
	 * "{@code org.springframework.cache.interceptor.LoggingCacheErrorHandler}".
	 */
	public LoggingCacheErrorHandler() {
		this(false);
	}

	/**
	 * Create a {@code LoggingCacheErrorHandler} that uses the default logging
	 * category and the supplied {@code logStackTraces} flag.
	 * <p>The default logging category is
	 * "{@code org.springframework.cache.interceptor.LoggingCacheErrorHandler}".
	 * @param logStackTraces whether to log stack traces
	 * @since 5.3.22
	 */
	public LoggingCacheErrorHandler(boolean logStackTraces) {
		this(LogFactory.getLog(LoggingCacheErrorHandler.class), logStackTraces);
	}

	/**
	 * Create a {@code LoggingCacheErrorHandler} that uses the supplied
	 * {@link Log logger} and {@code logStackTraces} flag.
	 * @param logger the logger to use
	 * @param logStackTraces whether to log stack traces
	 */
	public LoggingCacheErrorHandler(Log logger, boolean logStackTraces) {
		Assert.notNull(logger, "'logger' must not be null");
		this.logger = logger;
		this.logStackTraces = logStackTraces;
	}

	/**
	 * Create a {@code LoggingCacheErrorHandler} that uses the supplied
	 * {@code loggerName} and {@code logStackTraces} flag.
	 * @param loggerName the name of the logger to use. The name will be passed
	 * to the underlying logger implementation through Commons Logging, getting
	 * interpreted as log category according to the logger's configuration.
	 * @param logStackTraces whether to log stack traces
	 * @since 5.3.24
	 * @see org.apache.commons.logging.LogFactory#getLog(String)
	 * @see java.util.logging.Logger#getLogger(String)
	 */
	public LoggingCacheErrorHandler(String loggerName, boolean logStackTraces) {
		Assert.notNull(loggerName, "'loggerName' must not be null");
		this.logger = LogFactory.getLog(loggerName);
		this.logStackTraces = logStackTraces;
	}


	@Override
	public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
		logCacheError(
				() -> String.format("Cache '%s' failed to get entry with key '%s'", cache.getName(), key),
				exception);
	}

	@Override
	public void handleCachePutError(RuntimeException exception, Cache cache, Object key, @Nullable Object value) {
		logCacheError(
				() -> String.format("Cache '%s' failed to put entry with key '%s'", cache.getName(), key),
				exception);
	}

	@Override
	public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
		logCacheError(
				() -> String.format("Cache '%s' failed to evict entry with key '%s'", cache.getName(), key),
				exception);
	}

	@Override
	public void handleCacheClearError(RuntimeException exception, Cache cache) {
		logCacheError(
				() -> String.format("Cache '%s' failed to clear entries", cache.getName()),
				exception);
	}


	/**
	 * Get the logger for this {@code LoggingCacheErrorHandler}.
	 * @return the logger
	 * @since 5.3.22
	 */
	protected final Log getLogger() {
		return logger;
	}

	/**
	 * Get the {@code logStackTraces} flag for this {@code LoggingCacheErrorHandler}.
	 * @return {@code true} if this {@code LoggingCacheErrorHandler} logs stack traces
	 * @since 5.3.22
	 */
	protected final boolean isLogStackTraces() {
		return this.logStackTraces;
	}

	/**
	 * Log the cache error message in the given supplier.
	 * <p>If {@link #isLogStackTraces()} is {@code true}, the given
	 * {@code exception} will be logged as well.
	 * <p>The default implementation logs the message as a warning.
	 * @param messageSupplier the message supplier
	 * @param exception the exception thrown by the cache provider
	 * @since 5.3.22
	 * @see #isLogStackTraces()
	 * @see #getLogger()
	 */
	protected void logCacheError(Supplier<String> messageSupplier, RuntimeException exception) {
		if (getLogger().isWarnEnabled()) {
			if (isLogStackTraces()) {
				getLogger().warn(messageSupplier.get(), exception);
			}
			else {
				getLogger().warn(messageSupplier.get());
			}
		}
	}

}
