/*
 * Copyright 2002-2019 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A convenient accessor for Commons Logging, providing not only
 * {@code CharSequence} based log methods but also {@code Supplier}
 * based variants for use with Java 8 lambda expressions.
 *
 * @author Juergen Hoeller
 * @since 5.2
 */
public class LogAccessor {

	private final Log log;


	/**
	 * Create a new accessor for the given Commons Log.
	 * @see LogFactory#getLog(Class)
	 * @see LogFactory#getLog(String)
	 */
	public LogAccessor(Log log) {
		this.log = log;
	}

	/**
	 * Create a new accessor for the specified Commons Log category.
	 * @see LogFactory#getLog(Class)
	 */
	public LogAccessor(Class<?> logCategory) {
		this.log = LogFactory.getLog(logCategory);
	}

	/**
	 * Create a new accessor for the specified Commons Log category.
	 * @see LogFactory#getLog(String)
	 */
	public LogAccessor(String logCategory) {
		this.log = LogFactory.getLog(logCategory);
	}


	/**
	 * Return the target Commons Log.
	 */
	public final Log getLog() {
		return this.log;
	}


	// Plain log methods

	/**
	 * Log a message with fatal log level.
	 * @param message the message to log
	 */
	public void fatal(CharSequence message) {
		this.log.fatal(message);
	}

	/**
	 * Log an error with fatal log level.
	 * @param message the message to log
	 * @param t log this cause
	 */
	public void fatal(CharSequence message, Throwable t) {
		this.log.fatal(message, t);
	}

	/**
	 * Log a message with error log level.
	 * @param message the message to log
	 */
	public void error(CharSequence message) {
		this.log.error(message);
	}

	/**
	 * Log an error with error log level.
	 * @param message the message to log
	 * @param t log this cause
	 */
	public void error(CharSequence message, Throwable t) {
		this.log.error(message, t);
	}

	/**
	 * Log a message with warn log level.
	 * @param message the message to log
	 */
	public void warn(CharSequence message) {
		this.log.warn(message);
	}

	/**
	 * Log an error with warn log level.
	 * @param message the message to log
	 * @param t log this cause
	 */
	public void warn(CharSequence message, Throwable t) {
		this.log.warn(message, t);
	}

	/**
	 * Log a message with info log level.
	 * @param message the message to log
	 */
	public void info(CharSequence message) {
		this.log.info(message);
	}

	/**
	 * Log an error with info log level.
	 * @param message the message to log
	 * @param t log this cause
	 */
	public void info(CharSequence message, Throwable t) {
		this.log.info(message, t);
	}

	/**
	 * Log a message with debug log level.
	 * @param message the message to log
	 */
	public void debug(CharSequence message) {
		this.log.debug(message);
	}

	/**
	 * Log an error with debug log level.
	 * @param message the message to log
	 * @param t log this cause
	 */
	public void debug(CharSequence message, Throwable t) {
		this.log.debug(message, t);
	}

	/**
	 * Log a message with trace log level.
	 * @param message the message to log
	 */
	public void trace(CharSequence message) {
		this.log.trace(message);
	}

	/**
	 * Log an error with trace log level.
	 * @param message the message to log
	 * @param t log this cause
	 */
	public void trace(CharSequence message, Throwable t) {
		this.log.trace(message, t);
	}


	// Supplier-based log methods

	/**
	 * Log a message with fatal log level.
	 * @param messageSupplier a lazy supplier for the message to log
	 */
	public void fatal(Supplier<? extends CharSequence> messageSupplier) {
		this.log.fatal(new LogMessage(messageSupplier));
	}

	/**
	 * Log an error with fatal log level.
	 * @param messageSupplier a lazy supplier for the message to log
	 * @param t log this cause
	 */
	public void fatal(Supplier<? extends CharSequence> messageSupplier, Throwable t) {
		this.log.fatal(new LogMessage(messageSupplier), t);
	}

	/**
	 * Log a message with error log level.
	 * @param messageSupplier a lazy supplier for the message to log
	 */
	public void error(Supplier<? extends CharSequence> messageSupplier) {
		this.log.error(new LogMessage(messageSupplier));
	}

	/**
	 * Log an error with error log level.
	 * @param messageSupplier a lazy supplier for the message to log
	 * @param t log this cause
	 */
	public void error(Supplier<? extends CharSequence> messageSupplier, Throwable t) {
		this.log.error(new LogMessage(messageSupplier), t);
	}

	/**
	 * Log a message with warn log level.
	 * @param messageSupplier a lazy supplier for the message to log
	 */
	public void warn(Supplier<? extends CharSequence> messageSupplier) {
		this.log.warn(new LogMessage(messageSupplier));
	}

	/**
	 * Log an error with warn log level.
	 * @param messageSupplier a lazy supplier for the message to log
	 * @param t log this cause
	 */
	public void warn(Supplier<? extends CharSequence> messageSupplier, Throwable t) {
		this.log.warn(new LogMessage(messageSupplier), t);
	}

	/**
	 * Log a message with info log level.
	 * @param messageSupplier a lazy supplier for the message to log
	 */
	public void info(Supplier<? extends CharSequence> messageSupplier) {
		this.log.info(new LogMessage(messageSupplier));
	}

	/**
	 * Log an error with info log level.
	 * @param messageSupplier a lazy supplier for the message to log
	 * @param t log this cause
	 */
	public void info(Supplier<? extends CharSequence> messageSupplier, Throwable t) {
		this.log.info(new LogMessage(messageSupplier), t);
	}

	/**
	 * Log a message with debug log level.
	 * @param messageSupplier a lazy supplier for the message to log
	 */
	public void debug(Supplier<? extends CharSequence> messageSupplier) {
		this.log.debug(new LogMessage(messageSupplier));
	}

	/**
	 * Log an error with debug log level.
	 * @param messageSupplier a lazy supplier for the message to log
	 * @param t log this cause
	 */
	public void debug(Supplier<? extends CharSequence> messageSupplier, Throwable t) {
		this.log.debug(new LogMessage(messageSupplier), t);
	}

	/**
	 * Log a message with trace log level.
	 * @param messageSupplier a lazy supplier for the message to log
	 */
	public void trace(Supplier<? extends CharSequence> messageSupplier) {
		this.log.trace(new LogMessage(messageSupplier));
	}

	/**
	 * Log an error with trace log level.
	 * @param messageSupplier a lazy supplier for the message to log
	 * @param t log this cause
	 */
	public void trace(Supplier<? extends CharSequence> messageSupplier, Throwable t) {
		this.log.trace(new LogMessage(messageSupplier), t);
	}

}
