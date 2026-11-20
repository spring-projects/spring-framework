/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs.client;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.TextMessage;

/**
 * A SockJS {@link Transport} that uses HTTP requests to simulate a WebSocket
 * interaction. The {@code connect} method of the base {@code Transport} interface
 * is used to receive messages from the server while the
 * {@link #executeSendRequest} method here is used to send messages.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface XhrTransport extends Transport, InfoReceiver {

	/**
	 * An {@code XhrTransport} supports both the "xhr_streaming" and "xhr" SockJS
	 * server transports. From a client perspective there is no implementation
	 * difference.
	 * <p>By default an {@code XhrTransport} will be used with "xhr_streaming"
	 * first and then with "xhr", if the streaming fails to connect. In some
	 * cases it may be useful to suppress streaming so that only "xhr" is used.
	 */
	boolean isXhrStreamingDisabled();

	/**
	 * Execute a request to send the message to the server.
	 * <p>Note that as of 4.2 this method accepts a {@code headers} parameter.
	 * @param transportUrl the URL for sending messages.
	 * @param message the message to send
	 */
	void executeSendRequest(URI transportUrl, HttpHeaders headers, TextMessage message);

}
