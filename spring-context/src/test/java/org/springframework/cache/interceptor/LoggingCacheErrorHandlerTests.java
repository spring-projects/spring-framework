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
import org.junit.jupiter.api.Test;

import org.springframework.cache.support.NoOpCache;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LoggingCacheErrorHandler}.
 *
 * @author Adam Ostrožlík
 * @author Stephane Nicoll
 */
public class LoggingCacheErrorHandlerTests {

	@Test
	void handleGetCacheErrorLogsAppropriateMessage() {
		Log logger = mock(Log.class);
		LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler(logger, false);
		handler.handleCacheGetError(new RuntimeException(), new NoOpCache("NOOP"), "key");
		verify(logger).warn("Cache 'NOOP' failed to get entry with key 'key'");
	}

	@Test
	void handlePutCacheErrorLogsAppropriateMessage() {
		Log logger = mock(Log.class);
		LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler(logger, false);
		handler.handleCachePutError(new RuntimeException(), new NoOpCache("NOOP"), "key", new Object());
		verify(logger).warn("Cache 'NOOP' failed to put entry with key 'key'");
	}

	@Test
	void handleEvictCacheErrorLogsAppropriateMessage() {
		Log logger = mock(Log.class);
		LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler(logger, false);
		handler.handleCacheEvictError(new RuntimeException(), new NoOpCache("NOOP"), "key");
		verify(logger).warn("Cache 'NOOP' failed to evict entry with key 'key'");
	}

	@Test
	void handleClearErrorLogsAppropriateMessage() {
		Log logger = mock(Log.class);
		LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler(logger, false);
		handler.handleCacheClearError(new RuntimeException(), new NoOpCache("NOOP"));
		verify(logger).warn("Cache 'NOOP' failed to clear entries");
	}

	@Test
	void handleCacheErrorWithStacktrace() {
		Log logger = mock(Log.class);
		LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler(logger, true);
		RuntimeException exception = new RuntimeException();
		handler.handleCacheGetError(exception, new NoOpCache("NOOP"), "key");
		verify(logger).warn("Cache 'NOOP' failed to get entry with key 'key'", exception);
	}

}
