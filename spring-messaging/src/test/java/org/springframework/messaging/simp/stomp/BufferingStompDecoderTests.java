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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link BufferingStompDecoder}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
class BufferingStompDecoderTests {

	private static final StompDecoder STOMP_DECODER = new StompDecoder();


	@Test
	void basic() {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		String chunk = "SEND\na:alpha\n\nMessage body\0";

		List<Message<byte[]>> messages = stompDecoder.decode(toByteBuffer(chunk));
		assertThat(messages).hasSize(1);
		assertThat(new String(messages.get(0).getPayload())).isEqualTo("Message body");

		assertThat(stompDecoder.getBufferSize()).isEqualTo(0);
		assertThat(stompDecoder.getExpectedContentLength()).isNull();
	}

	@Test
	void oneMessageInTwoChunks() {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		String chunk1 = "SEND\na:alpha\n\nMessage";
		String chunk2 = " body\0";

		List<Message<byte[]>> messages = stompDecoder.decode(toByteBuffer(chunk1));
		assertThat(messages).isEqualTo(Collections.<Message<byte[]>>emptyList());

		messages = stompDecoder.decode(toByteBuffer(chunk2));
		assertThat(messages).hasSize(1);
		assertThat(new String(messages.get(0).getPayload())).isEqualTo("Message body");

		assertThat(stompDecoder.getBufferSize()).isEqualTo(0);
		assertThat(stompDecoder.getExpectedContentLength()).isNull();
	}

	@Test
	void twoMessagesInOneChunk() {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		String chunk = """
				SEND
				a:alpha

				Payload1\0SEND
				a:alpha

				Payload2\0""";
		List<Message<byte[]>> messages = stompDecoder.decode(toByteBuffer(chunk));

		assertThat(messages).hasSize(2);
		assertThat(new String(messages.get(0).getPayload())).isEqualTo("Payload1");
		assertThat(new String(messages.get(1).getPayload())).isEqualTo("Payload2");

		assertThat(stompDecoder.getBufferSize()).isEqualTo(0);
		assertThat(stompDecoder.getExpectedContentLength()).isNull();
	}

	@Test
	void oneFullAndOneSplitMessageContentLength() {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		int contentLength = "Payload2a-Payload2b".getBytes().length;
		String chunk1 = "SEND\na:alpha\n\nPayload1\0SEND\ncontent-length:" + contentLength + "\n";
		List<Message<byte[]>> messages = stompDecoder.decode(toByteBuffer(chunk1));

		assertThat(messages).hasSize(1);
		assertThat(new String(messages.get(0).getPayload())).isEqualTo("Payload1");

		assertThat(stompDecoder.getBufferSize()).isEqualTo(23);
		assertThat((int) stompDecoder.getExpectedContentLength()).isEqualTo(contentLength);

		String chunk2 = "\nPayload2a";
		messages = stompDecoder.decode(toByteBuffer(chunk2));

		assertThat(messages).isEmpty();
		assertThat(stompDecoder.getBufferSize()).isEqualTo(33);
		assertThat((int) stompDecoder.getExpectedContentLength()).isEqualTo(contentLength);

		String chunk3 = "-Payload2b\0";
		messages = stompDecoder.decode(toByteBuffer(chunk3));

		assertThat(messages).hasSize(1);
		assertThat(new String(messages.get(0).getPayload())).isEqualTo("Payload2a-Payload2b");
		assertThat(stompDecoder.getBufferSize()).isEqualTo(0);
		assertThat(stompDecoder.getExpectedContentLength()).isNull();
	}

	@Test
	void oneFullAndOneSplitMessageNoContentLength() {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		String chunk1 = "SEND\na:alpha\n\nPayload1\0SEND\na:alpha\n";
		List<Message<byte[]>> messages = stompDecoder.decode(toByteBuffer(chunk1));

		assertThat(messages).hasSize(1);
		assertThat(new String(messages.get(0).getPayload())).isEqualTo("Payload1");

		assertThat(stompDecoder.getBufferSize()).isEqualTo(13);
		assertThat(stompDecoder.getExpectedContentLength()).isNull();

		String chunk2 = "\nPayload2a";
		messages = stompDecoder.decode(toByteBuffer(chunk2));

		assertThat(messages).isEmpty();
		assertThat(stompDecoder.getBufferSize()).isEqualTo(23);
		assertThat(stompDecoder.getExpectedContentLength()).isNull();

		String chunk3 = "-Payload2b\0";
		messages = stompDecoder.decode(toByteBuffer(chunk3));

		assertThat(messages).hasSize(1);
		assertThat(new String(messages.get(0).getPayload())).isEqualTo("Payload2a-Payload2b");
		assertThat(stompDecoder.getBufferSize()).isEqualTo(0);
		assertThat(stompDecoder.getExpectedContentLength()).isNull();
	}

	@Test
	void oneFullAndOneSplitWithContentLengthExceedingBufferSize() {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		String chunk1 = "SEND\na:alpha\n\nPayload1\0SEND\ncontent-length:129\n";
		List<Message<byte[]>> messages = stompDecoder.decode(toByteBuffer(chunk1));

		assertThat(messages).as("We should have gotten the 1st message").hasSize(1);
		assertThat(new String(messages.get(0).getPayload())).isEqualTo("Payload1");

		assertThat(stompDecoder.getBufferSize()).isEqualTo(24);
		assertThat((int) stompDecoder.getExpectedContentLength()).isEqualTo(129);

		String chunk2 = "\nPayload2a";
		assertThatExceptionOfType(StompConversionException.class).isThrownBy(() ->
				stompDecoder.decode(toByteBuffer(chunk2)));
	}

	@Test
	void bufferSizeLimit() {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 10);
		String payload = "SEND\na:alpha\n\nMessage body";
		assertThatExceptionOfType(StompConversionException.class).isThrownBy(() ->
				stompDecoder.decode(toByteBuffer(payload)));
	}

	@Test
	void incompleteCommand() {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		String chunk = "MESSAG";

		List<Message<byte[]>> messages = stompDecoder.decode(toByteBuffer(chunk));
		assertThat(messages).isEmpty();
	}

	// SPR-13416

	@Test
	void incompleteHeaderWithPartialEscapeSequence() {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		String chunk = "SEND\na:long\\";

		List<Message<byte[]>> messages = stompDecoder.decode(toByteBuffer(chunk));
		assertThat(messages).isEmpty();
	}

	@Test
	void invalidEscapeSequence() {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		String payload = "SEND\na:alpha\\x\\n\nMessage body\0";
		assertThatExceptionOfType(StompConversionException.class).isThrownBy(() ->
				stompDecoder.decode(toByteBuffer(payload)));
	}

	@Test
	void invalidEscapeSequenceWithSingleSlashAtEndOfHeaderValue() {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		String payload = "SEND\na:alpha\\\n\nMessage body\0";
		assertThatExceptionOfType(StompConversionException.class).isThrownBy(() ->
				stompDecoder.decode(toByteBuffer(payload)));
	}

	private ByteBuffer toByteBuffer(String chunk) {
		return ByteBuffer.wrap(chunk.getBytes(StandardCharsets.UTF_8));
	}

}
