/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs.client;

import java.net.URI;
import java.security.Principal;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;

/**
 * Exposes information, typically to {@link Transport} and
 * {@link AbstractClientSockJsSession session} implementations, about a request
 * to connect to a SockJS server endpoint over a given transport.
 *
 * <p>Note that a single request to connect via {@link SockJsClient} may result
 * in multiple instances of {@link TransportRequest}, one for each transport
 * before a connection is successfully established.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface TransportRequest {

	/**
	 * Return information about the SockJS URL including server and session ID.
	 */
	SockJsUrlInfo getSockJsUrlInfo();

	/**
	 * Return the headers to send with the connect request.
	 */
	HttpHeaders getHandshakeHeaders();

	/**
	 * Return the headers to add to all other HTTP requests besides the handshake
	 * request such as XHR receive and send requests.
	 * @since 4.2
	 */
	HttpHeaders getHttpRequestHeaders();

	/**
	 * Return the transport URL for the given transport.
	 * <p>For an {@link XhrTransport} this is the URL for receiving messages.
	 */
	URI getTransportUrl();

	/**
	 * Return the user associated with the request, if any.
	 */
	@Nullable Principal getUser();

	/**
	 * Return the message codec to use for encoding SockJS messages.
	 */
	SockJsMessageCodec getMessageCodec();

	/**
	 * Register a timeout cleanup task to invoke if the SockJS session is not
	 * fully established within the calculated retransmission timeout period.
	 */
	void addTimeoutTask(Runnable runnable);

}
