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
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageHeaderAccessorFactorySupport;
import org.springframework.util.IdGenerator;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A default implementation of
 * {@link org.springframework.messaging.simp.stomp.StompHeaderAccessorFactory}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class DefaultStompHeaderAccessorFactory extends MessageHeaderAccessorFactorySupport
		implements StompHeaderAccessorFactory {


	public DefaultStompHeaderAccessorFactory() {
		super.setIdGenerator(ID_VALUE_NONE_GENERATOR);
	}


	@Override
	public StompHeaderAccessor create(StompCommand command) {
		StompHeaderAccessor accessor = new StompHeaderAccessor(command, null);
		updateMessageHeaderAccessor(accessor);
		return accessor;
	}

	@Override
	public StompHeaderAccessor create(StompCommand command, Map<String, List<String>> headers) {
		StompHeaderAccessor accessor = new StompHeaderAccessor(command, headers);
		updateMessageHeaderAccessor(accessor);
		return accessor;
	}

	@Override
	public StompHeaderAccessor createForHeartbeat() {
		StompHeaderAccessor accessor = new StompHeaderAccessor();
		updateMessageHeaderAccessor(accessor);
		return accessor;
	}

	@Override
	public StompHeaderAccessor wrap(Message<?> message) {
		StompHeaderAccessor accessor = new StompHeaderAccessor(message);
		updateMessageHeaderAccessor(accessor);
		return accessor;
	}

	private static final IdGenerator ID_VALUE_NONE_GENERATOR = new IdGenerator() {
		@Override
		public UUID generateId() {
			return MessageHeaders.ID_VALUE_NONE;
		}
	};
}
