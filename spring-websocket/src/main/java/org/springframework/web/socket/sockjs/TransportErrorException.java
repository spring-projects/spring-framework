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
import org.springframework.web.socket.WebSocketHandler;


/**
 * Raised when a TransportHandler fails during request processing.
 * <p>
 * If the underlying exception occurs while sending messages to the client, the session is
 * closed and the {@link WebSocketHandler} notified.
 * <p>
 * If the underlying exception occurs while processing an incoming HTTP request, including
 * over HTTP POST, the session will remain open. Only the incoming request is rejected.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("serial")
public class TransportErrorException extends NestedRuntimeException {

	private final String sockJsSessionId;

	public TransportErrorException(String msg, Throwable cause, String sockJsSessionId) {
		super(msg, cause);
		this.sockJsSessionId = sockJsSessionId;
	}

	public String getSockJsSessionId() {
		return sockJsSessionId;
	}

	@Override
	public String getMessage() {
		return "Transport error for SockJS session id=" + this.sockJsSessionId + ", " + super.getMessage();
	}

}
