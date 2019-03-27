/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.StubMessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.LinkedMultiValueMap;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link org.springframework.messaging.simp.SimpMessagingTemplate}.
 *
 * @author Rossen Stoyanchev
 */
public class SimpMessagingTemplateTests {

	private SimpMessagingTemplate messagingTemplate;

	private StubMessageChannel messageChannel;


	@Before
	public void setup() {
		this.messageChannel = new StubMessageChannel();
		this.messagingTemplate = new SimpMessagingTemplate(this.messageChannel);
	}


	@Test
	public void convertAndSendToUser() {
		this.messagingTemplate.convertAndSendToUser("joe", "/queue/foo", "data");
		List<Message<byte[]>> messages = this.messageChannel.getMessages();

		assertEquals(1, messages.size());

		Message<byte[]> message = messages.get(0);
		SimpMessageHeaderAccessor headerAccessor =
				MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);

		assertNotNull(headerAccessor);
		assertEquals(SimpMessageType.MESSAGE, headerAccessor.getMessageType());
		assertEquals("/user/joe/queue/foo", headerAccessor.getDestination());
	}

	@Test
	public void convertAndSendToUserWithEncoding() {
		this.messagingTemplate.convertAndSendToUser("https://joe.openid.example.org/", "/queue/foo", "data");
		List<Message<byte[]>> messages = this.messageChannel.getMessages();

		assertEquals(1, messages.size());

		SimpMessageHeaderAccessor headerAccessor =
				MessageHeaderAccessor.getAccessor(messages.get(0), SimpMessageHeaderAccessor.class);

		assertNotNull(headerAccessor);
		assertEquals("/user/https:%2F%2Fjoe.openid.example.org%2F/queue/foo", headerAccessor.getDestination());
	}

	@Test
	public void convertAndSendWithCustomHeader() {
		Map<String, Object> headers = Collections.<String, Object>singletonMap("key", "value");
		this.messagingTemplate.convertAndSend("/foo", "data", headers);

		List<Message<byte[]>> messages = this.messageChannel.getMessages();

		SimpMessageHeaderAccessor headerAccessor =
				MessageHeaderAccessor.getAccessor(messages.get(0), SimpMessageHeaderAccessor.class);

		assertNotNull(headerAccessor);
		assertNull(headerAccessor.toMap().get("key"));
		assertEquals(Arrays.asList("value"), headerAccessor.getNativeHeader("key"));
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

		assertNotNull(headerAccessor);
		assertEquals("value", headerAccessor.toMap().get("key"));
		assertNull(headerAccessor.getNativeHeader("key"));
	}

	// SPR-11868

	@Test
	public void convertAndSendWithCustomDestinationPrefix() {
		this.messagingTemplate.setUserDestinationPrefix("/prefix");
		this.messagingTemplate.convertAndSendToUser("joe", "/queue/foo", "data");
		List<Message<byte[]>> messages = this.messageChannel.getMessages();

		assertEquals(1, messages.size());

		Message<byte[]> message = messages.get(0);
		SimpMessageHeaderAccessor headerAccessor =
				MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);

		assertNotNull(headerAccessor);
		assertEquals(SimpMessageType.MESSAGE, headerAccessor.getMessageType());
		assertEquals("/prefix/joe/queue/foo", headerAccessor.getDestination());
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

		assertSame(headers, message.getHeaders());
		assertFalse(accessor.isMutable());
	}

	@Test
	public void processHeadersToSend() {
		Map<String, Object> map = this.messagingTemplate.processHeadersToSend(null);

		assertNotNull(map);
		assertTrue("Actual: " + map.getClass().toString(), MessageHeaders.class.isAssignableFrom(map.getClass()));

		SimpMessageHeaderAccessor headerAccessor =
				MessageHeaderAccessor.getAccessor((MessageHeaders) map, SimpMessageHeaderAccessor.class);

		assertTrue(headerAccessor.isMutable());
		assertEquals(SimpMessageType.MESSAGE, headerAccessor.getMessageType());
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

		assertSame(message, sentMessage);
		assertFalse(accessor.isMutable());
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
		assertEquals(StompHeaderAccessor.class, sentAccessor.getClass());
		assertEquals("/queue/foo-user123", ((StompHeaderAccessor) sentAccessor).getDestination());
	}

}
