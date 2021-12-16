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
import org.springframework.util.Assert;

/**
 * A {@link CacheErrorHandler} implementation that simply logs error message.
 *
 * @author Adam Ostrožlík
 * @since 5.3
 */
public final class LoggingCacheErrorHandler implements CacheErrorHandler {

	private final Log logger;
	private final boolean includeStacktrace;

	/**
	 * Construct new {@link LoggingCacheErrorHandler} that may log stacktraces.
	 * @param includeStacktrace whether to log or not log stacktraces.
	 * @param logger custom logger.
	 */
	private LoggingCacheErrorHandler(boolean includeStacktrace, Log logger) {
		Assert.notNull(logger, "logger cannot be null");
		this.includeStacktrace = includeStacktrace;
		this.logger = logger;
	}

	@Override
	public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
		logCacheError("Cache '" + cache.getName() + "' failed to get entry with key '" + key + "'", exception);
	}

	@Override
	public void handleCachePutError(RuntimeException exception, Cache cache, Object key, @Nullable Object value) {
		logCacheError("Cache '" + cache.getName() + "' failed to put entry with key '" + key + "'", exception);
	}

	@Override
	public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
		logCacheError("Cache '" + cache.getName() + "' failed to evict entry with key '" + key + "'", exception);
	}

	@Override
	public void handleCacheClearError(RuntimeException exception, Cache cache) {
		logCacheError("Cache '" + cache.getName() + "' failed to clear itself", exception);
	}

	private void logCacheError(String msg, RuntimeException ex) {
		if (this.includeStacktrace) {
			logger.warn(msg, ex);
		}
		else {
			logger.warn(msg);
		}
	}

	/**
	 * Builder class for {@link LoggingCacheErrorHandler}.
	 */
	public static class Builder {
		private Log logger;
		private boolean includeStacktrace;

		/**
		 * Overrides default logger.
		 * @param logger new logger.
		 * @return this builder.
		 */
		public Builder setLogger(Log logger) {
			this.logger = logger;
			return this;
		}

		/**
		 * Enable/disable logging of stacktraces.
		 * @param includeStacktrace true - include stacktraces; false otherwise.
		 * @return this builder.
		 */
		public Builder setIncludeStacktrace(boolean includeStacktrace) {
			this.includeStacktrace = includeStacktrace;
			return this;
		}

		public LoggingCacheErrorHandler build() {
			if (logger == null) {
				return new LoggingCacheErrorHandler(this.includeStacktrace, LogFactory.getLog(LoggingCacheErrorHandler.class));
			}
			return new LoggingCacheErrorHandler(this.includeStacktrace, logger);
		}
	}
}
