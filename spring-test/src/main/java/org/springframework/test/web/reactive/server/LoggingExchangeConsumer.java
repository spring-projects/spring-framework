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
 * Provides options for logging information about the performed exchange.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class LoggingExchangeConsumer {

	private static Log logger = LogFactory.getLog(LoggingExchangeConsumer.class);


	private final ExchangeActions exchangeActions;


	public LoggingExchangeConsumer(ExchangeActions exchangeActions) {
		this.exchangeActions = exchangeActions;
	}


	/**
	 * Log with {@link System#out}.
	 */
	public ExchangeActions toConsole() {
		System.out.println(getOutput());
		return this.exchangeActions;
	}

	/**
	 * Log with a given {@link OutputStream}.
	 */
	public ExchangeActions toOutputStream(OutputStream stream) {
		return toWriter(new PrintWriter(stream, true));
	}

	/**
	 * Log with a given {@link Writer}.
	 */
	public ExchangeActions toWriter(Writer writer) {
		try {
			writer.write(getOutput());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to print exchange info", ex);
		}
		return this.exchangeActions;
	}

	/**
	 * Log if TRACE level logging is enabled.
	 */
	public ExchangeActions ifTraceEnabled() {
		return doLog(Log::isTraceEnabled, Log::trace);
	}

	/**
	 * Log if DEBUG level logging is enabled.
	 */
	public ExchangeActions ifDebugEnabled() {
		return doLog(Log::isDebugEnabled, Log::debug);
	}

	/**
	 * Log if INFO level logging is enabled.
	 */
	public ExchangeActions ifInfoEnabled() {
		return doLog(Log::isInfoEnabled, Log::info);
	}

	private ExchangeActions doLog(Predicate<Log> logLevelPredicate, BiConsumer<Log, String> logAction) {
		if (logLevelPredicate.test(logger)) {
			logAction.accept(logger, getOutput());
		}
		return this.exchangeActions;
	}

	private String getOutput() {
		return this.exchangeActions.andReturn().toString();
	}

}
