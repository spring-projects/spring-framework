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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.support.NoOpCache;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LoggingCacheErrorHandler}.
 *
 * @author Adam Ostrožlík
 */
@ExtendWith(MockitoExtension.class)
public class LoggingCacheErrorHandlerTests {

	@Mock
	Log logger;

	@Test
	void givenHandlerWhenHandleGetErrorThenLogWithoutException() {
		LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler.Builder()
				.setLogger(logger)
				.build();
		handler.handleCacheGetError(new RuntimeException(), new NoOpCache("NOOP"), "key");
		verify(logger, times(1)).warn(anyString());
	}

	@Test
	void givenHandlerWhenHandleGetErrorThenLogWithException() {
		LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler.Builder()
				.setLogger(logger)
				.setIncludeStacktrace(true)
				.build();
		RuntimeException exception = new RuntimeException();
		handler.handleCacheGetError(exception, new NoOpCache("NOOP"), "key");
		verify(logger, times(1)).warn(anyString(), eq(exception));
	}

}
