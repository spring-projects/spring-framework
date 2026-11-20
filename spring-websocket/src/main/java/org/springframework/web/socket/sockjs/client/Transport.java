/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	 * @since 6.0
	 */
	CompletableFuture<WebSocketSession> connectAsync(TransportRequest request, WebSocketHandler webSocketHandler);

}
