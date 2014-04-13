/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.simp.stomp;


import org.springframework.messaging.Message;

import java.util.List;
import java.util.Map;

/**
 * A factory for creating pre-configured instances of type
 * {@link org.springframework.messaging.simp.stomp.StompHeaderAccessor}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface StompHeaderAccessorFactory {

	/**
	 * Create an instance for the given STOMP command.
	 */
	StompHeaderAccessor create(StompCommand command);

	/**
	 * Create an instance for the given STOMP command and headers.
	 */
	StompHeaderAccessor create(StompCommand command, Map<String, List<String>> headers);

	/**
	 * Create headers for a heartbeat. While a STOMP heartbeat frame does not
	 * have headers, a session id is needed for processing purposes at a minimum.
	 */
	StompHeaderAccessor createForHeartbeat();

	/**
	 * Create an instance from the payload and headers of the given Message.
	 */
	StompHeaderAccessor wrap(Message<?> message);

}
