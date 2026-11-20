/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * A simpler interceptor that calls {@link MessageHeaderAccessor#setImmutable()}
 * on the headers of messages passed through the preSend method.
 *
 * <p>When configured as the last interceptor in a chain, it allows the component
 * sending the message to leave headers mutable for interceptors to modify prior
 * to the message actually being sent and exposed to concurrent access.
 *
 * @author Rossen Stoyanchev
 * @since 4.1.2
 */
public class ImmutableMessageChannelInterceptor implements ChannelInterceptor {

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
		if (accessor != null && accessor.isMutable()) {
			accessor.setImmutable();
		}
		return message;
	}

}
