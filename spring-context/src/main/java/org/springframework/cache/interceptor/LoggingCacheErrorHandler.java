/*
 * Copyright 2002-2021 the original author or authors.
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

/**
 * A {@link CacheErrorHandler} implementation that simply logs exception.
 *
 * @author Adam Ostrožlík
 * @since 5.3
 */
public class LoggingCacheErrorHandler implements CacheErrorHandler {

	protected static final Log logger = LogFactory.getLog(LoggingCacheErrorHandler.class);
	private final boolean includeStacktrace;

	/**
	 * Construct new {@link LoggingCacheErrorHandler} that does not log stacktraces.
	 */
	public LoggingCacheErrorHandler() {
		this.includeStacktrace = false;
	}

	/**
	 * Construct new {@link LoggingCacheErrorHandler} that may log stacktraces.
	 * @param includeStacktrace whether to log or not log stacktraces.
	 */
	public LoggingCacheErrorHandler(boolean includeStacktrace) {
		this.includeStacktrace = includeStacktrace;
	}

	@Override
	public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
		logCacheError("Cache get error for key " + key + " and cache " + cache.getName(), exception);
	}

	@Override
	public void handleCachePutError(RuntimeException exception, Cache cache, Object key, @Nullable Object value) {
		logCacheError("Cache put error for key " + key + " and cache " + cache.getName(), exception);
	}

	@Override
	public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
		logCacheError("Cache evict error for key " + key + " and cache " + cache.getName(), exception);
	}

	@Override
	public void handleCacheClearError(RuntimeException exception, Cache cache) {
		logCacheError("Cache clear error for cache " + cache.getName(), exception);
	}

	public boolean isIncludeStacktrace() {
		return this.includeStacktrace;
	}

	protected void logCacheError(String msg, RuntimeException ex) {
		if (isIncludeStacktrace()) {
			logger.warn(msg, ex);
		}
		else {
			logger.warn(msg);
		}
	}
}
