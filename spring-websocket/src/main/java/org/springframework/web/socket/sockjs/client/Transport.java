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

package org.springframework.web.socket.sockjs.client;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.transport.TransportType;

/**
 * A client-side implementation for a SockJS transport.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface Transport {

	/**
	 * Get the SockJS transport types that this transport can be used for.
	 * <p>In particular since from a client perspective there is no difference
	 * between XHR and XHR streaming, an {@code XhrTransport} could do both.
	 */
	List<TransportType> getTransportTypes();

	/**
	 * Connect the transport.
	 * @param request the transport request
	 * @param webSocketHandler the application handler to delegate lifecycle events to
	 * @return a future to indicate success or failure to connect
	 * @deprecated as of 6.0, in favor of {@link #connectAsync(TransportRequest, WebSocketHandler)}
	 */
	@Deprecated(since = "6.0")
	default org.springframework.util.concurrent.ListenableFuture<WebSocketSession> connect(
			TransportRequest request, WebSocketHandler webSocketHandler) {
		return new org.springframework.util.concurrent.CompletableToListenableFutureAdapter<>(
				connectAsync(request, webSocketHandler));
	}

	/**
	 * Connect the transport.
	 * @param request the transport request
	 * @param webSocketHandler the application handler to delegate lifecycle events to
	 * @return a future to indicate success or failure to connect
	 * @since 6.0
	 */
	CompletableFuture<WebSocketSession> connectAsync(TransportRequest request, WebSocketHandler webSocketHandler);

}
