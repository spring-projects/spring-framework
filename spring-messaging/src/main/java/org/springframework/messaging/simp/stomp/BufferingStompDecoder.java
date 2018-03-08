/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * An extension of {@link org.springframework.messaging.simp.stomp.StompDecoder}
 * that buffers content remaining in the input ByteBuffer after the parent
 * class has read all (complete) STOMP frames from it. The remaining content
 * represents an incomplete STOMP frame. When called repeatedly with additional
 * data, the decode method returns one or more messages or, if there is not
 * enough data still, continues to buffer.
 *
 * <p>A single instance of this decoder can be invoked repeatedly to read all
 * messages from a single stream (e.g. WebSocket session) as long as decoding
 * does not fail. If there is an exception, StompDecoder instance should not
 * be used any more as its internal state is not guaranteed to be consistent.
 * It is expected that the underlying session is closed at that point.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 * @see StompDecoder
 */
public class BufferingStompDecoder {

	private final StompDecoder stompDecoder;

	private final int bufferSizeLimit;

	private final Queue<ByteBuffer> chunks = new LinkedBlockingQueue<>();

	@Nullable
	private volatile Integer expectedContentLength;


	/**
	 * Create a new {@code BufferingStompDecoder} wrapping the given {@code StompDecoder}.
	 * @param stompDecoder the target decoder to wrap
	 * @param bufferSizeLimit the buffer size limit
	 */
	public BufferingStompDecoder(StompDecoder stompDecoder, int bufferSizeLimit) {
		Assert.notNull(stompDecoder, "StompDecoder is required");
		Assert.isTrue(bufferSizeLimit > 0, "Buffer size limit must be greater than 0");
		this.stompDecoder = stompDecoder;
		this.bufferSizeLimit = bufferSizeLimit;
	}


	/**
	 * Return the wrapped {@link StompDecoder}.
	 */
	public final StompDecoder getStompDecoder() {
		return this.stompDecoder;
	}

	/**
	 * Return the configured buffer size limit.
	 */
	public final int getBufferSizeLimit() {
		return this.bufferSizeLimit;
	}


	/**
	 * Decodes one or more STOMP frames from the given {@code ByteBuffer} into a
	 * list of {@link Message}s.
	 * <p>If there was enough data to parse a "content-length" header, then the
	 * value is used to determine how much more data is needed before a new
	 * attempt to decode is made.
	 * <p>If there was not enough data to parse the "content-length", or if there
	 * is "content-length" header, every subsequent call to decode attempts to
	 * parse again with all available data. Therefore the presence of a "content-length"
	 * header helps to optimize the decoding of large messages.
	 * @param newBuffer a buffer containing new data to decode
	 * @return decoded messages or an empty list
	 * @throws StompConversionException raised in case of decoding issues
	 */
	public List<Message<byte[]>> decode(ByteBuffer newBuffer) {
		this.chunks.add(newBuffer);
		checkBufferLimits();

		Integer contentLength = this.expectedContentLength;
		if (contentLength != null && getBufferSize() < contentLength) {
			return Collections.emptyList();
		}

		ByteBuffer bufferToDecode = assembleChunksAndReset();
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		List<Message<byte[]>> messages = this.stompDecoder.decode(bufferToDecode, headers);

		if (bufferToDecode.hasRemaining()) {
			this.chunks.add(bufferToDecode);
			this.expectedContentLength = StompHeaderAccessor.getContentLength(headers);
		}

		return messages;
	}

	private ByteBuffer assembleChunksAndReset() {
		ByteBuffer result;
		if (this.chunks.size() == 1) {
			result = this.chunks.remove();
		}
		else {
			result = ByteBuffer.allocate(getBufferSize());
			for (ByteBuffer partial : this.chunks) {
				result.put(partial);
			}
			result.flip();
		}
		this.chunks.clear();
		this.expectedContentLength = null;
		return result;
	}

	private void checkBufferLimits() {
		Integer contentLength = this.expectedContentLength;
		if (contentLength != null && contentLength > this.bufferSizeLimit) {
			throw new StompConversionException(
					"STOMP 'content-length' header value " + this.expectedContentLength +
					"  exceeds configured buffer size limit " + this.bufferSizeLimit);
		}
		if (getBufferSize() > this.bufferSizeLimit) {
			throw new StompConversionException("The configured STOMP buffer size limit of " +
					this.bufferSizeLimit + " bytes has been exceeded");
		}
	}

	/**
	 * Calculate the current buffer size.
	 */
	public int getBufferSize() {
		int size = 0;
		for (ByteBuffer buffer : this.chunks) {
			size = size + buffer.remaining();
		}
		return size;
	}

	/**
	 * Get the expected content length of the currently buffered, incomplete STOMP frame.
	 */
	@Nullable
	public Integer getExpectedContentLength() {
		return this.expectedContentLength;
	}

}
