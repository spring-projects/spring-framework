/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.simp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.StubMessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link org.springframework.messaging.simp.SimpMessagingTemplate}.
 *
 * @author Rossen Stoyanchev
 */
public class SimpMessagingTemplateTests {

	private SimpMessagingTemplate messagingTemplate;

	private StubMessageChannel messageChannel;


	@BeforeEach
	public void setup() {
		this.messageChannel = new StubMessageChannel();
		this.messagingTemplate = new SimpMessagingTemplate(this.messageChannel);
	}


	@Test
	public void convertAndSendToUser() {
		this.messagingTemplate.convertAndSendToUser("joe", "/queue/foo", "data");
		List<Message<byte[]>> messages = this.messageChannel.getMessages();

		assertThat(messages).hasSize(1);

		Message<byte[]> message = messages.get(0);
		SimpMessageHeaderAccessor headerAccessor =
				MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);

		assertThat(headerAccessor).isNotNull();
		assertThat(headerAccessor.getMessageType()).isEqualTo(SimpMessageType.MESSAGE);
		assertThat(headerAccessor.getDestination()).isEqualTo("/user/joe/queue/foo");
	}

	@Test
	public void convertAndSendToUserWithEncoding() {
		this.messagingTemplate.convertAndSendToUser("https://joe.openid.example.org/", "/queue/foo", "data");
		List<Message<byte[]>> messages = this.messageChannel.getMessages();

		assertThat(messages).hasSize(1);

		SimpMessageHeaderAccessor headerAccessor =
				MessageHeaderAccessor.getAccessor(messages.get(0), SimpMessageHeaderAccessor.class);

		assertThat(headerAccessor).isNotNull();
		assertThat(headerAccessor.getDestination()).isEqualTo("/user/https:%2F%2Fjoe.openid.example.org%2F/queue/foo");
	}

	@Test // gh-23836
	public void convertAndSendToUserWithInvalidSequence() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.messagingTemplate.convertAndSendToUser("joe%2F", "/queue/foo", "data"));
	}

	@Test
	public void convertAndSendWithCustomHeader() {
		Map<String, Object> headers = Collections.<String, Object>singletonMap("key", "value");
		this.messagingTemplate.convertAndSend("/foo", "data", headers);

		List<Message<byte[]>> messages = this.messageChannel.getMessages();

		SimpMessageHeaderAccessor headerAccessor =
				MessageHeaderAccessor.getAccessor(messages.get(0), SimpMessageHeaderAccessor.class);

		assertThat(headerAccessor).isNotNull();
		assertThat(headerAccessor.toMap().get("key")).isNull();
		assertThat(headerAccessor.getNativeHeader("key")).isEqualTo(Arrays.asList("value"));
	}

	@Test
	public void convertAndSendWithCustomHeaderNonNative() {
		Map<String, Object> headers = new HashMap<>();
		headers.put("key", "value");
		headers.put(NativeMessageHeaderAccessor.NATIVE_HEADERS, new LinkedMultiValueMap<String, String>());
		this.messagingTemplate.convertAndSend("/foo", "data", headers);

		List<Message<byte[]>> messages = this.messageChannel.getMessages();

		SimpMessageHeaderAccessor headerAccessor =
				MessageHeaderAccessor.getAccessor(messages.get(0), SimpMessageHeaderAccessor.class);

		assertThat(headerAccessor).isNotNull();
		assertThat(headerAccessor.toMap().get("key")).isEqualTo("value");
		assertThat(headerAccessor.getNativeHeader("key")).isNull();
	}

	// SPR-11868

	@Test
	public void convertAndSendWithCustomDestinationPrefix() {
		this.messagingTemplate.setUserDestinationPrefix("/prefix");
		this.messagingTemplate.convertAndSendToUser("joe", "/queue/foo", "data");
		List<Message<byte[]>> messages = this.messageChannel.getMessages();

		assertThat(messages).hasSize(1);

		Message<byte[]> message = messages.get(0);
		SimpMessageHeaderAccessor headerAccessor =
				MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);

		assertThat(headerAccessor).isNotNull();
		assertThat(headerAccessor.getMessageType()).isEqualTo(SimpMessageType.MESSAGE);
		assertThat(headerAccessor.getDestination()).isEqualTo("/prefix/joe/queue/foo");
	}

	@Test
	public void convertAndSendWithMutableSimpMessageHeaders() {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
		accessor.setHeader("key", "value");
		accessor.setNativeHeader("fooNative", "barNative");
		accessor.setLeaveMutable(true);
		MessageHeaders headers = accessor.getMessageHeaders();

		this.messagingTemplate.convertAndSend("/foo", "data", headers);

		List<Message<byte[]>> messages = this.messageChannel.getMessages();
		Message<byte[]> message = messages.get(0);

		assertThat(message.getHeaders()).isSameAs(headers);
		assertThat(accessor.isMutable()).isFalse();
	}

	@Test
	public void processHeadersToSend() {
		Map<String, Object> map = this.messagingTemplate.processHeadersToSend(null);

		assertThat(map).isNotNull();
		assertThat(MessageHeaders.class.isAssignableFrom(map.getClass())).as("Actual: " + map.getClass().toString()).isTrue();

		SimpMessageHeaderAccessor headerAccessor =
				MessageHeaderAccessor.getAccessor((MessageHeaders) map, SimpMessageHeaderAccessor.class);

		assertThat(headerAccessor.isMutable()).isTrue();
		assertThat(headerAccessor.getMessageType()).isEqualTo(SimpMessageType.MESSAGE);
	}

	@Test
	public void doSendWithMutableHeaders() {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
		accessor.setHeader("key", "value");
		accessor.setNativeHeader("fooNative", "barNative");
		accessor.setLeaveMutable(true);
		MessageHeaders headers = accessor.getMessageHeaders();
		Message<?> message = MessageBuilder.createMessage("payload", headers);

		this.messagingTemplate.doSend("/topic/foo", message);

		List<Message<byte[]>> messages = this.messageChannel.getMessages();
		Message<byte[]> sentMessage = messages.get(0);

		assertThat(sentMessage).isSameAs(message);
		assertThat(accessor.isMutable()).isFalse();
	}

	@Test
	public void doSendWithStompHeaders() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		accessor.setDestination("/user/queue/foo");
		Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

		this.messagingTemplate.doSend("/queue/foo-user123", message);

		List<Message<byte[]>> messages = this.messageChannel.getMessages();
		Message<byte[]> sentMessage = messages.get(0);

		MessageHeaderAccessor sentAccessor = MessageHeaderAccessor.getAccessor(sentMessage, MessageHeaderAccessor.class);
		assertThat(sentAccessor.getClass()).isEqualTo(StompHeaderAccessor.class);
		assertThat(((StompHeaderAccessor) sentAccessor).getDestination()).isEqualTo("/queue/foo-user123");
	}

}
