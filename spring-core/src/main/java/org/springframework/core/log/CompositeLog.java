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


	private final List<Log> loggers;


	/**
	 * Package-private constructor with list of loggers.
	 * @param loggers the loggers to use
	 */
	CompositeLog(List<Log> loggers) {
		this.loggers = loggers;
	}


	@Override
	public boolean isFatalEnabled() {
		return isEnabled(Log::isFatalEnabled);
	}

	@Override
	public boolean isErrorEnabled() {
		return isEnabled(Log::isErrorEnabled);
	}

	@Override
	public boolean isWarnEnabled() {
		return isEnabled(Log::isWarnEnabled);
	}

	@Override
	public boolean isInfoEnabled() {
		return isEnabled(Log::isInfoEnabled);
	}

	@Override
	public boolean isDebugEnabled() {
		return isEnabled(Log::isDebugEnabled);
	}

	@Override
	public boolean isTraceEnabled() {
		return isEnabled(Log::isTraceEnabled);
	}

	private boolean isEnabled(Predicate<Log> predicate) {
		return (getLogger(predicate) != NO_OP_LOG);
	}

	@Override
	public void fatal(Object message) {
		getLogger(Log::isFatalEnabled).fatal(message);
	}

	@Override
	public void fatal(Object message, Throwable ex) {
		getLogger(Log::isFatalEnabled).fatal(message, ex);
	}

	@Override
	public void error(Object message) {
		getLogger(Log::isErrorEnabled).error(message);
	}

	@Override
	public void error(Object message, Throwable ex) {
		getLogger(Log::isErrorEnabled).error(message, ex);
	}

	@Override
	public void warn(Object message) {
		getLogger(Log::isWarnEnabled).warn(message);
	}

	@Override
	public void warn(Object message, Throwable ex) {
		getLogger(Log::isWarnEnabled).warn(message, ex);
	}

	@Override
	public void info(Object message) {
		getLogger(Log::isInfoEnabled).info(message);
	}

	@Override
	public void info(Object message, Throwable ex) {
		getLogger(Log::isInfoEnabled).info(message, ex);
	}

	@Override
	public void debug(Object message) {
		getLogger(Log::isDebugEnabled).debug(message);
	}

	@Override
	public void debug(Object message, Throwable ex) {
		getLogger(Log::isDebugEnabled).debug(message, ex);
	}

	@Override
	public void trace(Object message) {
		getLogger(Log::isTraceEnabled).trace(message);
	}

	@Override
	public void trace(Object message, Throwable ex) {
		getLogger(Log::isTraceEnabled).trace(message, ex);
	}

	private Log getLogger(Predicate<Log> predicate) {
		for (Log logger : this.loggers) {
			if (predicate.test(logger)) {
				return logger;
			}
		}
		return NO_OP_LOG;
	}

}
