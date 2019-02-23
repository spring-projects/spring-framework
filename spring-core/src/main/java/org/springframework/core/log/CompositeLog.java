/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.log;

import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.NoOpLog;

/**
 * Implementation of {@link Log} that wraps a list of loggers and delegates
 * to the first one for which logging is enabled at the given level.
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 * @see LogDelegateFactory#getCompositeLog
 */
final class CompositeLog implements Log {

	private static final Log NO_OP_LOG = new NoOpLog();


	private final Log fatalLogger;

	private final Log errorLogger;

	private final Log warnLogger;

	private final Log infoLogger;

	private final Log debugLogger;

	private final Log traceLogger;


	/**
	 * Constructor with list of loggers. For optimal performance, the constructor
	 * checks and remembers which logger is on for each log category.
	 * @param loggers the loggers to use
	 */
	public CompositeLog(List<Log> loggers) {
		this.fatalLogger = initLogger(loggers, Log::isFatalEnabled);
		this.errorLogger = initLogger(loggers, Log::isErrorEnabled);
		this.warnLogger  = initLogger(loggers, Log::isWarnEnabled);
		this.infoLogger  = initLogger(loggers, Log::isInfoEnabled);
		this.debugLogger = initLogger(loggers, Log::isDebugEnabled);
		this.traceLogger = initLogger(loggers, Log::isTraceEnabled);
	}

	private static Log initLogger(List<Log> loggers, Predicate<Log> predicate) {
		for (Log logger : loggers) {
			if (predicate.test(logger)) {
				return logger;
			}
		}
		return NO_OP_LOG;
	}


	@Override
	public boolean isFatalEnabled() {
		return this.fatalLogger != NO_OP_LOG;
	}

	@Override
	public boolean isErrorEnabled() {
		return this.errorLogger != NO_OP_LOG;
	}

	@Override
	public boolean isWarnEnabled() {
		return this.warnLogger != NO_OP_LOG;
	}

	@Override
	public boolean isInfoEnabled() {
		return this.infoLogger != NO_OP_LOG;
	}

	@Override
	public boolean isDebugEnabled() {
		return this.debugLogger != NO_OP_LOG;
	}

	@Override
	public boolean isTraceEnabled() {
		return this.traceLogger != NO_OP_LOG;
	}

	@Override
	public void fatal(Object message) {
		this.fatalLogger.fatal(message);
	}

	@Override
	public void fatal(Object message, Throwable ex) {
		this.fatalLogger.fatal(message, ex);
	}

	@Override
	public void error(Object message) {
		this.errorLogger.error(message);
	}

	@Override
	public void error(Object message, Throwable ex) {
		this.errorLogger.error(message, ex);
	}

	@Override
	public void warn(Object message) {
		this.warnLogger.warn(message);
	}

	@Override
	public void warn(Object message, Throwable ex) {
		this.warnLogger.warn(message, ex);
	}

	@Override
	public void info(Object message) {
		this.infoLogger.info(message);
	}

	@Override
	public void info(Object message, Throwable ex) {
		this.infoLogger.info(message, ex);
	}

	@Override
	public void debug(Object message) {
		this.debugLogger.debug(message);
	}

	@Override
	public void debug(Object message, Throwable ex) {
		this.debugLogger.debug(message, ex);
	}

	@Override
	public void trace(Object message) {
		this.traceLogger.trace(message);
	}

	@Override
	public void trace(Object message, Throwable ex) {
		this.traceLogger.trace(message, ex);
	}

}
