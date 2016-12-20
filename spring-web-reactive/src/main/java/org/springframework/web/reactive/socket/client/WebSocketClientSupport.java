/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.web.reactive.socket.client;

import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.WebSocketHandler;

/**
 * Base class for {@link WebSocketClient} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class WebSocketClientSupport {

	protected static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";


	protected String[] getSubProtocols(HttpHeaders headers, WebSocketHandler handler) {
		String value = headers.getFirst(SEC_WEBSOCKET_PROTOCOL);
		return (value != null ?
				StringUtils.commaDelimitedListToStringArray(value) :
				handler.getSubProtocols());
	}

}