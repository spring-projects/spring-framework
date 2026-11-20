/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.interceptor;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.support.NoOpCache;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LoggingCacheErrorHandler}.
 *
 * @author Adam Ostrožlík
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author Sam Brannen
 */
class LoggingCacheErrorHandlerTests {

	private static final Cache CACHE = new NoOpCache("NOOP");

	private static final String KEY = "enigma";

	private final Log logger = mock();

	private LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler(this.logger, false);


	@BeforeEach
	void setUp() {
		given(this.logger.isWarnEnabled()).willReturn(true);
	}


	@Test
	void handleGetCacheErrorLogsAppropriateMessage() {
		this.handler.handleCacheGetError(new RuntimeException(), CACHE, KEY);
		verify(this.logger).warn("Cache 'NOOP' failed to get entry with key 'enigma'");
	}

	@Test
	void handlePutCacheErrorLogsAppropriateMessage() {
		this.handler.handleCachePutError(new RuntimeException(), CACHE, KEY, null);
		verify(this.logger).warn("Cache 'NOOP' failed to put entry with key 'enigma'");
	}

	@Test
	void handleEvictCacheErrorLogsAppropriateMessage() {
		this.handler.handleCacheEvictError(new RuntimeException(), CACHE, KEY);
		verify(this.logger).warn("Cache 'NOOP' failed to evict entry with key 'enigma'");
	}

	@Test
	void handleClearErrorLogsAppropriateMessage() {
		this.handler.handleCacheClearError(new RuntimeException(), CACHE);
		verify(this.logger).warn("Cache 'NOOP' failed to clear entries");
	}

	@Test
	void handleGetCacheErrorWithStackTraceLoggingEnabled() {
		this.handler = new LoggingCacheErrorHandler(this.logger, true);
		RuntimeException exception = new RuntimeException();
		this.handler.handleCacheGetError(exception, CACHE, KEY);
		verify(this.logger).warn("Cache 'NOOP' failed to get entry with key 'enigma'", exception);
	}

	@Test
	void constructorWithLoggerName() {
		assertThatCode(() -> new LoggingCacheErrorHandler("org.apache.commons.logging.Log", true))
				.doesNotThrowAnyException();
	}

}
