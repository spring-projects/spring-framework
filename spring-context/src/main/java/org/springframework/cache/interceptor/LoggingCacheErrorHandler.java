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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A {@link CacheErrorHandler} implementation that logs error message. Can be
 * used when underlying cache errors should be ignored.
 *
 * @author Adam Ostrožlík
 * @author Stephane Nicoll
 * @since 5.3.16
 */
public class LoggingCacheErrorHandler implements CacheErrorHandler {

	private final Log logger;

	private final boolean logStacktrace;


	/**
	 * Create an instance with the {@link Log logger} to use.
	 * @param logger the logger to use
	 * @param logStacktrace whether to log stack trace
	 */
	public LoggingCacheErrorHandler(Log logger, boolean logStacktrace) {
		Assert.notNull(logger, "Logger must not be null");
		this.logger = logger;
		this.logStacktrace = logStacktrace;
	}

	/**
	 * Create an instance that does not log stack traces.
	 */
	public LoggingCacheErrorHandler() {
		this(LogFactory.getLog(LoggingCacheErrorHandler.class), false);
	}


	@Override
	public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
		logCacheError(logger,
				createMessage(cache, "failed to get entry with key '" + key + "'"),
				exception);
	}

	@Override
	public void handleCachePutError(RuntimeException exception, Cache cache, Object key, @Nullable Object value) {
		logCacheError(logger,
				createMessage(cache, "failed to put entry with key '" + key + "'"),
				exception);
	}

	@Override
	public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
		logCacheError(logger,
				createMessage(cache, "failed to evict entry with key '" + key + "'"),
				exception);
	}

	@Override
	public void handleCacheClearError(RuntimeException exception, Cache cache) {
		logCacheError(logger, createMessage(cache, "failed to clear entries"), exception);
	}

	/**
	 * Log the specified message.
	 * @param logger the logger
	 * @param message the message
	 * @param ex the exception
	 */
	protected void logCacheError(Log logger, String message, RuntimeException ex) {
		if (this.logStacktrace) {
			logger.warn(message, ex);
		}
		else {
			logger.warn(message);
		}
	}

	private String createMessage(Cache cache, String reason) {
		return String.format("Cache '%s' %s", cache.getName(), reason);
	}

}
