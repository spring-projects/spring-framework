/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SimpMessageTypeMessageCondition}.
 *
 * @author Rossen Stoyanchev
 */
class SimpMessageTypeMessageConditionTests {

	@Test
	void combine() {
		SimpMessageType messageType = SimpMessageType.MESSAGE;
		SimpMessageType subscribeType = SimpMessageType.SUBSCRIBE;

		SimpMessageType actual = condition(messageType).combine(condition(subscribeType)).getMessageType();
		assertThat(actual).isEqualTo(subscribeType);

		actual = condition(messageType).combine(condition(messageType)).getMessageType();
		assertThat(actual).isEqualTo(messageType);

		actual = condition(subscribeType).combine(condition(subscribeType)).getMessageType();
		assertThat(actual).isEqualTo(subscribeType);
	}

	@Test
	void getMatchingCondition() {
		Message<?> message = message(SimpMessageType.MESSAGE);
		SimpMessageTypeMessageCondition condition = condition(SimpMessageType.MESSAGE);
		SimpMessageTypeMessageCondition actual = condition.getMatchingCondition(message);

		assertThat(actual).isNotNull();
		assertThat(actual.getMessageType()).isEqualTo(SimpMessageType.MESSAGE);
	}

	@Test
	void getMatchingConditionNoMessageType() {
		Message<?> message = message(null);
		SimpMessageTypeMessageCondition condition = condition(SimpMessageType.MESSAGE);

		assertThat(condition.getMatchingCondition(message)).isNull();
	}

	@Test
	void compareTo() {
		Message<byte[]> message = message(null);
		assertThat(condition(SimpMessageType.MESSAGE).compareTo(condition(SimpMessageType.MESSAGE), message)).isEqualTo(0);
		assertThat(condition(SimpMessageType.MESSAGE).compareTo(condition(SimpMessageType.SUBSCRIBE), message)).isEqualTo(0);
	}

	private Message<byte[]> message(SimpMessageType messageType) {
		MessageBuilder<byte[]> builder = MessageBuilder.withPayload(new byte[0]);
		if (messageType != null) {
			builder.setHeader(SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER, messageType);
		}
		return builder.build();
	}

	private SimpMessageTypeMessageCondition condition(SimpMessageType messageType) {
		return new SimpMessageTypeMessageCondition(messageType);
	}

}
