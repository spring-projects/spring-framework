/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.simp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.AbstractMessageCondition;
import org.springframework.util.Assert;

/**
 * {@code MessageCondition} that matches by the message type obtained via
 * {@link SimpMessageHeaderAccessor#getMessageType(Map)}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpMessageTypeMessageCondition extends AbstractMessageCondition<SimpMessageTypeMessageCondition> {

	public static final SimpMessageTypeMessageCondition MESSAGE =
			new SimpMessageTypeMessageCondition(SimpMessageType.MESSAGE);

	public static final SimpMessageTypeMessageCondition SUBSCRIBE =
			new SimpMessageTypeMessageCondition(SimpMessageType.SUBSCRIBE);


	private final SimpMessageType messageType;


	/**
	 * A constructor accepting a message type.
	 * @param messageType the message type to match messages to
	 */
	public SimpMessageTypeMessageCondition(SimpMessageType messageType) {
		Assert.notNull(messageType, "MessageType must not be null");
		this.messageType = messageType;
	}


	public SimpMessageType getMessageType() {
		return this.messageType;
	}

	@Override
	protected Collection<?> getContent() {
		return Collections.singletonList(this.messageType);
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	@Override
	public SimpMessageTypeMessageCondition combine(SimpMessageTypeMessageCondition other) {
		return other;
	}

	@Override
	@Nullable
	public SimpMessageTypeMessageCondition getMatchingCondition(Message<?> message) {
		SimpMessageType actual = SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
		return (actual != null && actual.equals(this.messageType) ? this : null);
	}

	@Override
	public int compareTo(SimpMessageTypeMessageCondition other, Message<?> message) {
		Object actual = SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
		if (actual != null) {
			if (actual.equals(this.messageType) && actual.equals(other.getMessageType())) {
				return 0;
			}
			else if (actual.equals(this.messageType)) {
				return -1;
			}
			else if (actual.equals(other.getMessageType())) {
				return 1;
			}
		}
		return 0;
	}

}
