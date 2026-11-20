/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.config.annotation;

import org.springframework.web.socket.WebSocketHandler;

/**
 * Provides methods for configuring {@link WebSocketHandler} request mappings.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface WebSocketHandlerRegistry {

	/**
	 * Configure a WebSocketHandler at the specified URL paths.
	 */
	WebSocketHandlerRegistration addHandler(WebSocketHandler webSocketHandler, String... paths);

}
