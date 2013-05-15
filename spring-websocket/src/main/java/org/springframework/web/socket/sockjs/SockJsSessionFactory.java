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

import org.springframework.web.socket.WebSocketHandler;

/**
 * A factory for creating a SockJS session. {@link TransportHandler}s typically also serve
 * as SockJS session factories.
 *
 * @param <S> The type of session being created
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SockJsSessionFactory {

	/**
	 * Create a new SockJS session.
	 * @param sessionId the ID of the session
	 * @param webSocketHandler the underlying {@link WebSocketHandler}
	 * @return a new non-null session
	 */
	AbstractSockJsSession createSession(String sessionId, WebSocketHandler webSocketHandler);

}
