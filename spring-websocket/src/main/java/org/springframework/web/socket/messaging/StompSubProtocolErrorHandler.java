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

package org.springframework.web.socket.messaging;

import org.springframework.lang.Nullable;
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

	private static byte[] EMPTY_PAYLOAD = new byte[0];


	@Override
	@Nullable
	public Message<byte[]> handleClientMessageProcessingError(@Nullable Message<byte[]> clientMessage, Throwable ex) {
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
	@Nullable
	public Message<byte[]> handleErrorMessageToClient(Message<byte[]> errorMessage) {
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
