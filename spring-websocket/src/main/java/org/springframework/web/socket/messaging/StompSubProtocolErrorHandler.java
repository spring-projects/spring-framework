/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.messaging;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;

/**
 * A {@link SubProtocolErrorHandler} for use with STOMP.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class StompSubProtocolErrorHandler implements SubProtocolErrorHandler<byte[]> {

	private static final byte[] EMPTY_PAYLOAD = new byte[0];


	@Override
	public @Nullable Message<byte[]> handleClientMessageProcessingError(@Nullable Message<byte[]> clientMessage, Throwable ex) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
		accessor.setMessage(ex.getMessage());
		accessor.setLeaveMutable(true);

		StompHeaderAccessor clientHeaderAccessor = null;
		if (clientMessage != null) {
			clientHeaderAccessor = MessageHeaderAccessor.getAccessor(clientMessage, StompHeaderAccessor.class);
			if (clientHeaderAccessor != null) {
				String receiptId = clientHeaderAccessor.getReceipt();
				if (receiptId != null) {
					accessor.setReceiptId(receiptId);
				}
			}
		}

		return handleInternal(accessor, EMPTY_PAYLOAD, ex, clientHeaderAccessor);
	}

	@Override
	public @Nullable Message<byte[]> handleErrorMessageToClient(Message<byte[]> errorMessage) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(errorMessage, StompHeaderAccessor.class);
		Assert.notNull(accessor, "No StompHeaderAccessor");
		if (!accessor.isMutable()) {
			accessor = StompHeaderAccessor.wrap(errorMessage);
		}
		return handleInternal(accessor, errorMessage.getPayload(), null, null);
	}

	protected Message<byte[]> handleInternal(StompHeaderAccessor errorHeaderAccessor, byte[] errorPayload,
			@Nullable Throwable cause, @Nullable StompHeaderAccessor clientHeaderAccessor) {

		return MessageBuilder.createMessage(errorPayload, errorHeaderAccessor.getMessageHeaders());
	}

}
