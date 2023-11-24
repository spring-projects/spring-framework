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

package org.springframework.web.socket.server.standard;

import java.util.Map;

import io.undertow.websockets.jsr.ServerWebSocketContainer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * A WebSocket {@code RequestUpgradeStrategy} for WildFly and its underlying
 * Undertow web server. Also compatible with embedded Undertow usage.
 *
 * <p>Designed for Undertow 2.2, also compatible with Undertow 2.3
 * (which implements Jakarta WebSocket 2.1 as well).
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0.1
 * @see io.undertow.websockets.jsr.ServerWebSocketContainer#doUpgrade
 */
public class UndertowRequestUpgradeStrategy extends StandardWebSocketUpgradeStrategy {

	private static final String[] SUPPORTED_VERSIONS = new String[] {"13", "8", "7"};


	@Override
	public String[] getSupportedVersions() {
		return SUPPORTED_VERSIONS;
	}

	@Override
	protected void upgradeHttpToWebSocket(HttpServletRequest request, HttpServletResponse response,
			ServerEndpointConfig endpointConfig, Map<String, String> pathParams) throws Exception {

		((ServerWebSocketContainer) getContainer(request)).doUpgrade(
				request, response, endpointConfig, pathParams);
	}

}
