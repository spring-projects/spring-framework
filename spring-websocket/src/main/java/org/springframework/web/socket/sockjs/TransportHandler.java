/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

/**
 * A contract for SockJS transport implementations. A {@link TransportHandler} is closely
 * related to and often delegates to an {@link AbstractSockJsSession}. In fact most
 * transports are also implementations of {@link SockJsSessionFactory} with the only exception
 * to that being HTTP transports that receive messages as they depend on finding an existing
 * session. See {@link TransportType} for a list of all available transport types.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 *
 * @see SockJsService
 */
public interface TransportHandler {

	TransportType getTransportType();

	void setSockJsConfiguration(SockJsConfiguration sockJsConfig);

	void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler handler, AbstractSockJsSession session) throws TransportErrorException;

}
