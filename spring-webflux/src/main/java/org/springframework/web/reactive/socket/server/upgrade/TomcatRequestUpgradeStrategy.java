/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.reactive.socket.server.upgrade;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.server.ServerEndpointConfig;
import org.apache.tomcat.websocket.server.WsServerContainer;

/**
 * A WebSocket {@code RequestUpgradeStrategy} for Apache Tomcat. Compatible with Tomcat 10
 * and higher, in particular with Tomcat 10.0 (not based on Jakarta WebSocket 2.1 yet).
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 * @see org.apache.tomcat.websocket.server.WsServerContainer#upgradeHttpToWebSocket
 */
public class TomcatRequestUpgradeStrategy extends StandardWebSocketUpgradeStrategy {

	@Override
	protected void upgradeHttpToWebSocket(HttpServletRequest request, HttpServletResponse response,
			ServerEndpointConfig endpointConfig, Map<String, String> pathParams) throws Exception {

		((WsServerContainer) getContainer(request)).upgradeHttpToWebSocket(
				request, response, endpointConfig, pathParams);
	}

}
