/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket;

import java.util.List;

/**
 * An interface for WebSocket handlers that support sub-protocols as defined in RFC 6455.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see WebSocketHandler
 * @see <a href="https://tools.ietf.org/html/rfc6455#section-1.9">RFC-6455 section 1.9</a>
 */
public interface SubProtocolCapable {

	/**
	 * Return the list of supported sub-protocols.
	 */
	List<String> getSubProtocols();

}
