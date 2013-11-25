package org.springframework.web.socket.support;

import java.util.List;

/**
 * An interface for WebSocket handlers that support sub-protocols as defined in RFC 6455.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 *
 * @see <a href="http://tools.ietf.org/html/rfc6455#section-1.9">RFC-6455 section 1.9</a>
 */
public interface SubProtocolCapable {

	/**
	 * Return the list of supported sub-protocols.
	 */
	List<String> getSubProtocols();
}
