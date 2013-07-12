/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;

import static org.junit.Assert.*;

/**
 * @author Gary Russell
 * @author Rossen Stoyanchev
 */
public class StompMessageConverterTests {

	private StompMessageConverter converter;


	@Before
	public void setup() {
		this.converter = new StompMessageConverter();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void connectFrame() throws Exception {

		String accept = "accept-version:1.1\n";
		String host = "host:github.org\n";
		String frame = "\n\n\nCONNECT\n" + accept + host + "\n";
		Message<byte[]> message = (Message<byte[]>) this.converter.toMessage(frame.getBytes("UTF-8"));

		assertEquals(0, message.getPayload().length);

		MessageHeaders headers = message.getHeaders();
		StompHeaderAccessor stompHeaders = StompHeaderAccessor.wrap(message);
		Map<String, Object> map = stompHeaders.toMap();
		assertEquals(5, map.size());
		assertNotNull(map.get(MessageHeaders.ID));
		assertNotNull(map.get(MessageHeaders.TIMESTAMP));
		assertNotNull(map.get(SimpMessageHeaderAccessor.NATIVE_HEADERS));
		assertNotNull(map.get(SimpMessageHeaderAccessor.MESSAGE_TYPE));
		assertNotNull(map.get(SimpMessageHeaderAccessor.PROTOCOL_MESSAGE_TYPE));

		assertEquals(Collections.singleton("1.1"), stompHeaders.getAcceptVersion());
		assertEquals("github.org", stompHeaders.getHost());

		assertEquals(SimpMessageType.CONNECT, stompHeaders.getMessageType());
		assertEquals(StompCommand.CONNECT, stompHeaders.getStompCommand());
		assertNotNull(headers.get(MessageHeaders.ID));
		assertNotNull(headers.get(MessageHeaders.TIMESTAMP));

		String convertedBack = new String(this.converter.fromMessage(message), "UTF-8");

		assertEquals("CONNECT\n", convertedBack.substring(0,8));
		assertTrue(convertedBack.contains(accept));
		assertTrue(convertedBack.contains(host));
	}

	@Test
	public void connectWithEscapes() throws Exception {

		String accept = "accept-version:1.1\n";
		String host = "ho\\c\\ns\\rt:st\\nomp.gi\\cthu\\b.org\n";
		String frame = "CONNECT\n" + accept + host + "\n";
		@SuppressWarnings("unchecked")
		Message<byte[]> message = (Message<byte[]>) this.converter.toMessage(frame.getBytes("UTF-8"));

		assertEquals(0, message.getPayload().length);

		StompHeaderAccessor stompHeaders = StompHeaderAccessor.wrap(message);
		assertEquals(Collections.singleton("1.1"), stompHeaders.getAcceptVersion());
		assertEquals("st\nomp.gi:thu\\b.org", stompHeaders.toNativeHeaderMap().get("ho:\ns\rt").get(0));

		String convertedBack = new String(this.converter.fromMessage(message), "UTF-8");

		assertEquals("CONNECT\n", convertedBack.substring(0,8));
		assertTrue(convertedBack.contains(accept));
		assertTrue(convertedBack.contains(host));
	}

	@Test
	public void connectCR12() throws Exception {

		String accept = "accept-version:1.2\n";
		String host = "host:github.org\n";
		String test = "CONNECT\r\n" + accept.replaceAll("\n", "\r\n") + host.replaceAll("\n", "\r\n") + "\r\n";
		@SuppressWarnings("unchecked")
		Message<byte[]> message = (Message<byte[]>) this.converter.toMessage(test.getBytes("UTF-8"));

		assertEquals(0, message.getPayload().length);

		StompHeaderAccessor stompHeaders = StompHeaderAccessor.wrap(message);
		assertEquals(Collections.singleton("1.2"), stompHeaders.getAcceptVersion());
		assertEquals("github.org", stompHeaders.getHost());

		String convertedBack = new String(this.converter.fromMessage(message), "UTF-8");

		assertEquals("CONNECT\n", convertedBack.substring(0,8));
		assertTrue(convertedBack.contains(accept));
		assertTrue(convertedBack.contains(host));
	}

	@Test
	public void connectWithEscapesAndCR12() throws Exception {

		String accept = "accept-version:1.1\n";
		String host = "ho\\c\\ns\\rt:st\\nomp.gi\\cthu\\b.org\n";
		String test = "\n\n\nCONNECT\r\n" + accept.replaceAll("\n", "\r\n") + host.replaceAll("\n", "\r\n") + "\r\n";
		@SuppressWarnings("unchecked")
		Message<byte[]> message = (Message<byte[]>) this.converter.toMessage(test.getBytes("UTF-8"));

		assertEquals(0, message.getPayload().length);

		StompHeaderAccessor stompHeaders = StompHeaderAccessor.wrap(message);
		assertEquals(Collections.singleton("1.1"), stompHeaders.getAcceptVersion());
		assertEquals("st\nomp.gi:thu\\b.org", stompHeaders.toNativeHeaderMap().get("ho:\ns\rt").get(0));

		String convertedBack = new String(this.converter.fromMessage(message), "UTF-8");

		assertEquals("CONNECT\n", convertedBack.substring(0,8));
		assertTrue(convertedBack.contains(accept));
		assertTrue(convertedBack.contains(host));
	}

}
