/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.stomp;

import java.lang.reflect.Type;

import org.jspecify.annotations.Nullable;

/**
 * Contract to handle a STOMP frame.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface StompFrameHandler {

	/**
	 * Invoked before {@link #handleFrame(StompHeaders, Object)} to determine the
	 * type of Object the payload should be converted to.
	 * @param headers the headers of a message
	 */
	Type getPayloadType(StompHeaders headers);

	/**
	 * Handle a STOMP frame with the payload converted to the target type returned
	 * from {@link #getPayloadType(StompHeaders)}.
	 * @param headers the headers of the frame
	 * @param payload the payload, or {@code null} if there was no payload
	 */
	void handleFrame(StompHeaders headers, @Nullable Object payload);

}
