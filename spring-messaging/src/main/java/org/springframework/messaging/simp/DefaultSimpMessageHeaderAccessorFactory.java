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
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageHeaderAccessorFactorySupport;
import org.springframework.util.IdGenerator;

import java.util.UUID;

/**
 * A default implementation of
 * {@link org.springframework.messaging.simp.SimpMessageHeaderAccessorFactory}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class DefaultSimpMessageHeaderAccessorFactory extends MessageHeaderAccessorFactorySupport
		implements SimpMessageHeaderAccessorFactory {


	public DefaultSimpMessageHeaderAccessorFactory() {
		super.setIdGenerator(ID_VALUE_NONE_GENERATOR);
	}


	@Override
	public SimpMessageHeaderAccessor create() {
		SimpMessageHeaderAccessor accessor = new SimpMessageHeaderAccessor(SimpMessageType.MESSAGE, null);
		updateMessageHeaderAccessor(accessor);
		return accessor;
	}

	@Override
	public SimpMessageHeaderAccessor create(SimpMessageType messageType) {
		SimpMessageHeaderAccessor accessor = new SimpMessageHeaderAccessor(messageType, null);
		updateMessageHeaderAccessor(accessor);
		return accessor;
	}

	@Override
	public SimpMessageHeaderAccessor wrap(Message<?> message) {
		SimpMessageHeaderAccessor accessor = new SimpMessageHeaderAccessor(message);
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
