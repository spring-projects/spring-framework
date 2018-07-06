/*
 * Copyright 2002-2018 the original author or authors.
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
package org.springframework.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.NoOpLog;

import org.springframework.util.ObjectUtils;

/**
 * Composite {@link Log} configured with a primary logger and a list of secondary
 * ones to fall back on if the main one is not enabled.
 *
 * <p>This class also declares {@link #webLogger} for use as fallback when
 * logging in the "org.springframework.http" package.
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 */
public final class HttpLog implements Log {

	/**
	 * Logger with category "org.springframework.web.HTTP" to use as fallback
	 * if "org.springframework.web" is on.
	 */
	public static final Log webLogger = LogFactory.getLog("org.springframework.web.HTTP");

	private static final Log noOpLog = new NoOpLog();


	private final Log fatalLogger;

	private final Log errorLogger;

	private final Log warnLogger;

	private final Log infoLogger;

	private final Log debugLogger;

	private final Log traceLogger;


	private HttpLog(List<Log> loggers) {
		this.fatalLogger = initLogger(loggers, Log::isFatalEnabled);
		this.errorLogger = initLogger(loggers, Log::isErrorEnabled);
		this.warnLogger  = initLogger(loggers, Log::isWarnEnabled);
		this.infoLogger  = initLogger(loggers, Log::isInfoEnabled);
		this.debugLogger = initLogger(loggers, Log::isDebugEnabled);
		this.traceLogger = initLogger(loggers, Log::isTraceEnabled);
	}

	private static Log initLogger(List<Log> loggers, Predicate<Log> predicate) {
		return loggers.stream().filter(predicate).findFirst().orElse(noOpLog);
	}


	@Override
	public boolean isFatalEnabled() {
		return this.fatalLogger != noOpLog;
	}

	@Override
	public boolean isErrorEnabled() {
		return this.errorLogger != noOpLog;
	}

	@Override
	public boolean isWarnEnabled() {
		return this.warnLogger != noOpLog;
	}

	@Override
	public boolean isInfoEnabled() {
		return this.infoLogger != noOpLog;
	}

	@Override
	public boolean isDebugEnabled() {
		return this.debugLogger != noOpLog;
	}

	@Override
	public boolean isTraceEnabled() {
		return this.traceLogger != noOpLog;
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
		this.errorLogger.error(message);
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


	/**
	 * Create a composite logger that uses the given primary logger, if enabled,
	 * or falls back on {@link #webLogger}.
	 * @param primaryLogger the primary logger
	 * @return a composite logger
	 */
	public static Log create(Log primaryLogger) {
		return createWith(primaryLogger, webLogger);
	}

	/**
	 * Create a composite logger that uses the given primary logger, if enabled,
	 * or falls back on one of the given secondary loggers..
	 * @param primaryLogger the primary logger
	 * @param secondaryLoggers fallback loggers
	 * @return a composite logger
	 */
	public static Log createWith(Log primaryLogger, Log... secondaryLoggers) {
		if (ObjectUtils.isEmpty(secondaryLoggers)) {
			return primaryLogger;
		}
		List<Log> loggers = new ArrayList<>(1 + secondaryLoggers.length);
		loggers.add(primaryLogger);
		Collections.addAll(loggers, secondaryLoggers);
		return new HttpLog(loggers);
	}

}
