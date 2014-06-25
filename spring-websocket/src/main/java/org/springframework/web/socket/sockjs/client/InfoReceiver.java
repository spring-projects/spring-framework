package org.springframework.web.socket.sockjs.client;

import java.net.URI;

/**
 * A simple contract for executing the SockJS "Info" request before the SockJS
 * session starts. The request is used to check server capabilities such as
 * whether it permits use of the WebSocket transport.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface InfoReceiver {

	/**
	 * Perform an HTTP request to the SockJS "Info" URL.
	 * and return the resulting JSON response content, or raise an exception.
	 *
	 * @param infoUrl the URL to obtain SockJS server information from
	 * @return the body of the response
	 */
	String executeInfoRequest(URI infoUrl);

}