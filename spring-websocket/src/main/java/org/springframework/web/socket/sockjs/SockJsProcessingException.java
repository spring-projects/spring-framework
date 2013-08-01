/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * Raised when SockJS request handling fails.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("serial")
public class SockJsProcessingException extends NestedRuntimeException {

	private final String sessionId;


	public SockJsProcessingException(String msg, Throwable cause, String sessionId) {
		super(msg, cause);
		this.sessionId = sessionId;
	}

	public String getSockJsSessionId() {
		return this.sessionId;
	}

	@Override
	public String getMessage() {
		return "Transport error for SockJS session id=" + this.sessionId + ", " + super.getMessage();
	}

}
