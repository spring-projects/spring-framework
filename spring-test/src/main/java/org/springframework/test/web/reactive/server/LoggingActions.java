/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.test.web.reactive.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides options for logging diagnostic information about the exchange.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see ResponseAssertions#log()
 */
public class LoggingActions<T> {

	private static Log logger = LogFactory.getLog(LoggingActions.class);

	private final ResponseAssertions<T> resultAssertions;

	private final ExchangeResult<T> exchange;


	public LoggingActions(ExchangeResult<T> exchange, ResponseAssertions<T> resultAssertions) {
		this.resultAssertions = resultAssertions;
		this.exchange = exchange;
	}


	/**
	 * Log with {@link System#out}.
	 */
	public ResponseAssertions<T> toConsole() {
		return toOutputStream(System.out);
	}

	/**
	 * Log with a given {@link OutputStream}.
	 */
	public ResponseAssertions<T> toOutputStream(OutputStream stream) {
		return toWriter(new PrintWriter(stream, true));
	}

	/**
	 * Log with a given {@link Writer}.
	 */
	public ResponseAssertions<T> toWriter(Writer writer) {
		try {
			writer.write(getOutput());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to print exchange info", ex);
		}
		return this.resultAssertions;
	}

	/**
	 * Log if TRACE level logging is enabled.
	 */
	public ResponseAssertions<T> ifTraceEnabled() {
		return doLog(Log::isTraceEnabled, Log::trace);
	}

	/**
	 * Log if DEBUG level logging is enabled.
	 */
	public ResponseAssertions<T> ifDebugEnabled() {
		return doLog(Log::isDebugEnabled, Log::debug);
	}

	/**
	 * Log if INFO level logging is enabled.
	 */
	public ResponseAssertions<T> ifInfoEnabled() {
		return doLog(Log::isInfoEnabled, Log::info);
	}


	private ResponseAssertions<T> doLog(Predicate<Log> predicate, BiConsumer<Log, String> consumer) {
		if (predicate.test(logger)) {
			consumer.accept(logger, getOutput());
		}
		return this.resultAssertions;
	}

	private String getOutput() {
		return this.resultAssertions.toString();
	}

}
