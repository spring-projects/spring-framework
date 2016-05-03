/*
 * Copyright 2002-2015 the original author or authors.
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
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.InvalidMimeTypeException;
import reactor.fn.Consumer;
import reactor.fn.Function;
import reactor.io.buffer.Buffer;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link Reactor2StompCodec}.
 *
 * @author Andy Wilkinson
 */
public class StompCodecTests {

	private final ArgumentCapturingConsumer<Message<byte[]>> consumer = new ArgumentCapturingConsumer<Message<byte[]>>();

	private final Function<Buffer, Message<byte[]>> decoder = new Reactor2StompCodec().decoder(consumer);

	@Test
	public void decodeFrameWithCrLfEols() {
		Message<byte[]> frame = decode("DISCONNECT\r\n\r\n\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(frame);

		assertEquals(StompCommand.DISCONNECT, headers.getCommand());
		assertEquals(0, headers.toNativeHeaderMap().size());
		assertEquals(0, frame.getPayload().length);
	}

	@Test
	public void decodeFrameWithNoHeadersAndNoBody() {
		Message<byte[]> frame = decode("DISCONNECT\n\n\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(frame);

		assertEquals(StompCommand.DISCONNECT, headers.getCommand());
		assertEquals(0, headers.toNativeHeaderMap().size());
		assertEquals(0, frame.getPayload().length);
	}

	@Test
	public void decodeFrameWithNoBody() {
		String accept = "accept-version:1.1\n";
		String host = "host:github.org\n";

		Message<byte[]> frame = decode("CONNECT\n" + accept + host + "\n\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(frame);

		assertEquals(StompCommand.CONNECT, headers.getCommand());

		assertEquals(2, headers.toNativeHeaderMap().size());
		assertEquals("1.1", headers.getFirstNativeHeader("accept-version"));
		assertEquals("github.org", headers.getHost());

		assertEquals(0, frame.getPayload().length);
	}

	@Test
	public void decodeFrame() throws UnsupportedEncodingException {
		Message<byte[]> frame = decode("SEND\ndestination:test\n\nThe body of the message\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(frame);

		assertEquals(StompCommand.SEND, headers.getCommand());

		assertEquals(headers.toNativeHeaderMap().toString(), 1, headers.toNativeHeaderMap().size());
		assertEquals("test", headers.getDestination());

		String bodyText = new String(frame.getPayload());
		assertEquals("The body of the message", bodyText);
	}

	@Test
	public void decodeFrameWithContentLength() {
		Message<byte[]> message = decode("SEND\ncontent-length:23\n\nThe body of the message\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);

		assertEquals(StompCommand.SEND, headers.getCommand());

		assertEquals(1, headers.toNativeHeaderMap().size());
		assertEquals(Integer.valueOf(23), headers.getContentLength());

		String bodyText = new String(message.getPayload());
		assertEquals("The body of the message", bodyText);
	}

	// SPR-11528

	@Test
	public void decodeFrameWithInvalidContentLength() {
		Message<byte[]> message = decode("SEND\ncontent-length:-1\n\nThe body of the message\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);

		assertEquals(StompCommand.SEND, headers.getCommand());

		assertEquals(1, headers.toNativeHeaderMap().size());
		assertEquals(Integer.valueOf(-1), headers.getContentLength());

		String bodyText = new String(message.getPayload());
		assertEquals("The body of the message", bodyText);
	}

	@Test
	public void decodeFrameWithContentLengthZero() {
		Message<byte[]> frame = decode("SEND\ncontent-length:0\n\n\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(frame);

		assertEquals(StompCommand.SEND, headers.getCommand());

		assertEquals(1, headers.toNativeHeaderMap().size());
		assertEquals(Integer.valueOf(0), headers.getContentLength());

		String bodyText = new String(frame.getPayload());
		assertEquals("", bodyText);
	}

	@Test
	public void decodeFrameWithNullOctectsInTheBody() {
		Message<byte[]> frame = decode("SEND\ncontent-length:23\n\nThe b\0dy \0f the message\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(frame);

		assertEquals(StompCommand.SEND, headers.getCommand());

		assertEquals(1, headers.toNativeHeaderMap().size());
		assertEquals(Integer.valueOf(23), headers.getContentLength());

		String bodyText = new String(frame.getPayload());
		assertEquals("The b\0dy \0f the message", bodyText);
	}

	@Test
	public void decodeFrameWithEscapedHeaders() {
		Message<byte[]> frame = decode("DISCONNECT\na\\c\\r\\n\\\\b:alpha\\cbravo\\r\\n\\\\\n\n\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(frame);

		assertEquals(StompCommand.DISCONNECT, headers.getCommand());

		assertEquals(1, headers.toNativeHeaderMap().size());
		assertEquals("alpha:bravo\r\n\\", headers.getFirstNativeHeader("a:\r\n\\b"));
	}

	@Test(expected=StompConversionException.class)
	public void decodeFrameBodyNotAllowed() {
		decode("CONNECT\naccept-version:1.2\n\nThe body of the message\0");
	}

	@Test
	public void decodeMultipleFramesFromSameBuffer() {
		String frame1 = "SEND\ndestination:test\n\nThe body of the message\0";
		String frame2 = "DISCONNECT\n\n\0";

		Buffer buffer = Buffer.wrap(frame1 + frame2);

		final List<Message<byte[]>> messages = new ArrayList<Message<byte[]>>();
		new Reactor2StompCodec().decoder(messages::add).apply(buffer);

		assertEquals(2, messages.size());
		assertEquals(StompCommand.SEND, StompHeaderAccessor.wrap(messages.get(0)).getCommand());
		assertEquals(StompCommand.DISCONNECT, StompHeaderAccessor.wrap(messages.get(1)).getCommand());
	}

	// SPR-13111

	@Test
	public void decodeFrameWithHeaderWithEmptyValue() {
		String accept = "accept-version:1.1\n";
		String valuelessKey = "key:\n";

		Message<byte[]> frame = decode("CONNECT\n" + accept + valuelessKey + "\n\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(frame);

		assertEquals(StompCommand.CONNECT, headers.getCommand());

		assertEquals(2, headers.toNativeHeaderMap().size());
		assertEquals("1.1", headers.getFirstNativeHeader("accept-version"));
		assertEquals("", headers.getFirstNativeHeader("key"));

		assertEquals(0, frame.getPayload().length);
	}

	@Test
	public void decodeFrameWithIncompleteCommand() {
		assertIncompleteDecode("MESSAG");
	}

	@Test
	public void decodeFrameWithIncompleteHeader() {
		assertIncompleteDecode("SEND\ndestination");
		assertIncompleteDecode("SEND\ndestination:");
		assertIncompleteDecode("SEND\ndestination:test");
	}

	@Test
	public void decodeFrameWithoutNullOctetTerminator() {
		assertIncompleteDecode("SEND\ndestination:test\n");
		assertIncompleteDecode("SEND\ndestination:test\n\n");
		assertIncompleteDecode("SEND\ndestination:test\n\nThe body");
	}

	@Test
	public void decodeFrameWithInsufficientContent() {
		assertIncompleteDecode("SEND\ncontent-length:23\n\nThe body of the mess");
	}

	@Test
	public void decodeFrameWithIncompleteContentType() {
		assertIncompleteDecode("SEND\ncontent-type:text/plain;charset=U");
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void decodeFrameWithInvalidContentType() {
		assertIncompleteDecode("SEND\ncontent-type:text/plain;charset=U\n\nThe body\0");
	}

	@Test(expected=StompConversionException.class)
	public void decodeFrameWithIncorrectTerminator() {
		decode("SEND\ncontent-length:23\n\nThe body of the message*");
	}

	@Test
	public void decodeHeartbeat() {
		String frame = "\n";

		Buffer buffer = Buffer.wrap(frame);

		final List<Message<byte[]>> messages = new ArrayList<Message<byte[]>>();
		new Reactor2StompCodec().decoder(messages::add).apply(buffer);

		assertEquals(1, messages.size());
		assertEquals(SimpMessageType.HEARTBEAT, StompHeaderAccessor.wrap(messages.get(0)).getMessageType());
	}

	@Test
	public void encodeFrameWithNoHeadersAndNoBody() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);

		Message<byte[]> frame = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

		assertEquals("DISCONNECT\n\n\0", new Reactor2StompCodec().encoder().apply(frame).asString());
	}

	@Test
	public void encodeFrameWithHeaders() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
		headers.setAcceptVersion("1.2");
		headers.setHost("github.org");

		Message<byte[]> frame = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

		String frameString = new Reactor2StompCodec().encoder().apply(frame).asString();

		assertTrue(frameString.equals("CONNECT\naccept-version:1.2\nhost:github.org\n\n\0") ||
				frameString.equals("CONNECT\nhost:github.org\naccept-version:1.2\n\n\0"));
	}

	@Test
	public void encodeFrameWithHeadersThatShouldBeEscaped() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		headers.addNativeHeader("a:\r\n\\b",  "alpha:bravo\r\n\\");

		Message<byte[]> frame = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

		assertEquals("DISCONNECT\na\\c\\r\\n\\\\b:alpha\\cbravo\\r\\n\\\\\n\n\0",
				new Reactor2StompCodec().encoder().apply(frame).asString());
	}

	@Test
	public void encodeFrameWithHeadersBody() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.addNativeHeader("a", "alpha");

		Message<byte[]> frame = MessageBuilder.createMessage("Message body".getBytes(), headers.getMessageHeaders());

		assertEquals("SEND\na:alpha\ncontent-length:12\n\nMessage body\0",
				new Reactor2StompCodec().encoder().apply(frame).asString());
	}

	@Test
	public void encodeFrameWithContentLengthPresent() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setContentLength(12);

		Message<byte[]> frame = MessageBuilder.createMessage("Message body".getBytes(), headers.getMessageHeaders());

		assertEquals("SEND\ncontent-length:12\n\nMessage body\0",
				new Reactor2StompCodec().encoder().apply(frame).asString());
	}

	private void assertIncompleteDecode(String partialFrame) {
		Buffer buffer = Buffer.wrap(partialFrame);
		assertNull(decode(buffer));
		assertEquals(0, buffer.position());
	}

	private Message<byte[]> decode(String stompFrame) {
		Buffer buffer = Buffer.wrap(stompFrame);
		return decode(buffer);
	}

	private Message<byte[]> decode(Buffer buffer) {
		this.decoder.apply(buffer);
		if (consumer.arguments.isEmpty()) {
			return null;
		}
		else {
			return consumer.arguments.get(0);
		}
	}



	private static final class ArgumentCapturingConsumer<T> implements Consumer<T> {

		private final List<T> arguments = new ArrayList<T>();

		@Override
		public void accept(T t) {
			arguments.add(t);
		}

	}
}
