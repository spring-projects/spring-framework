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

package org.springframework.sockjs;

import org.springframework.websocket.CloseStatus;



/**
 * A handler for SockJS messages.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SockJsHandler {

	/**
	 * A new connection was opened and is ready for use.
	 */
	void afterConnectionEstablished(SockJsSession session) throws Exception;

	/**
	 * Handle an incoming message.
	 */
	void handleMessage(String message, SockJsSession session) throws Exception;

	/**
	 * TODO
	 */
	void handleError(Throwable exception, SockJsSession session);

	/**
	 * A connection has been closed.
	 */
	void afterConnectionClosed(CloseStatus status, SockJsSession session);

}
