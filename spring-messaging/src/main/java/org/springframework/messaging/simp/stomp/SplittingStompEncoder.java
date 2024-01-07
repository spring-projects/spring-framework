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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * An extension of {@link org.springframework.messaging.simp.stomp.StompEncoder}
 * that splits the STOMP message to multiple incomplete STOMP frames
 * when the encoded bytes length exceeds {@link SplittingStompEncoder#bufferSizeLimit}.
 *
 * @author Injae Kim
 * @since 6.2
 * @see StompEncoder
 */
public class SplittingStompEncoder {

	private final StompEncoder encoder;

	private final int bufferSizeLimit;

	public SplittingStompEncoder(StompEncoder encoder, int bufferSizeLimit) {
		Assert.notNull(encoder, "StompEncoder is required");
		Assert.isTrue(bufferSizeLimit > 0, "Buffer size limit must be greater than 0");
		this.encoder = encoder;
		this.bufferSizeLimit = bufferSizeLimit;
	}

	/**
	 * Encodes the given payload and headers into a list of one or more {@code byte[]}s.
	 * @param headers the headers
	 * @param payload the payload
	 * @return the list of one or more encoded messages
	 */
	public List<byte[]> encode(Map<String, Object> headers, byte[] payload) {
		byte[] result = this.encoder.encode(headers, payload);
		int length = result.length;

		if (length <= this.bufferSizeLimit) {
			return List.of(result);
		}

		List<byte[]> frames = new ArrayList<>();
		for (int i = 0; i < length; i += this.bufferSizeLimit) {
			frames.add(Arrays.copyOfRange(result, i, Math.min(i + this.bufferSizeLimit, length)));
		}
		return frames;
	}
}
