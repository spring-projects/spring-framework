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

package org.springframework.websocket.server.support;

import java.io.IOException;

import javax.websocket.Endpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.adapter.StandardEndpointAdapter;
import org.springframework.websocket.server.RequestUpgradeStrategy;

/**
 * A {@link RequestUpgradeStrategy} that supports WebSocket handlers of type
 * {@link WebSocketHandler} as well as {@link javax.websocket.Endpoint}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractEndpointUpgradeStrategy implements RequestUpgradeStrategy {

	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public void upgrade(ServerHttpRequest request, ServerHttpResponse response,
			String protocol, WebSocketHandler<?> webSocketHandler) throws IOException {

		upgradeInternal(request, response, protocol, new StandardEndpointAdapter(webSocketHandler));
	}

	protected abstract void upgradeInternal(ServerHttpRequest request, ServerHttpResponse response,
			String selectedProtocol, Endpoint endpoint) throws IOException;

}
