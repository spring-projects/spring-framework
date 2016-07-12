/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.simp.stomp;

import java.lang.reflect.Type;

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
	 * 
	 * The default implementation returns String as the expected payload type
	 * for STOMP ERROR frames.
	 * 
	 * @param headers the headers of a message
	 */
	default Type getPayloadType(StompHeaders headers) {
		return String.class;
	}

	/**
	 * Handle a STOMP frame with the payload converted to the target type returned
	 * from {@link #getPayloadType(StompHeaders)}.
	 * 
	 * The default implementation is empty.
	 * 
	 * @param headers the headers of the frame
	 * @param payload the payload or {@code null} if there was no payload
	 */
	default void handleFrame(StompHeaders headers, Object payload) {
	}

}
