/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.socket.sockjs;

/**
 * Indicates a serious failure that occurred in the SockJS implementation as opposed to
 * in user code (e.g. IOException while writing to the response). When this exception
 * is raised, the SockJS session is typically closed.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("serial")
public class SockJsTransportFailureException extends SockJsException {

	/**
	 * Constructor for SockJsTransportFailureException.
	 * @param message the exception message
	 * @param cause the root cause
	 * @since 4.1.7
	 */
	public SockJsTransportFailureException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor for SockJsTransportFailureException.
	 * @param message the exception message
	 * @param sessionId the SockJS session id
	 * @param cause the root cause
	 */
	public SockJsTransportFailureException(String message, String sessionId, Throwable cause) {
		super(message, sessionId, cause);
	}

}
