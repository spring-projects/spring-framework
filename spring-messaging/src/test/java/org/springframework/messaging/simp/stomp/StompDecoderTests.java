/*
 * Copyright 2002-2024 the original author or authors.
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

import java.nio.ByteBuffer;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.util.InvalidMimeTypeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test fixture for {@link StompDecoder}.
 *
 * @author Andy Wilkinson
 * @author Stephane Maldini
 */
class StompDecoderTests {

	private final StompDecoder decoder = new StompDecoder();


	@Test
	void decodeFrameWithCrLfEols() {
		Message<byte[]> frame = decode("DISCONNECT\r\n\r\n\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(frame);

		assertThat(headers.getCommand()).isEqualTo(StompCommand.DISCONNECT);
		assertThat(headers.toNativeHeaderMap()).isEmpty();
		assertThat(frame.getPayload()).isEmpty();
	}

	@Test
	void decodeFrameWithNoHeadersAndNoBody() {
		Message<byte[]> frame = decode("DISCONNECT\n\n\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(frame);

		assertThat(headers.getCommand()).isEqualTo(StompCommand.DISCONNECT);
		assertThat(headers.toNativeHeaderMap()).isEmpty();
		assertThat(frame.getPayload()).isEmpty();
	}

	@Test
	void decodeFrameWithNoBody() {
		String accept = "accept-version:1.1\n";
		String host = "host:github.org\n";

		Message<byte[]> frame = decode("CONNECT\n" + accept + host + "\n\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(frame);

		assertThat(headers.getCommand()).isEqualTo(StompCommand.CONNECT);

		assertThat(headers.toNativeHeaderMap()).hasSize(2);
		assertThat(headers.getFirstNativeHeader("accept-version")).isEqualTo("1.1");
		assertThat(headers.getHost()).isEqualTo("github.org");

		assertThat(frame.getPayload()).isEmpty();
	}

	@Test
	void decodeFrame() {
		Message<byte[]> frame = decode("SEND\ndestination:test\n\nThe body of the message\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(frame);

		assertThat(headers.getCommand()).isEqualTo(StompCommand.SEND);

		assertThat(headers.toNativeHeaderMap().size()).as(headers.toNativeHeaderMap().toString()).isEqualTo(1);
		assertThat(headers.getDestination()).isEqualTo("test");

		String bodyText = new String(frame.getPayload());
		assertThat(bodyText).isEqualTo("The body of the message");
	}

	@Test
	void decodeFrameWithContentLength() {
		Message<byte[]> message = decode("SEND\ncontent-length:23\n\nThe body of the message\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);

		assertThat(headers.getCommand()).isEqualTo(StompCommand.SEND);

		assertThat(headers.toNativeHeaderMap()).hasSize(1);
		assertThat(headers.getContentLength()).isEqualTo(Integer.valueOf(23));

		String bodyText = new String(message.getPayload());
		assertThat(bodyText).isEqualTo("The body of the message");
	}

	// SPR-11528

	@Test
	void decodeFrameWithInvalidContentLength() {
		Message<byte[]> message = decode("SEND\ncontent-length:-1\n\nThe body of the message\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);

		assertThat(headers.getCommand()).isEqualTo(StompCommand.SEND);

		assertThat(headers.toNativeHeaderMap()).hasSize(1);
		assertThat(headers.getContentLength()).isEqualTo(Integer.valueOf(-1));

		String bodyText = new String(message.getPayload());
		assertThat(bodyText).isEqualTo("The body of the message");
	}

	@Test
	void decodeFrameWithContentLengthZero() {
		Message<byte[]> frame = decode("SEND\ncontent-length:0\n\n\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(frame);

		assertThat(headers.getCommand()).isEqualTo(StompCommand.SEND);

		assertThat(headers.toNativeHeaderMap()).hasSize(1);
		assertThat(headers.getContentLength()).isEqualTo(Integer.valueOf(0));

		String bodyText = new String(frame.getPayload());
		assertThat(bodyText).isEmpty();
	}

	@Test
	void decodeFrameWithNullOctectsInTheBody() {
		Message<byte[]> frame = decode("SEND\ncontent-length:23\n\nThe b\0dy \0f the message\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(frame);

		assertThat(headers.getCommand()).isEqualTo(StompCommand.SEND);

		assertThat(headers.toNativeHeaderMap()).hasSize(1);
		assertThat(headers.getContentLength()).isEqualTo(Integer.valueOf(23));

		String bodyText = new String(frame.getPayload());
		assertThat(bodyText).isEqualTo("The b\0dy \0f the message");
	}

	@Test
	void decodeFrameWithEscapedHeaders() {
		Message<byte[]> frame = decode("DISCONNECT\na\\c\\r\\n\\\\b:alpha\\cbravo\\r\\n\\\\\n\n\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(frame);

		assertThat(headers.getCommand()).isEqualTo(StompCommand.DISCONNECT);

		assertThat(headers.toNativeHeaderMap()).hasSize(1);
		assertThat(headers.getFirstNativeHeader("a:\r\n\\b")).isEqualTo("alpha:bravo\r\n\\");
	}

	@Test // gh-27722
	public void decodeFrameWithHeaderWithBackslashValue() {
		String accept = "accept-version:1.1\n";
		String keyAndValueWithBackslash = "key:\\value\n";

		Message<byte[]> frame = decode("CONNECT\n" + accept + keyAndValueWithBackslash + "\n\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(frame);

		assertThat(headers.getCommand()).isEqualTo(StompCommand.CONNECT);

		assertThat(headers.toNativeHeaderMap()).hasSize(2);
		assertThat(headers.getFirstNativeHeader("accept-version")).isEqualTo("1.1");
		assertThat(headers.getFirstNativeHeader("key")).isEqualTo("\\value");

		assertThat(frame.getPayload()).isEmpty();
	}

	@Test
	void decodeFrameBodyNotAllowed() {
		assertThatExceptionOfType(StompConversionException.class).isThrownBy(() ->
				decode("CONNECT\naccept-version:1.2\n\nThe body of the message\0"));
	}

	@Test // gh-23713
	public void decodeFramesWithExtraNewLines() {
		String frame1 = "SEND\ndestination:test\n\nbody\0\n\n\n";
		ByteBuffer buffer = ByteBuffer.wrap((frame1).getBytes());

		final List<Message<byte[]>> messages = decoder.decode(buffer);

		assertThat(messages).hasSize(1);
		assertThat(StompHeaderAccessor.wrap(messages.get(0)).getCommand()).isEqualTo(StompCommand.SEND);
	}

	@Test
	void decodeMultipleFramesFromSameBuffer() {
		String frame1 = "SEND\ndestination:test\n\nThe body of the message\0";
		String frame2 = "DISCONNECT\n\n\0";
		ByteBuffer buffer = ByteBuffer.wrap((frame1 + frame2).getBytes());

		final List<Message<byte[]>> messages = decoder.decode(buffer);

		assertThat(messages).hasSize(2);
		assertThat(StompHeaderAccessor.wrap(messages.get(0)).getCommand()).isEqualTo(StompCommand.SEND);
		assertThat(StompHeaderAccessor.wrap(messages.get(1)).getCommand()).isEqualTo(StompCommand.DISCONNECT);
	}

	@Test // SPR-13111
	public void decodeFrameWithHeaderWithEmptyValue() {
		String accept = "accept-version:1.1\n";
		String valuelessKey = "key:\n";

		Message<byte[]> frame = decode("CONNECT\n" + accept + valuelessKey + "\n\0");
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(frame);

		assertThat(headers.getCommand()).isEqualTo(StompCommand.CONNECT);

		assertThat(headers.toNativeHeaderMap()).hasSize(2);
		assertThat(headers.getFirstNativeHeader("accept-version")).isEqualTo("1.1");
		assertThat(headers.getFirstNativeHeader("key")).isEmpty();

		assertThat(frame.getPayload()).isEmpty();
	}

	@Test
	void decodeFrameWithIncompleteCommand() {
		assertIncompleteDecode("MESSAG");
	}

	@Test
	void decodeFrameWithIncompleteHeader() {
		assertIncompleteDecode("SEND\ndestination");
		assertIncompleteDecode("SEND\ndestination:");
		assertIncompleteDecode("SEND\ndestination:test");
	}

	@Test
	void decodeFrameWithoutNullOctetTerminator() {
		assertIncompleteDecode("SEND\ndestination:test\n");
		assertIncompleteDecode("SEND\ndestination:test\n\n");
		assertIncompleteDecode("SEND\ndestination:test\n\nThe body");
	}

	@Test
	void decodeFrameWithInsufficientContent() {
		assertIncompleteDecode("SEND\ncontent-length:23\n\nThe body of the mess");
	}

	@Test
	void decodeFrameWithIncompleteContentType() {
		assertIncompleteDecode("SEND\ncontent-type:text/plain;charset=U");
	}

	@Test
	void decodeFrameWithInvalidContentType() {
		assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() ->
				assertIncompleteDecode("SEND\ncontent-type:text/plain;charset=U\n\nThe body\0"));
	}

	@Test
	void decodeFrameWithIncorrectTerminator() {
		assertThatExceptionOfType(StompConversionException.class).isThrownBy(() ->
				decode("SEND\ncontent-length:23\n\nThe body of the message*"));
	}

	@Test
	void decodeHeartbeat() {
		String frame = "\n";

		ByteBuffer buffer = ByteBuffer.wrap(frame.getBytes());

		final List<Message<byte[]>> messages = decoder.decode(buffer);

		assertThat(messages).hasSize(1);
		assertThat(StompHeaderAccessor.wrap(messages.get(0)).getMessageType()).isEqualTo(SimpMessageType.HEARTBEAT);
	}

	private void assertIncompleteDecode(String partialFrame) {
		ByteBuffer buffer = ByteBuffer.wrap(partialFrame.getBytes());
		assertThat(decode(buffer)).isNull();
		assertThat(buffer.position()).isEqualTo(0);
	}

	private Message<byte[]> decode(String stompFrame) {
		ByteBuffer buffer = ByteBuffer.wrap(stompFrame.getBytes());
		return decode(buffer);
	}

	private Message<byte[]> decode(ByteBuffer buffer) {
		List<Message<byte[]>> messages = this.decoder.decode(buffer);
		if (messages.isEmpty()) {
			return null;
		}
		else {
			return messages.get(0);
		}
	}

}
