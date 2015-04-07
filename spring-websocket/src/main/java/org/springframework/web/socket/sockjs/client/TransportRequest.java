package org.springframework.web.socket.sockjs.client;

import java.net.URI;
import java.security.Principal;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;

/**
 * Represents a request to connect to a SockJS service using a specific
 * Transport. A single SockJS request however may require falling back
 * and therefore multiple TransportRequest instances.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface TransportRequest {

	/**
	 * Return information about the SockJS URL including server and session id..
	 */
	SockJsUrlInfo getSockJsUrlInfo();

	/**
	 * Return the headers to send with the connect request.
	 */
	HttpHeaders getHandshakeHeaders();

	/**
	 * Return the transport URL for the given transport.
	 * For an {@link XhrTransport} this is the URL for receiving messages.
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
