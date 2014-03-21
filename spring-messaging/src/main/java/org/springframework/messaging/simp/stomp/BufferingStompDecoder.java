/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.simp.stomp;


import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * A an extension of {@link org.springframework.messaging.simp.stomp.StompDecoder}
 * that chunks any bytes remaining after a single full STOMP frame has been read.
 * The remaining bytes may contain more STOMP frames or an incomplete STOMP frame.
 *
 * <p>Similarly if there is not enough content for a full STOMP frame, the content
 * is buffered until more input is received. That means the
 * {@link #decode(java.nio.ByteBuffer)} effectively never returns {@code null} as
 * the parent class does.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
public class BufferingStompDecoder extends StompDecoder {

	private final int bufferSizeLimit;

	private final Queue<ByteBuffer> chunks = new LinkedBlockingQueue<ByteBuffer>();

	private volatile Integer expectedContentLength;


	public BufferingStompDecoder(int bufferSizeLimit) {
		Assert.isTrue(bufferSizeLimit > 0, "Buffer size must be greater than 0");
		this.bufferSizeLimit = bufferSizeLimit;
	}


	public int getBufferSizeLimit() {
		return this.bufferSizeLimit;
	}

	public int getBufferSize() {
		int size = 0;
		for (ByteBuffer buffer : this.chunks) {
			size = size + buffer.remaining();
		}
		return size;
	}

	public Integer getExpectedContentLength() {
		return this.expectedContentLength;
	}


	@Override
	public List<Message<byte[]>> decode(ByteBuffer newData) {

		this.chunks.add(newData);

		checkBufferLimits();

		if (getExpectedContentLength() != null && getBufferSize() < this.expectedContentLength) {
			return Collections.<Message<byte[]>>emptyList();
		}

		ByteBuffer buffer = assembleChunksAndReset();

		MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
		List<Message<byte[]>> messages = decode(buffer, headers);

		if (buffer.hasRemaining()) {
			this.chunks.add(buffer);
			this.expectedContentLength = getContentLength(headers);
		}

		return messages;
	}

	private void checkBufferLimits() {
		if (getExpectedContentLength() != null) {
			if (getExpectedContentLength() > getBufferSizeLimit()) {
				throw new StompConversionException(
						"The 'content-length' header " + getExpectedContentLength() +
								"  exceeds the configured message buffer size limit " + getBufferSizeLimit());
			}
		}
		if (getBufferSize() > getBufferSizeLimit()) {
			throw new StompConversionException("The configured stomp frame buffer size limit of " +
					getBufferSizeLimit() + " bytes has been exceeded");

		}
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

}
