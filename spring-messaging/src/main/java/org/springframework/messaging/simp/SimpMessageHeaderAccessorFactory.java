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

package org.springframework.messaging.simp;


import org.springframework.messaging.Message;

/**
 * A factory for creating pre-configured instances of type
 * {@link org.springframework.messaging.simp.SimpMessageHeaderAccessor}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface SimpMessageHeaderAccessorFactory {

	/**
	 * Create an instance with
	 * {@link org.springframework.messaging.simp.SimpMessageType} {@code MESSAGE}.
	 */
	SimpMessageHeaderAccessor create();

	/**
	 * Create an instance with the given
	 * {@link org.springframework.messaging.simp.SimpMessageType}.
	 */
	SimpMessageHeaderAccessor create(SimpMessageType messageType);

	/**
	 * Create an instance from the payload and headers of the given Message.
	 */
	SimpMessageHeaderAccessor wrap(Message<?> message);

}
