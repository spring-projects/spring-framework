/*
 * Copyright 2024-2024 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SplittingStompEncoder}.
 *
 * @author Injae Kim
 * @since 6.2
 */
public class SplittingStompEncoderTests {

	private final StompEncoder STOMP_ENCODER = new StompEncoder();

	private static final int DEFAULT_MESSAGE_MAX_SIZE = 64 * 1024;

	@Test
	public void encodeFrameWithNoHeadersAndNoBody() {
		SplittingStompEncoder encoder = new SplittingStompEncoder(STOMP_ENCODER, DEFAULT_MESSAGE_MAX_SIZE);
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		Message<byte[]> frame = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

		List<byte[]> actual = encoder.encode(frame.getHeaders(), frame.getPayload());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);

		assertThat(outputStream.toString()).isEqualTo("DISCONNECT\n\n\0");
		assertThat(actual.size()).isOne();
	}

	@Test
	public void encodeFrameWithNoHeadersAndNoBodySplitTwoFrames() {
		SplittingStompEncoder encoder = new SplittingStompEncoder(STOMP_ENCODER, 7);
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		Message<byte[]> frame = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

		List<byte[]> actual = encoder.encode(frame.getHeaders(), frame.getPayload());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);

		assertThat(outputStream.toString()).isEqualTo("DISCONNECT\n\n\0");
		assertThat(actual.size()).isEqualTo(2);
		assertThat(actual.get(0)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 0, 7));
		assertThat(actual.get(1)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 7, outputStream.size()));
	}

	@Test
	public void encodeFrameWithNoHeadersAndNoBodySplitMultipleFrames() {
		SplittingStompEncoder encoder = new SplittingStompEncoder(STOMP_ENCODER, 3);
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		Message<byte[]> frame = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

		List<byte[]> actual = encoder.encode(frame.getHeaders(), frame.getPayload());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);

		assertThat(outputStream.toString()).isEqualTo("DISCONNECT\n\n\0");
		assertThat(actual.size()).isEqualTo(5);
		assertThat(actual.get(0)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 0, 3));
		assertThat(actual.get(1)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 3, 6));
		assertThat(actual.get(2)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 6, 9));
		assertThat(actual.get(3)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 9, 12));
		assertThat(actual.get(4)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 12, outputStream.size()));
	}

	@Test
	public void encodeFrameWithHeaders() {
		SplittingStompEncoder encoder = new SplittingStompEncoder(STOMP_ENCODER, DEFAULT_MESSAGE_MAX_SIZE);
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
		headers.setAcceptVersion("1.2");
		headers.setHost("github.org");
		Message<byte[]> frame = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

		List<byte[]> actual = encoder.encode(frame.getHeaders(), frame.getPayload());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);
		String actualString = outputStream.toString();

		assertThat("CONNECT\naccept-version:1.2\nhost:github.org\n\n\0".equals(actualString) ||
				"CONNECT\nhost:github.org\naccept-version:1.2\n\n\0".equals(actualString)).isTrue();
		assertThat(actual.size()).isOne();
	}

	@Test
	public void encodeFrameWithHeadersSplitTwoFrames() {
		SplittingStompEncoder encoder = new SplittingStompEncoder(STOMP_ENCODER, 30);
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
		headers.setAcceptVersion("1.2");
		headers.setHost("github.org");
		Message<byte[]> frame = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

		List<byte[]> actual = encoder.encode(frame.getHeaders(), frame.getPayload());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);
		String actualString = outputStream.toString();

		assertThat("CONNECT\naccept-version:1.2\nhost:github.org\n\n\0".equals(actualString) ||
				"CONNECT\nhost:github.org\naccept-version:1.2\n\n\0".equals(actualString)).isTrue();
		assertThat(actual.size()).isEqualTo(2);
		assertThat(actual.get(0)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 0, 30));
		assertThat(actual.get(1)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 30, outputStream.size()));
	}

	@Test
	public void encodeFrameWithHeadersSplitMultipleFrames() {
		SplittingStompEncoder encoder = new SplittingStompEncoder(STOMP_ENCODER, 10);
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
		headers.setAcceptVersion("1.2");
		headers.setHost("github.org");
		Message<byte[]> frame = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

		List<byte[]> actual = encoder.encode(frame.getHeaders(), frame.getPayload());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);
		String actualString = outputStream.toString();

		assertThat("CONNECT\naccept-version:1.2\nhost:github.org\n\n\0".equals(actualString) ||
				"CONNECT\nhost:github.org\naccept-version:1.2\n\n\0".equals(actualString)).isTrue();
		assertThat(actual.size()).isEqualTo(5);
		assertThat(actual.get(0)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 0, 10));
		assertThat(actual.get(1)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 10, 20));
		assertThat(actual.get(2)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 20, 30));
		assertThat(actual.get(3)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 30, 40));
		assertThat(actual.get(4)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 40, outputStream.size()));
	}

	@Test
	public void encodeFrameWithHeadersThatShouldBeEscaped() {
		SplittingStompEncoder encoder = new SplittingStompEncoder(STOMP_ENCODER, DEFAULT_MESSAGE_MAX_SIZE);
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		headers.addNativeHeader("a:\r\n\\b", "alpha:bravo\r\n\\");
		Message<byte[]> frame = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

		List<byte[]> actual = encoder.encode(frame.getHeaders(), frame.getPayload());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);

		assertThat(outputStream.toString()).isEqualTo("DISCONNECT\na\\c\\r\\n\\\\b:alpha\\cbravo\\r\\n\\\\\n\n\0");
		assertThat(actual.size()).isOne();
	}

	@Test
	public void encodeFrameWithHeadersThatShouldBeEscapedSplitTwoFrames() {
		SplittingStompEncoder encoder = new SplittingStompEncoder(STOMP_ENCODER, 30);
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		headers.addNativeHeader("a:\r\n\\b", "alpha:bravo\r\n\\");
		Message<byte[]> frame = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

		List<byte[]> actual = encoder.encode(frame.getHeaders(), frame.getPayload());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);

		assertThat(outputStream.toString()).isEqualTo("DISCONNECT\na\\c\\r\\n\\\\b:alpha\\cbravo\\r\\n\\\\\n\n\0");
		assertThat(actual.size()).isEqualTo(2);
		assertThat(actual.get(0)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 0, 30));
		assertThat(actual.get(1)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 30, outputStream.size()));
	}


	@Test
	public void encodeFrameWithHeadersThatShouldBeEscapedSplitMultipleFrames() {
		SplittingStompEncoder encoder = new SplittingStompEncoder(STOMP_ENCODER, 10);
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		headers.addNativeHeader("a:\r\n\\b", "alpha:bravo\r\n\\");
		Message<byte[]> frame = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

		List<byte[]> actual = encoder.encode(frame.getHeaders(), frame.getPayload());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);
		String actualString = outputStream.toString();

		assertThat(outputStream.toString()).isEqualTo("DISCONNECT\na\\c\\r\\n\\\\b:alpha\\cbravo\\r\\n\\\\\n\n\0");
		assertThat(actual.size()).isEqualTo(5);
		assertThat(actual.get(0)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 0, 10));
		assertThat(actual.get(1)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 10, 20));
		assertThat(actual.get(2)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 20, 30));
		assertThat(actual.get(3)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 30, 40));
		assertThat(actual.get(4)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 40, outputStream.size()));
	}


	@Test
	public void encodeFrameWithHeadersBody() {
		SplittingStompEncoder encoder = new SplittingStompEncoder(STOMP_ENCODER, DEFAULT_MESSAGE_MAX_SIZE);
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.addNativeHeader("a", "alpha");
		Message<byte[]> frame = MessageBuilder.createMessage(
				"Message body".getBytes(), headers.getMessageHeaders());

		List<byte[]> actual = encoder.encode(frame.getHeaders(), frame.getPayload());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);

		assertThat(outputStream.toString()).isEqualTo("SEND\na:alpha\ncontent-length:12\n\nMessage body\0");
		assertThat(actual.size()).isOne();
	}

	@Test
	public void encodeFrameWithHeadersBodySplitTwoFrames() {
		SplittingStompEncoder encoder = new SplittingStompEncoder(STOMP_ENCODER, 30);
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.addNativeHeader("a", "alpha");
		Message<byte[]> frame = MessageBuilder.createMessage(
				"Message body".getBytes(), headers.getMessageHeaders());

		List<byte[]> actual = encoder.encode(frame.getHeaders(), frame.getPayload());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);

		assertThat(outputStream.toString()).isEqualTo("SEND\na:alpha\ncontent-length:12\n\nMessage body\0");
		assertThat(actual.size()).isEqualTo(2);
		assertThat(actual.get(0)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 0, 30));
		assertThat(actual.get(1)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 30, outputStream.size()));
	}

	@Test
	public void encodeFrameWithHeadersBodySplitMultipleFrames() {
		SplittingStompEncoder encoder = new SplittingStompEncoder(STOMP_ENCODER, 10);
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.addNativeHeader("a", "alpha");
		Message<byte[]> frame = MessageBuilder.createMessage(
				"Message body".getBytes(), headers.getMessageHeaders());

		List<byte[]> actual = encoder.encode(frame.getHeaders(), frame.getPayload());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);

		assertThat(outputStream.toString()).isEqualTo("SEND\na:alpha\ncontent-length:12\n\nMessage body\0");
		assertThat(actual.size()).isEqualTo(5);
		assertThat(actual.get(0)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 0, 10));
		assertThat(actual.get(1)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 10, 20));
		assertThat(actual.get(2)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 20, 30));
		assertThat(actual.get(3)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 30, 40));
		assertThat(actual.get(4)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 40, outputStream.size()));
	}

	@Test
	public void encodeFrameWithContentLengthPresent() {
		SplittingStompEncoder encoder = new SplittingStompEncoder(STOMP_ENCODER, DEFAULT_MESSAGE_MAX_SIZE);
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setContentLength(12);
		Message<byte[]> frame = MessageBuilder.createMessage(
				"Message body".getBytes(), headers.getMessageHeaders());

		List<byte[]> actual = encoder.encode(frame.getHeaders(), frame.getPayload());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);

		assertThat(outputStream.toString()).isEqualTo("SEND\ncontent-length:12\n\nMessage body\0");
		assertThat(actual.size()).isOne();
	}

	@Test
	public void encodeFrameWithContentLengthPresentSplitTwoFrames() {
		SplittingStompEncoder encoder = new SplittingStompEncoder(STOMP_ENCODER, 20);
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setContentLength(12);
		Message<byte[]> frame = MessageBuilder.createMessage(
				"Message body".getBytes(), headers.getMessageHeaders());

		List<byte[]> actual = encoder.encode(frame.getHeaders(), frame.getPayload());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);

		assertThat(outputStream.toString()).isEqualTo("SEND\ncontent-length:12\n\nMessage body\0");
		assertThat(actual.size()).isEqualTo(2);
		assertThat(actual.get(0)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 0, 20));
		assertThat(actual.get(1)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 20, outputStream.size()));
	}

	@Test
	public void encodeFrameWithContentLengthPresentSplitMultipleFrames() {
		SplittingStompEncoder encoder = new SplittingStompEncoder(STOMP_ENCODER, 10);
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setContentLength(12);
		Message<byte[]> frame = MessageBuilder.createMessage(
				"Message body".getBytes(), headers.getMessageHeaders());

		List<byte[]> actual = encoder.encode(frame.getHeaders(), frame.getPayload());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);

		assertThat(outputStream.toString()).isEqualTo("SEND\ncontent-length:12\n\nMessage body\0");
		assertThat(actual.size()).isEqualTo(4);
		assertThat(actual.get(0)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 0, 10));
		assertThat(actual.get(1)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 10, 20));
		assertThat(actual.get(2)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 20, 30));
		assertThat(actual.get(3)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 30, outputStream.size()));
	}

	@Test
	public void sameLengthAndBufferSizeLimit() {
		SplittingStompEncoder encoder = new SplittingStompEncoder(STOMP_ENCODER, 44);
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.addNativeHeader("a", "1234");
		Message<byte[]> frame = MessageBuilder.createMessage(
				"Message body".getBytes(), headers.getMessageHeaders());

		List<byte[]> actual = encoder.encode(frame.getHeaders(), frame.getPayload());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);

		assertThat(outputStream.toString()).isEqualTo("SEND\na:1234\ncontent-length:12\n\nMessage body\0");
		assertThat(actual.size()).isOne();
		assertThat(outputStream.toByteArray().length).isEqualTo(44);
	}

	@Test
	public void lengthAndBufferSizeLimitExactlySplitTwoFrames() {
		SplittingStompEncoder encoder = new SplittingStompEncoder(STOMP_ENCODER, 22);
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.addNativeHeader("a", "1234");
		Message<byte[]> frame = MessageBuilder.createMessage(
				"Message body".getBytes(), headers.getMessageHeaders());

		List<byte[]> actual = encoder.encode(frame.getHeaders(), frame.getPayload());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);

		assertThat(outputStream.toString()).isEqualTo("SEND\na:1234\ncontent-length:12\n\nMessage body\0");
		assertThat(actual.size()).isEqualTo(2);
		assertThat(outputStream.toByteArray().length).isEqualTo(44);
		assertThat(actual.get(0)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 0, 22));
		assertThat(actual.get(1)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 22, 44));
	}

	@Test
	public void lengthAndBufferSizeLimitExactlySplitMultipleFrames() {
		SplittingStompEncoder encoder = new SplittingStompEncoder(STOMP_ENCODER, 11);
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.addNativeHeader("a", "1234");
		Message<byte[]> frame = MessageBuilder.createMessage(
				"Message body".getBytes(), headers.getMessageHeaders());

		List<byte[]> actual = encoder.encode(frame.getHeaders(), frame.getPayload());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);

		assertThat(outputStream.toString()).isEqualTo("SEND\na:1234\ncontent-length:12\n\nMessage body\0");
		assertThat(actual.size()).isEqualTo(4);
		assertThat(outputStream.toByteArray().length).isEqualTo(44);
		assertThat(actual.get(0)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 0, 11));
		assertThat(actual.get(1)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 11, 22));
		assertThat(actual.get(2)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 22, 33));
		assertThat(actual.get(3)).isEqualTo(Arrays.copyOfRange(outputStream.toByteArray(), 33, 44));
	}

	@Test
	public void bufferSizeLimitShouldBePositive() {
		assertThatThrownBy(() -> new SplittingStompEncoder(STOMP_ENCODER, 0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new SplittingStompEncoder(STOMP_ENCODER, -1))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
