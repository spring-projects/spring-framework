/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.support;

/**
 * Callback interface for initializing a {@link MessageHeaderAccessor}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface MessageHeaderInitializer {

	/**
	 * Initialize the given {@code MessageHeaderAccessor}.
	 * @param headerAccessor the MessageHeaderAccessor to initialize
	 */
	void initHeaders(MessageHeaderAccessor headerAccessor);

}
