/*
 * Copyright 2002-present the original author or authors.
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

import java.util.function.Supplier;

import guru.mocker.annotation.mixin.Mixin;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A convenient accessor for Commons Logging, providing not only
 * {@code CharSequence} based log methods but also {@code Supplier}
 * based variants for use with Java lambda expressions.
 *
 * @author Juergen Hoeller
 * @since 5.2
 */
public class LogAccessor extends LogAccessorForwarder {

	/**
	 * Create a new accessor for the given Commons Log.
	 * @see LogFactory#getLog(Class)
	 * @see LogFactory#getLog(String)
	 */
	@Mixin
	public LogAccessor(Log log) {
		super(log);
	}

	/**
	 * Create a new accessor for the specified Commons Log category.
	 * @see LogFactory#getLog(Class)
	 */
	public LogAccessor(Class<?> logCategory) {
		this(LogFactory.getLog(logCategory));
	}

	/**
	 * Create a new accessor for the specified Commons Log category.
	 * @see LogFactory#getLog(String)
	 */
	public LogAccessor(String logCategory) {
		this(LogFactory.getLog(logCategory));
	}

	/**
	 * Return the target Commons Log.
	 */
	public final Log getLog() {
		return log;
	}

	// Supplier-based log methods

	/**
	 * Log a message with fatal log level.
	 * @param messageSupplier a lazy supplier for the message to log
	 */
	public void fatal(Supplier<? extends CharSequence> messageSupplier) {
		if (super.isFatalEnabled()) {
			super.fatal(LogMessage.of(messageSupplier));
		}
	}

	/**
	 * Log an error with fatal log level.
	 * @param cause the exception to log
	 * @param messageSupplier a lazy supplier for the message to log
	 */
	public void fatal(Throwable cause, Supplier<? extends CharSequence> messageSupplier) {
		if (super.isFatalEnabled()) {
			super.fatal(LogMessage.of(messageSupplier), cause);
		}
	}

	/**
	 * Log a message with error log level.
	 * @param messageSupplier a lazy supplier for the message to log
	 */
	public void error(Supplier<? extends CharSequence> messageSupplier) {
		if (super.isErrorEnabled()) {
			super.error(LogMessage.of(messageSupplier));
		}
	}

	/**
	 * Log an error with error log level.
	 * @param cause the exception to log
	 * @param messageSupplier a lazy supplier for the message to log
	 */
	public void error(Throwable cause, Supplier<? extends CharSequence> messageSupplier) {
		if (super.isErrorEnabled()) {
			super.error(LogMessage.of(messageSupplier), cause);
		}
	}

	/**
	 * Log a message with warn log level.
	 * @param messageSupplier a lazy supplier for the message to log
	 */
	public void warn(Supplier<? extends CharSequence> messageSupplier) {
		if (super.isWarnEnabled()) {
			super.warn(LogMessage.of(messageSupplier));
		}
	}

	/**
	 * Log an error with warn log level.
	 * @param cause the exception to log
	 * @param messageSupplier a lazy supplier for the message to log
	 */
	public void warn(Throwable cause, Supplier<? extends CharSequence> messageSupplier) {
		if (super.isWarnEnabled()) {
			super.warn(LogMessage.of(messageSupplier), cause);
		}
	}

	/**
	 * Log a message with info log level.
	 * @param messageSupplier a lazy supplier for the message to log
	 */
	public void info(Supplier<? extends CharSequence> messageSupplier) {
		if (super.isInfoEnabled()) {
			super.info(LogMessage.of(messageSupplier));
		}
	}

	/**
	 * Log an error with info log level.
	 * @param cause the exception to log
	 * @param messageSupplier a lazy supplier for the message to log
	 */
	public void info(Throwable cause, Supplier<? extends CharSequence> messageSupplier) {
		if (super.isInfoEnabled()) {
			super.info(LogMessage.of(messageSupplier), cause);
		}
	}

	/**
	 * Log a message with debug log level.
	 * @param messageSupplier a lazy supplier for the message to log
	 */
	public void debug(Supplier<? extends CharSequence> messageSupplier) {
		if (super.isDebugEnabled()) {
			super.debug(LogMessage.of(messageSupplier));
		}
	}

	/**
	 * Log an error with debug log level.
	 * @param cause the exception to log
	 * @param messageSupplier a lazy supplier for the message to log
	 */
	public void debug(Throwable cause, Supplier<? extends CharSequence> messageSupplier) {
		if (super.isDebugEnabled()) {
			super.debug(LogMessage.of(messageSupplier), cause);
		}
	}

	/**
	 * Log a message with trace log level.
	 * @param messageSupplier a lazy supplier for the message to log
	 */
	public void trace(Supplier<? extends CharSequence> messageSupplier) {
		if (super.isTraceEnabled()) {
			super.trace(LogMessage.of(messageSupplier));
		}
	}

	/**
	 * Log an error with trace log level.
	 * @param cause the exception to log
	 * @param messageSupplier a lazy supplier for the message to log
	 */
	public void trace(Throwable cause, Supplier<? extends CharSequence> messageSupplier) {
		if (super.isTraceEnabled()) {
			super.trace(LogMessage.of(messageSupplier), cause);
		}
	}

}
