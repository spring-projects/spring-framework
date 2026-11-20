/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StompSubProtocolErrorHandler}.
 *
 * @author Rossen Stoyanchev
 */
class StompSubProtocolErrorHandlerTests {

	private StompSubProtocolErrorHandler handler;


	@BeforeEach
	void setUp() {
		this.handler = new StompSubProtocolErrorHandler();
	}


	@Test
	void handleClientMessageProcessingError() {

		Exception ex = new Exception("fake exception");
		Message<byte[]> actual = this.handler.handleClientMessageProcessingError(null, ex);

		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(actual, StompHeaderAccessor.class);
		assertThat(accessor).isNotNull();
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.ERROR);
		assertThat(accessor.getMessage()).isEqualTo(ex.getMessage());
		byte[] expecteds = new byte[0];
		assertThat(actual.getPayload()).isEqualTo(expecteds);
	}

	@Test
	void handleClientMessageProcessingErrorWithReceipt() {

		String receiptId = "123";
		StompHeaderAccessor clientHeaderAccessor = StompHeaderAccessor.create(StompCommand.SEND);
		clientHeaderAccessor.setReceipt(receiptId);
		MessageHeaders clientHeaders = clientHeaderAccessor.getMessageHeaders();
		Message<byte[]> clientMessage = MessageBuilder.createMessage(new byte[0], clientHeaders);
		Message<byte[]> actual = this.handler.handleClientMessageProcessingError(clientMessage, new Exception());

		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(actual, StompHeaderAccessor.class);
		assertThat(accessor).isNotNull();
		assertThat(accessor.getReceiptId()).isEqualTo(receiptId);
	}


}
