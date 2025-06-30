/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.socket.sockjs.client;

import java.io.IOException;

import org.springframework.web.socket.UndertowTestServer;
import org.springframework.web.socket.WebSocketTestServer;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

/**
 * @author Brian Clozel
 */
class UndertowSockJsIntegrationTests extends AbstractSockJsIntegrationTests {

	@Override
	protected WebSocketTestServer createWebSocketTestServer() {
		return new UndertowTestServer();
	}

	@Override
	protected Transport createWebSocketTransport() {
		return new WebSocketTransport(new StandardWebSocketClient());
	}

	@Override
	protected AbstractXhrTransport createXhrTransport() {
		try {
			return new UndertowXhrTransport();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not create UndertowXhrTransport");
		}
	}

}
