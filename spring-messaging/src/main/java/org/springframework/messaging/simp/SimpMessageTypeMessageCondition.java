/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.jspecify.annotations.Nullable;

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
	public @Nullable SimpMessageTypeMessageCondition getMatchingCondition(Message<?> message) {
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
