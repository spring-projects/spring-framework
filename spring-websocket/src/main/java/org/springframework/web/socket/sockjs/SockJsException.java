/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.socket.sockjs;

import org.springframework.core.NestedRuntimeException;

/**
 * Base class for exceptions raised while processing SockJS HTTP requests.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("serial")
public class SockJsException extends NestedRuntimeException {

	private final String sessionId;


	/**
	 * Constructor for SockJsException.
	 * @param message the exception message
	 * @param cause the root cause
	 */
	public SockJsException(String message, Throwable cause) {
		this(message, null, cause);
	}

	/**
	 * Constructor for SockJsException.
	 * @param message the exception message
	 * @param sessionId the SockJS session id
	 * @param cause the root cause
	 */
	public SockJsException(String message, String sessionId, Throwable cause) {
		super(message, cause);
		this.sessionId = sessionId;
	}


	/**
	 * Return the SockJS session id.
	 */
	public String getSockJsSessionId() {
		return this.sessionId;
	}

}
