/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.util;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.util.Assert;

/**
 * Utility methods to assist with identifying and logging exceptions that indicate
 * the client has gone away. Such exceptions fill logs with unnecessary stack
 * traces. The utility methods help to log a single line message at DEBUG level,
 * and a full stacktrace at TRACE level.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
public class DisconnectedClientHelper {

	private static final Set<String> EXCEPTION_PHRASES =
			Set.of("broken pipe", "connection reset");

	private static final Set<String> EXCEPTION_TYPE_NAMES =
			Set.of("AbortedException", "ClientAbortException",
					"EOFException", "EofException", "AsyncRequestNotUsableException");

	private final Log logger;


	public DisconnectedClientHelper(String logCategory) {
		Assert.notNull(logCategory, "'logCategory' is required");
		this.logger = LogFactory.getLog(logCategory);
	}


	/**
	 * Check via  {@link #isClientDisconnectedException} if the exception
	 * indicates the remote client disconnected, and if so log a single line
	 * message when DEBUG is on, and a full stacktrace when TRACE is on for
	 * the configured logger.
 	 */
	public boolean checkAndLogClientDisconnectedException(Throwable ex) {
		if (isClientDisconnectedException(ex)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Looks like the client has gone away", ex);
			}
			else if (logger.isDebugEnabled()) {
				logger.debug("Looks like the client has gone away: " + ex +
						" (For a full stack trace, set the log category '" + logger + "' to TRACE level.)");
			}
			return true;
		}
		return false;
	}

	/**
	 * Whether the given exception indicates the client has gone away.
	 * <p>Known cases covered:
	 * <ul>
	 * <li>ClientAbortException or EOFException for Tomcat
	 * <li>EofException for Jetty
	 * <li>IOException "Broken pipe" or "connection reset by peer"
	 * <li>SocketException "Connection reset"
	 * </ul>
	 */
	public static boolean isClientDisconnectedException(Throwable ex) {
		String message = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
		if (message != null) {
			String text = message.toLowerCase();
			for (String phrase : EXCEPTION_PHRASES) {
				if (text.contains(phrase)) {
					return true;
				}
			}
		}
		return EXCEPTION_TYPE_NAMES.contains(ex.getClass().getSimpleName());
	}

}
