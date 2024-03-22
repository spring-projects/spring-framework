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
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SplittingStompEncoder}.
 *
 * @author Injae Kim
 * @author Rossen Stoyanchev
 */
public class SplittingStompEncoderTests {

	private static final StompEncoder ENCODER = new StompEncoder();

	public static final byte[] EMPTY_PAYLOAD = new byte[0];


	@Test
	public void encodeFrameWithNoHeadersAndNoBody() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		List<byte[]> actual = splittingEncoder(null).encode(headers.getMessageHeaders(), EMPTY_PAYLOAD);

		assertThat(toAggregatedString(actual)).isEqualTo("DISCONNECT\n\n\0");
		assertThat(actual.size()).isOne();
	}

	@Test
	public void encodeFrameWithNoHeadersAndNoBodySplitTwoFrames() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		List<byte[]> actual = splittingEncoder(7).encode(headers.getMessageHeaders(), EMPTY_PAYLOAD);

		assertThat(toAggregatedString(actual)).isEqualTo("DISCONNECT\n\n\0");
		assertThat(actual.size()).isEqualTo(2);
	}

	@Test
	public void encodeFrameWithNoHeadersAndNoBodySplitMultipleFrames() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		List<byte[]> actual = splittingEncoder(3).encode(headers.getMessageHeaders(), EMPTY_PAYLOAD);

		assertThat(toAggregatedString(actual)).isEqualTo("DISCONNECT\n\n\0");
		assertThat(actual.size()).isEqualTo(5);
	}

	@Test
	public void encodeFrameWithHeaders() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
		headers.setAcceptVersion("1.2");
		headers.setHost("github.org");

		List<byte[]> actual = splittingEncoder(null).encode(headers.getMessageHeaders(), EMPTY_PAYLOAD);
		String output = toAggregatedString(actual);

		List<String> list = List.of(
				"CONNECT\naccept-version:1.2\nhost:github.org\n\n\0",
				"CONNECT\nhost:github.org\naccept-version:1.2\n\n\0");

		assertThat(list).contains(output);
		assertThat(actual.size()).isOne();
	}

	@Test
	public void encodeFrameWithHeadersSplitTwoFrames() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
		headers.setAcceptVersion("1.2");
		headers.setHost("github.org");

		List<byte[]> actual = splittingEncoder(30).encode(headers.getMessageHeaders(), EMPTY_PAYLOAD);
		String output = toAggregatedString(actual);

		assertThat("CONNECT\naccept-version:1.2\nhost:github.org\n\n\0".equals(output) ||
				"CONNECT\nhost:github.org\naccept-version:1.2\n\n\0".equals(output)).isTrue();
		assertThat(actual.size()).isEqualTo(2);
	}

	@Test
	public void encodeFrameWithHeadersSplitMultipleFrames() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
		headers.setAcceptVersion("1.2");
		headers.setHost("github.org");

		List<byte[]> actual = splittingEncoder(10).encode(headers.getMessageHeaders(), EMPTY_PAYLOAD);
		String output = toAggregatedString(actual);

		List<String> list = List.of(
				"CONNECT\naccept-version:1.2\nhost:github.org\n\n\0",
				"CONNECT\nhost:github.org\naccept-version:1.2\n\n\0");

		assertThat(list).contains(output);
		assertThat(actual.size()).isEqualTo(5);
	}

	@Test
	public void encodeFrameWithHeadersThatShouldBeEscaped() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		headers.addNativeHeader("a:\r\n\\b", "alpha:bravo\r\n\\");

		List<byte[]> actual = splittingEncoder(null).encode(headers.getMessageHeaders(), EMPTY_PAYLOAD);
		String output = toAggregatedString(actual);

		assertThat(output).isEqualTo("DISCONNECT\na\\c\\r\\n\\\\b:alpha\\cbravo\\r\\n\\\\\n\n\0");
		assertThat(actual.size()).isOne();
	}

	@Test
	public void encodeFrameWithHeadersThatShouldBeEscapedSplitTwoFrames() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		headers.addNativeHeader("a:\r\n\\b", "alpha:bravo\r\n\\");

		List<byte[]> actual = splittingEncoder(30).encode(headers.getMessageHeaders(), EMPTY_PAYLOAD);
		String output = toAggregatedString(actual);

		assertThat(output).isEqualTo("DISCONNECT\na\\c\\r\\n\\\\b:alpha\\cbravo\\r\\n\\\\\n\n\0");
		assertThat(actual.size()).isEqualTo(2);
	}


	@Test
	public void encodeFrameWithHeadersThatShouldBeEscapedSplitMultipleFrames() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		headers.addNativeHeader("a:\r\n\\b", "alpha:bravo\r\n\\");

		List<byte[]> actual = splittingEncoder(10).encode(headers.getMessageHeaders(), EMPTY_PAYLOAD);
		String output = toAggregatedString(actual);

		assertThat(output).isEqualTo("DISCONNECT\na\\c\\r\\n\\\\b:alpha\\cbravo\\r\\n\\\\\n\n\0");
		assertThat(actual.size()).isEqualTo(5);
	}


	@Test
	public void encodeFrameWithHeadersBody() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.addNativeHeader("a", "alpha");

		List<byte[]> actual = splittingEncoder(null).encode(headers.getMessageHeaders(), "Message body".getBytes());
		String output = toAggregatedString(actual);

		assertThat(output).isEqualTo("SEND\na:alpha\ncontent-length:12\n\nMessage body\0");
		assertThat(actual.size()).isOne();
	}

	@Test
	public void encodeFrameWithHeadersBodySplitTwoFrames() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.addNativeHeader("a", "alpha");

		List<byte[]> actual = splittingEncoder(30).encode(headers.getMessageHeaders(), "Message body".getBytes());
		String output = toAggregatedString(actual);

		assertThat(output).isEqualTo("SEND\na:alpha\ncontent-length:12\n\nMessage body\0");
		assertThat(actual.size()).isEqualTo(2);
	}

	@Test
	public void encodeFrameWithHeadersBodySplitMultipleFrames() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.addNativeHeader("a", "alpha");

		List<byte[]> actual = splittingEncoder(10).encode(headers.getMessageHeaders(), "Message body".getBytes());
		String output = toAggregatedString(actual);

		assertThat(output).isEqualTo("SEND\na:alpha\ncontent-length:12\n\nMessage body\0");
		assertThat(actual.size()).isEqualTo(5);
	}

	@Test
	public void encodeFrameWithContentLengthPresent() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setContentLength(12);

		List<byte[]> actual = splittingEncoder(null).encode(headers.getMessageHeaders(), "Message body".getBytes());
		String output = toAggregatedString(actual);

		assertThat(output).isEqualTo("SEND\ncontent-length:12\n\nMessage body\0");
		assertThat(actual.size()).isOne();
	}

	@Test
	public void encodeFrameWithContentLengthPresentSplitTwoFrames() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setContentLength(12);

		List<byte[]> actual = splittingEncoder(20).encode(headers.getMessageHeaders(), "Message body".getBytes());
		String output = toAggregatedString(actual);

		assertThat(output).isEqualTo("SEND\ncontent-length:12\n\nMessage body\0");
		assertThat(actual.size()).isEqualTo(2);
	}

	@Test
	public void encodeFrameWithContentLengthPresentSplitMultipleFrames() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setContentLength(12);

		List<byte[]> actual = splittingEncoder(10).encode(headers.getMessageHeaders(), "Message body".getBytes());
		String output = toAggregatedString(actual);

		assertThat(output).isEqualTo("SEND\ncontent-length:12\n\nMessage body\0");
		assertThat(actual.size()).isEqualTo(4);
	}

	@Test
	public void sameLengthAndBufferSizeLimit() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.addNativeHeader("a", "1234");

		List<byte[]> actual = splittingEncoder(44).encode(headers.getMessageHeaders(), "Message body".getBytes());
		String output = toAggregatedString(actual);

		assertThat(output).isEqualTo("SEND\na:1234\ncontent-length:12\n\nMessage body\0");
		assertThat(actual.size()).isOne();
	}

	@Test
	public void lengthAndBufferSizeLimitExactlySplitTwoFrames() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.addNativeHeader("a", "1234");

		List<byte[]> actual = splittingEncoder(22).encode(headers.getMessageHeaders(), "Message body".getBytes());
		String output = toAggregatedString(actual);

		assertThat(output).isEqualTo("SEND\na:1234\ncontent-length:12\n\nMessage body\0");
		assertThat(actual.size()).isEqualTo(2);
	}

	@Test
	public void lengthAndBufferSizeLimitExactlySplitMultipleFrames() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.addNativeHeader("a", "1234");

		List<byte[]> actual = splittingEncoder(11).encode(headers.getMessageHeaders(), "Message body".getBytes());
		String output = toAggregatedString(actual);

		assertThat(output).isEqualTo("SEND\na:1234\ncontent-length:12\n\nMessage body\0");
		assertThat(actual.size()).isEqualTo(4);
	}

	@Test
	public void bufferSizeLimitShouldBePositive() {
		assertThatThrownBy(() -> splittingEncoder(0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> splittingEncoder(-1)).isInstanceOf(IllegalArgumentException.class);
	}

	private static SplittingStompEncoder splittingEncoder(@Nullable Integer bufferSizeLimit) {
		return new SplittingStompEncoder(ENCODER, (bufferSizeLimit != null ? bufferSizeLimit : 64 * 1024));
	}

	private static String toAggregatedString(List<byte[]> actual) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		actual.forEach(outputStream::writeBytes);
		return outputStream.toString(StandardCharsets.UTF_8);
	}

}
