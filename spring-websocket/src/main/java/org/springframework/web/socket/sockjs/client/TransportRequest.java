/*
 * Copyright 2002-2015 the original author or authors.
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

import java.net.URI;
import java.security.Principal;

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
	Principal getUser();

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
