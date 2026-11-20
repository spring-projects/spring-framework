/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogDelegateFactory;

/**
 * Holds the shared logger named "org.springframework.web.SimpLogging" to use
 * for STOMP over WebSocket messaging when logging for
 * "org.springframework.messaging.simp" is off but logging for
 * "org.springframework.web" is on.
 *
 * <p>This makes it possible to enable all web related logging via
 * "org.springframework.web" including logging from lower-level packages such as
 * "org.springframework.messaging.simp".
 *
 * <p>To see logging from the primary classes where log messages originate from,
 * simply enable logging for "org.springframework.messaging".
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 * @see LogDelegateFactory
 */
public abstract class SimpLogging {

	private static final Log fallbackLogger =
			LogFactory.getLog("org.springframework.web." + SimpLogging.class.getSimpleName());


	/**
	 * Create a primary logger for the given class and wrap it with a composite
	 * that delegates to it or to the fallback logger named
	 * "org.springframework.web.SimpLogging", if the primary is not enabled.
	 * @param primaryLoggerClass the class for the name of the primary logger
	 * @return the resulting composite logger
	 */
	public static Log forLogName(Class<?> primaryLoggerClass) {
		Log primaryLogger = LogFactory.getLog(primaryLoggerClass);
		return forLog(primaryLogger);
	}

	/**
	 * Wrap the given primary logger with a composite logger that delegates to
	 * either the primary or to the shared fallback logger
	 * "org.springframework.web.HttpLogging", if the primary is not enabled.
	 * @param primaryLogger the primary logger to use
	 * @return the resulting composite logger
	 */
	public static Log forLog(Log primaryLogger) {
		return LogDelegateFactory.getCompositeLog(primaryLogger, fallbackLogger);
	}

}
