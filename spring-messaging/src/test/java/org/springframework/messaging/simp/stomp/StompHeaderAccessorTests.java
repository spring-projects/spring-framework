/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.simp.stomp;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.IdTimestampMessageHeaderInitializer;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link StompHeaderAccessor}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompHeaderAccessorTests {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	@Test
	public void createWithCommand() {

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
		assertEquals(StompCommand.CONNECTED, accessor.getCommand());

		accessor = StompHeaderAccessor.create(StompCommand.CONNECTED, new LinkedMultiValueMap<String, String>());
		assertEquals(StompCommand.CONNECTED, accessor.getCommand());
	}

	@Test
	public void createWithSubscribeNativeHeaders() {

		MultiValueMap<String, String> extHeaders = new LinkedMultiValueMap<>();
		extHeaders.add(StompHeaderAccessor.STOMP_ID_HEADER, "s1");
		extHeaders.add(StompHeaderAccessor.STOMP_DESTINATION_HEADER, "/d");

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE, extHeaders);

		assertEquals(StompCommand.SUBSCRIBE, headers.getCommand());
		assertEquals(SimpMessageType.SUBSCRIBE, headers.getMessageType());
		assertEquals("/d", headers.getDestination());
		assertEquals("s1", headers.getSubscriptionId());
	}

	@Test
	public void createWithUnubscribeNativeHeaders() {

		MultiValueMap<String, String> extHeaders = new LinkedMultiValueMap<>();
		extHeaders.add(StompHeaderAccessor.STOMP_ID_HEADER, "s1");

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE, extHeaders);

		assertEquals(StompCommand.UNSUBSCRIBE, headers.getCommand());
		assertEquals(SimpMessageType.UNSUBSCRIBE, headers.getMessageType());
		assertEquals("s1", headers.getSubscriptionId());
	}

	@Test
	public void createWithMessageFrameNativeHeaders() {

		MultiValueMap<String, String> extHeaders = new LinkedMultiValueMap<>();
		extHeaders.add(StompHeaderAccessor.DESTINATION_HEADER, "/d");
		extHeaders.add(StompHeaderAccessor.STOMP_SUBSCRIPTION_HEADER, "s1");
		extHeaders.add(StompHeaderAccessor.STOMP_CONTENT_TYPE_HEADER, "application/json");

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.MESSAGE, extHeaders);

		assertEquals(StompCommand.MESSAGE, headers.getCommand());
		assertEquals(SimpMessageType.MESSAGE, headers.getMessageType());
		assertEquals("s1", headers.getSubscriptionId());
	}

	@Test
	public void createWithConnectNativeHeaders() {

		MultiValueMap<String, String> extHeaders = new LinkedMultiValueMap<>();
		extHeaders.add(StompHeaderAccessor.STOMP_LOGIN_HEADER, "joe");
		extHeaders.add(StompHeaderAccessor.STOMP_PASSCODE_HEADER, "joe123");

		StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.CONNECT, extHeaders);

		assertEquals(StompCommand.CONNECT, headerAccessor.getCommand());
		assertEquals(SimpMessageType.CONNECT, headerAccessor.getMessageType());
		assertNotNull(headerAccessor.getHeader("stompCredentials"));
		assertEquals("joe", headerAccessor.getLogin());
		assertEquals("joe123", headerAccessor.getPasscode());
		assertThat(headerAccessor.toString(), CoreMatchers.containsString("passcode=[PROTECTED]"));

		Map<String, List<String>> output = headerAccessor.toNativeHeaderMap();
		assertEquals("joe", output.get(StompHeaderAccessor.STOMP_LOGIN_HEADER).get(0));
		assertEquals("PROTECTED", output.get(StompHeaderAccessor.STOMP_PASSCODE_HEADER).get(0));
	}

	@Test
	public void toNativeHeadersSubscribe() {

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		headers.setSubscriptionId("s1");
		headers.setDestination("/d");

		Map<String, List<String>> actual = headers.toNativeHeaderMap();

		assertEquals(2, actual.size());
		assertEquals("s1", actual.get(StompHeaderAccessor.STOMP_ID_HEADER).get(0));
		assertEquals("/d", actual.get(StompHeaderAccessor.STOMP_DESTINATION_HEADER).get(0));
	}

	@Test
	public void toNativeHeadersUnsubscribe() {

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
		headers.setSubscriptionId("s1");

		Map<String, List<String>> actual = headers.toNativeHeaderMap();

		assertEquals(1, actual.size());
		assertEquals("s1", actual.get(StompHeaderAccessor.STOMP_ID_HEADER).get(0));
	}

	@Test
	public void toNativeHeadersMessageFrame() {

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.MESSAGE);
		headers.setSubscriptionId("s1");
		headers.setDestination("/d");
		headers.setContentType(MimeTypeUtils.APPLICATION_JSON);
		headers.updateStompCommandAsServerMessage();

		Map<String, List<String>> actual = headers.toNativeHeaderMap();

		assertEquals(actual.toString(), 4, actual.size());
		assertEquals("s1", actual.get(StompHeaderAccessor.STOMP_SUBSCRIPTION_HEADER).get(0));
		assertEquals("/d", actual.get(StompHeaderAccessor.STOMP_DESTINATION_HEADER).get(0));
		assertEquals("application/json", actual.get(StompHeaderAccessor.STOMP_CONTENT_TYPE_HEADER).get(0));
		assertNotNull("message-id was not created", actual.get(StompHeaderAccessor.STOMP_MESSAGE_ID_HEADER).get(0));
	}

	@Test
	public void toNativeHeadersContentType() {

		SimpMessageHeaderAccessor simpHeaderAccessor = SimpMessageHeaderAccessor.create();
		simpHeaderAccessor.setContentType(MimeTypeUtils.APPLICATION_ATOM_XML);
		Message<byte[]> message = MessageBuilder.createMessage(new byte[0], simpHeaderAccessor.getMessageHeaders());

		StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.wrap(message);
		Map<String, List<String>> map = stompHeaderAccessor.toNativeHeaderMap();

		assertEquals("application/atom+xml", map.get(StompHeaderAccessor.STOMP_CONTENT_TYPE_HEADER).get(0));
	}

	@Test
	public void encodeConnectWithLoginAndPasscode() throws UnsupportedEncodingException {

		MultiValueMap<String, String> extHeaders = new LinkedMultiValueMap<>();
		extHeaders.add(StompHeaderAccessor.STOMP_LOGIN_HEADER, "joe");
		extHeaders.add(StompHeaderAccessor.STOMP_PASSCODE_HEADER, "joe123");

		StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.CONNECT, extHeaders);
		Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());
		byte[] bytes = new StompEncoder().encode(message);

		assertEquals("CONNECT\nlogin:joe\npasscode:joe123\n\n\0", new String(bytes, "UTF-8"));
	}

	@Test
	public void modifyCustomNativeHeader() {

		MultiValueMap<String, String> extHeaders = new LinkedMultiValueMap<>();
		extHeaders.add(StompHeaderAccessor.STOMP_ID_HEADER, "s1");
		extHeaders.add(StompHeaderAccessor.STOMP_DESTINATION_HEADER, "/d");
		extHeaders.add("accountId", "ABC123");

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE, extHeaders);
		String accountId = headers.getFirstNativeHeader("accountId");
		headers.setNativeHeader("accountId", accountId.toLowerCase());

		Map<String, List<String>> actual = headers.toNativeHeaderMap();
		assertEquals(3, actual.size());

		assertEquals("s1", actual.get(StompHeaderAccessor.STOMP_ID_HEADER).get(0));
		assertEquals("/d", actual.get(StompHeaderAccessor.STOMP_DESTINATION_HEADER).get(0));
		assertNotNull("abc123", actual.get("accountId").get(0));
	}

	@Test
	public void messageIdAndTimestampDefaultBehavior() {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.SEND);
		MessageHeaders headers = headerAccessor.getMessageHeaders();

		assertNull(headers.getId());
		assertNull(headers.getTimestamp());
	}

	@Test
	public void messageIdAndTimestampEnabled() {
		IdTimestampMessageHeaderInitializer headerInitializer = new IdTimestampMessageHeaderInitializer();
		headerInitializer.setIdGenerator(new AlternativeJdkIdGenerator());
		headerInitializer.setEnableTimestamp(true);

		StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.SEND);
		headerInitializer.initHeaders(headerAccessor);

		assertNotNull(headerAccessor.getMessageHeaders().getId());
		assertNotNull(headerAccessor.getMessageHeaders().getTimestamp());
	}

	@Test
	public void getAccessor() {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());

		assertSame(headerAccessor, MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class));
	}

	@Test
	public void getShortLogMessage() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
		accessor.setDestination("/foo");
		accessor.setContentType(MimeTypeUtils.APPLICATION_JSON);
		accessor.setSessionId("123");
		String actual = accessor.getShortLogMessage("payload".getBytes(Charset.forName("UTF-8")));
		assertEquals("SEND /foo session=123 application/json payload=payload", actual);

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 80; i++) {
			sb.append("a");
		}
		final String payload = sb.toString() + " > 80";
		actual = accessor.getShortLogMessage(payload.getBytes(UTF_8));
		assertEquals("SEND /foo session=123 application/json payload=" + sb + "...(truncated)", actual);
	}

}
