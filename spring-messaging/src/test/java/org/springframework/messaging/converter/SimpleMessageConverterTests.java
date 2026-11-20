/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.converter;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageHeaderAccessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SimpleMessageConverter}.
 *
 * @author Rossen Stoyanchev
 */
class SimpleMessageConverterTests {

	private final SimpleMessageConverter converter = new SimpleMessageConverter();


	@Test
	void toMessageWithPayloadAndHeaders() {
		MessageHeaders headers = new MessageHeaders(Collections.singletonMap("foo", "bar"));
		Message<?> message = this.converter.toMessage("payload", headers);

		assertThat(message.getPayload()).isEqualTo("payload");
		assertThat(message.getHeaders().get("foo")).isEqualTo("bar");
	}

	@Test
	void toMessageWithPayloadAndMutableHeaders() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setHeader("foo", "bar");
		accessor.setLeaveMutable(true);
		MessageHeaders headers = accessor.getMessageHeaders();

		Message<?> message = this.converter.toMessage("payload", headers);

		assertThat(message.getPayload()).isEqualTo("payload");
		assertThat(message.getHeaders()).isSameAs(headers);
		assertThat(message.getHeaders().get("foo")).isEqualTo("bar");
	}
}
