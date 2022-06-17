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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cache.support.NoOpCache;

import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LoggingCacheErrorHandler}.
 *
 * @author Adam Ostrožlík
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
@ExtendWith(MockitoExtension.class)
class LoggingCacheErrorHandlerTests {

	@Mock
	private Log logger;


	@Test
	void handleGetCacheErrorLogsAppropriateMessage() {
		LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler(this.logger, false);
		handler.handleCacheGetError(new RuntimeException(), new NoOpCache("NOOP"), "key");
		verify(this.logger).warn("Cache 'NOOP' failed to get entry with key 'key'");
	}

	@Test
	void handlePutCacheErrorLogsAppropriateMessage() {
		LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler(this.logger, false);
		handler.handleCachePutError(new RuntimeException(), new NoOpCache("NOOP"), "key", new Object());
		verify(this.logger).warn("Cache 'NOOP' failed to put entry with key 'key'");
	}

	@Test
	void handleEvictCacheErrorLogsAppropriateMessage() {
		LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler(this.logger, false);
		handler.handleCacheEvictError(new RuntimeException(), new NoOpCache("NOOP"), "key");
		verify(this.logger).warn("Cache 'NOOP' failed to evict entry with key 'key'");
	}

	@Test
	void handleClearErrorLogsAppropriateMessage() {
		LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler(this.logger, false);
		handler.handleCacheClearError(new RuntimeException(), new NoOpCache("NOOP"));
		verify(this.logger).warn("Cache 'NOOP' failed to clear entries");
	}

	@Test
	void handleCacheErrorWithStacktrace() {
		LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler(this.logger, true);
		RuntimeException exception = new RuntimeException();
		handler.handleCacheGetError(exception, new NoOpCache("NOOP"), "key");
		verify(this.logger).warn("Cache 'NOOP' failed to get entry with key 'key'", exception);
	}

}
