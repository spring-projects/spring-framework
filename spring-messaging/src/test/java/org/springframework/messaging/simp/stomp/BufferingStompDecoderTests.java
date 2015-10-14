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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.messaging.Message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link BufferingStompDecoder}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
public class BufferingStompDecoderTests {

	private final StompDecoder STOMP_DECODER = new StompDecoder();


	@Test
	public void basic() throws InterruptedException {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		String chunk = "SEND\na:alpha\n\nMessage body\0";

		List<Message<byte[]>> messages = stompDecoder.decode(toByteBuffer(chunk));
		assertEquals(1, messages.size());
		assertEquals("Message body", new String(messages.get(0).getPayload()));

		assertEquals(0, stompDecoder.getBufferSize());
		assertNull(stompDecoder.getExpectedContentLength());
	}

	@Test
	public void oneMessageInTwoChunks() throws InterruptedException {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		String chunk1 = "SEND\na:alpha\n\nMessage";
		String chunk2 = " body\0";

		List<Message<byte[]>> messages = stompDecoder.decode(toByteBuffer(chunk1));
		assertEquals(Collections.<Message<byte[]>>emptyList(), messages);

		messages = stompDecoder.decode(toByteBuffer(chunk2));
		assertEquals(1, messages.size());
		assertEquals("Message body", new String(messages.get(0).getPayload()));

		assertEquals(0, stompDecoder.getBufferSize());
		assertNull(stompDecoder.getExpectedContentLength());
	}

	@Test
	public void twoMessagesInOneChunk() throws InterruptedException {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		String chunk = "SEND\na:alpha\n\nPayload1\0" + "SEND\na:alpha\n\nPayload2\0";
		List<Message<byte[]>> messages = stompDecoder.decode(toByteBuffer(chunk));

		assertEquals(2, messages.size());
		assertEquals("Payload1", new String(messages.get(0).getPayload()));
		assertEquals("Payload2", new String(messages.get(1).getPayload()));

		assertEquals(0, stompDecoder.getBufferSize());
		assertNull(stompDecoder.getExpectedContentLength());
	}

	@Test
	public void oneFullAndOneSplitMessageContentLength() throws InterruptedException {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		int contentLength = "Payload2a-Payload2b".getBytes().length;
		String chunk1 = "SEND\na:alpha\n\nPayload1\0SEND\ncontent-length:" + contentLength + "\n";
		List<Message<byte[]>> messages = stompDecoder.decode(toByteBuffer(chunk1));

		assertEquals(1, messages.size());
		assertEquals("Payload1", new String(messages.get(0).getPayload()));

		assertEquals(23, stompDecoder.getBufferSize());
		assertEquals(contentLength, (int) stompDecoder.getExpectedContentLength());

		String chunk2 = "\nPayload2a";
		messages = stompDecoder.decode(toByteBuffer(chunk2));

		assertEquals(0, messages.size());
		assertEquals(33, stompDecoder.getBufferSize());
		assertEquals(contentLength, (int) stompDecoder.getExpectedContentLength());

		String chunk3 = "-Payload2b\0";
		messages = stompDecoder.decode(toByteBuffer(chunk3));

		assertEquals(1, messages.size());
		assertEquals("Payload2a-Payload2b", new String(messages.get(0).getPayload()));
		assertEquals(0, stompDecoder.getBufferSize());
		assertNull(stompDecoder.getExpectedContentLength());
	}

	@Test
	public void oneFullAndOneSplitMessageNoContentLength() throws InterruptedException {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		String chunk1 = "SEND\na:alpha\n\nPayload1\0SEND\na:alpha\n";
		List<Message<byte[]>> messages = stompDecoder.decode(toByteBuffer(chunk1));

		assertEquals(1, messages.size());
		assertEquals("Payload1", new String(messages.get(0).getPayload()));

		assertEquals(13, stompDecoder.getBufferSize());
		assertNull(stompDecoder.getExpectedContentLength());

		String chunk2 = "\nPayload2a";
		messages = stompDecoder.decode(toByteBuffer(chunk2));

		assertEquals(0, messages.size());
		assertEquals(23, stompDecoder.getBufferSize());
		assertNull(stompDecoder.getExpectedContentLength());

		String chunk3 = "-Payload2b\0";
		messages = stompDecoder.decode(toByteBuffer(chunk3));

		assertEquals(1, messages.size());
		assertEquals("Payload2a-Payload2b", new String(messages.get(0).getPayload()));
		assertEquals(0, stompDecoder.getBufferSize());
		assertNull(stompDecoder.getExpectedContentLength());
	}

	@Test
	public void oneFullAndOneSplitWithContentLengthExceedingBufferSize() throws InterruptedException {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		String chunk1 = "SEND\na:alpha\n\nPayload1\0SEND\ncontent-length:129\n";
		List<Message<byte[]>> messages = stompDecoder.decode(toByteBuffer(chunk1));

		assertEquals("We should have gotten the 1st message", 1, messages.size());
		assertEquals("Payload1", new String(messages.get(0).getPayload()));

		assertEquals(24, stompDecoder.getBufferSize());
		assertEquals(129, (int) stompDecoder.getExpectedContentLength());

		try {
			String chunk2 = "\nPayload2a";
			stompDecoder.decode(toByteBuffer(chunk2));
			fail("Expected exception");
		}
		catch (StompConversionException ex) {
			// expected
		}
	}

	@Test(expected = StompConversionException.class)
	public void bufferSizeLimit() {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 10);
		String payload = "SEND\na:alpha\n\nMessage body";
		stompDecoder.decode(toByteBuffer(payload));
	}

	@Test
	public void incompleteCommand() {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		String chunk = "MESSAG";

		List<Message<byte[]>> messages = stompDecoder.decode(toByteBuffer(chunk));
		assertEquals(0, messages.size());
	}

	// SPR-13416

	@Test
	public void incompleteHeaderWithPartialEscapeSequence() throws Exception {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		String chunk = "SEND\na:long\\";

		List<Message<byte[]>> messages = stompDecoder.decode(toByteBuffer(chunk));
		assertEquals(0, messages.size());
	}

	@Test(expected = StompConversionException.class)
	public void invalidEscapeSequence() {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		String payload = "SEND\na:alpha\\x\\n\nMessage body\0";
		stompDecoder.decode(toByteBuffer(payload));
	}

	@Test(expected = StompConversionException.class)
	public void invalidEscapeSequenceWithSingleSlashAtEndOfHeaderValue() {
		BufferingStompDecoder stompDecoder = new BufferingStompDecoder(STOMP_DECODER, 128);
		String payload = "SEND\na:alpha\\\n\nMessage body\0";
		stompDecoder.decode(toByteBuffer(payload));
	}

	private ByteBuffer toByteBuffer(String chunk) {
		return ByteBuffer.wrap(chunk.getBytes(Charset.forName("UTF-8")));
	}

}
