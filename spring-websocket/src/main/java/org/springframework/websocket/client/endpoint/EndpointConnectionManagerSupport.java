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

package org.springframework.websocket.client.endpoint;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.springframework.websocket.client.AbstractWebSocketConnectionManager;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class EndpointConnectionManagerSupport extends AbstractWebSocketConnectionManager {

	private WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();

	private Session session;


	public EndpointConnectionManagerSupport(String uriTemplate, Object... uriVariables) {
		super(uriTemplate, uriVariables);
	}

	public void setWebSocketContainer(WebSocketContainer webSocketContainer) {
		this.webSocketContainer = webSocketContainer;
	}

	public WebSocketContainer getWebSocketContainer() {
		return this.webSocketContainer;
	}

	protected void updateSession(Session session) {
		this.session = session;
	}

	@Override
	protected void closeConnection() throws Exception {
		try {
			if (isConnected()) {
				this.session.close();
			}
		}
		finally {
			this.session = null;
		}
	}

	@Override
	protected boolean isConnected() {
		return ((this.session != null) && this.session.isOpen());
	}

}
