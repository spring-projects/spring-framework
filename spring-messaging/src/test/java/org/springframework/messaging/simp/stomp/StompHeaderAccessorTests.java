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

package org.springframework.messaging.simp.stomp;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.IdTimestampMessageHeaderInitializer;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StompHeaderAccessor}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompHeaderAccessorTests {

	@Test
	public void createWithCommand() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.CONNECTED);

		accessor = StompHeaderAccessor.create(StompCommand.CONNECTED, new LinkedMultiValueMap<>());
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.CONNECTED);
	}

	@Test
	public void createWithSubscribeNativeHeaders() {
		MultiValueMap<String, String> extHeaders = new LinkedMultiValueMap<>();
		extHeaders.add(StompHeaderAccessor.STOMP_ID_HEADER, "s1");
		extHeaders.add(StompHeaderAccessor.STOMP_DESTINATION_HEADER, "/d");

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE, extHeaders);

		assertThat(headers.getCommand()).isEqualTo(StompCommand.SUBSCRIBE);
		assertThat(headers.getMessageType()).isEqualTo(SimpMessageType.SUBSCRIBE);
		assertThat(headers.getDestination()).isEqualTo("/d");
		assertThat(headers.getSubscriptionId()).isEqualTo("s1");
	}

	@Test
	public void createWithUnubscribeNativeHeaders() {
		MultiValueMap<String, String> extHeaders = new LinkedMultiValueMap<>();
		extHeaders.add(StompHeaderAccessor.STOMP_ID_HEADER, "s1");

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE, extHeaders);

		assertThat(headers.getCommand()).isEqualTo(StompCommand.UNSUBSCRIBE);
		assertThat(headers.getMessageType()).isEqualTo(SimpMessageType.UNSUBSCRIBE);
		assertThat(headers.getSubscriptionId()).isEqualTo("s1");
	}

	@Test
	public void createWithMessageFrameNativeHeaders() {
		MultiValueMap<String, String> extHeaders = new LinkedMultiValueMap<>();
		extHeaders.add(StompHeaderAccessor.DESTINATION_HEADER, "/d");
		extHeaders.add(StompHeaderAccessor.STOMP_SUBSCRIPTION_HEADER, "s1");
		extHeaders.add(StompHeaderAccessor.STOMP_CONTENT_TYPE_HEADER, "application/json");

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.MESSAGE, extHeaders);

		assertThat(headers.getCommand()).isEqualTo(StompCommand.MESSAGE);
		assertThat(headers.getMessageType()).isEqualTo(SimpMessageType.MESSAGE);
		assertThat(headers.getSubscriptionId()).isEqualTo("s1");
	}

	@Test
	public void createWithConnectNativeHeaders() {
		MultiValueMap<String, String> extHeaders = new LinkedMultiValueMap<>();
		extHeaders.add(StompHeaderAccessor.STOMP_LOGIN_HEADER, "joe");
		extHeaders.add(StompHeaderAccessor.STOMP_PASSCODE_HEADER, "joe123");

		StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.STOMP, extHeaders);

		assertThat(headerAccessor.getCommand()).isEqualTo(StompCommand.STOMP);
		assertThat(headerAccessor.getMessageType()).isEqualTo(SimpMessageType.CONNECT);
		assertThat(headerAccessor.getHeader("stompCredentials")).isNotNull();
		assertThat(headerAccessor.getLogin()).isEqualTo("joe");
		assertThat(headerAccessor.getPasscode()).isEqualTo("joe123");
		assertThat(headerAccessor.toString()).contains("passcode=[PROTECTED]");

		Map<String, List<String>> output = headerAccessor.toNativeHeaderMap();
		assertThat(output.get(StompHeaderAccessor.STOMP_LOGIN_HEADER).get(0)).isEqualTo("joe");
		assertThat(output.get(StompHeaderAccessor.STOMP_PASSCODE_HEADER).get(0)).isEqualTo("PROTECTED");
	}

	@Test
	public void toNativeHeadersSubscribe() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		headers.setSubscriptionId("s1");
		headers.setDestination("/d");

		Map<String, List<String>> actual = headers.toNativeHeaderMap();

		assertThat(actual.size()).isEqualTo(2);
		assertThat(actual.get(StompHeaderAccessor.STOMP_ID_HEADER).get(0)).isEqualTo("s1");
		assertThat(actual.get(StompHeaderAccessor.STOMP_DESTINATION_HEADER).get(0)).isEqualTo("/d");
	}

	@Test
	public void toNativeHeadersUnsubscribe() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
		headers.setSubscriptionId("s1");

		Map<String, List<String>> actual = headers.toNativeHeaderMap();

		assertThat(actual.size()).isEqualTo(1);
		assertThat(actual.get(StompHeaderAccessor.STOMP_ID_HEADER).get(0)).isEqualTo("s1");
	}

	@Test
	public void toNativeHeadersMessageFrame() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.MESSAGE);
		headers.setSubscriptionId("s1");
		headers.setDestination("/d");
		headers.setContentType(MimeTypeUtils.APPLICATION_JSON);
		headers.updateStompCommandAsServerMessage();

		Map<String, List<String>> actual = headers.toNativeHeaderMap();

		assertThat(actual.size()).as(actual.toString()).isEqualTo(4);
		assertThat(actual.get(StompHeaderAccessor.STOMP_SUBSCRIPTION_HEADER).get(0)).isEqualTo("s1");
		assertThat(actual.get(StompHeaderAccessor.STOMP_DESTINATION_HEADER).get(0)).isEqualTo("/d");
		assertThat(actual.get(StompHeaderAccessor.STOMP_CONTENT_TYPE_HEADER).get(0)).isEqualTo("application/json");
		assertThat(actual.get(StompHeaderAccessor.STOMP_MESSAGE_ID_HEADER).get(0)).as("message-id was not created").isNotNull();
	}

	@Test
	public void toNativeHeadersContentType() {
		SimpMessageHeaderAccessor simpHeaderAccessor = SimpMessageHeaderAccessor.create();
		simpHeaderAccessor.setContentType(MimeType.valueOf("application/atom+xml"));
		Message<byte[]> message = MessageBuilder.createMessage(new byte[0], simpHeaderAccessor.getMessageHeaders());

		StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.wrap(message);
		Map<String, List<String>> map = stompHeaderAccessor.toNativeHeaderMap();

		assertThat(map.get(StompHeaderAccessor.STOMP_CONTENT_TYPE_HEADER).get(0)).isEqualTo("application/atom+xml");
	}

	@Test
	public void encodeConnectWithLoginAndPasscode() throws UnsupportedEncodingException {
		MultiValueMap<String, String> extHeaders = new LinkedMultiValueMap<>();
		extHeaders.add(StompHeaderAccessor.STOMP_LOGIN_HEADER, "joe");
		extHeaders.add(StompHeaderAccessor.STOMP_PASSCODE_HEADER, "joe123");

		StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.CONNECT, extHeaders);
		Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());
		byte[] bytes = new StompEncoder().encode(message);

		assertThat(new String(bytes, "UTF-8")).isEqualTo("CONNECT\nlogin:joe\npasscode:joe123\n\n\0");
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
		assertThat(actual.size()).isEqualTo(3);

		assertThat(actual.get(StompHeaderAccessor.STOMP_ID_HEADER).get(0)).isEqualTo("s1");
		assertThat(actual.get(StompHeaderAccessor.STOMP_DESTINATION_HEADER).get(0)).isEqualTo("/d");
		assertThat(actual.get("accountId").get(0)).as("abc123").isNotNull();
	}

	@Test
	public void messageIdAndTimestampDefaultBehavior() {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.SEND);
		MessageHeaders headers = headerAccessor.getMessageHeaders();

		assertThat((Object) headers.getId()).isNull();
		assertThat((Object) headers.getTimestamp()).isNull();
	}

	@Test
	public void messageIdAndTimestampEnabled() {
		IdTimestampMessageHeaderInitializer headerInitializer = new IdTimestampMessageHeaderInitializer();
		headerInitializer.setIdGenerator(new AlternativeJdkIdGenerator());
		headerInitializer.setEnableTimestamp(true);

		StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.SEND);
		headerInitializer.initHeaders(headerAccessor);

		assertThat(headerAccessor.getMessageHeaders().getId()).isNotNull();
		assertThat(headerAccessor.getMessageHeaders().getTimestamp()).isNotNull();
	}

	@Test
	public void getAccessor() {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());

		assertThat(MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class)).isSameAs(headerAccessor);
	}

	@Test
	public void getShortLogMessage() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
		accessor.setDestination("/foo");
		accessor.setContentType(MimeTypeUtils.APPLICATION_JSON);
		accessor.setSessionId("123");
		String actual = accessor.getShortLogMessage("payload".getBytes(StandardCharsets.UTF_8));
		assertThat(actual).isEqualTo("SEND /foo session=123 application/json payload=payload");

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 80; i++) {
			sb.append("a");
		}
		final String payload = sb.toString() + " > 80";
		actual = accessor.getShortLogMessage(payload.getBytes(StandardCharsets.UTF_8));
		assertThat(actual).isEqualTo(("SEND /foo session=123 application/json payload=" + sb + "...(truncated)"));
	}

}
