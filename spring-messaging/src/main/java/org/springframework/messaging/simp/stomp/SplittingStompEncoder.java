/*
 * Copyright 2024-present the original author or authors.
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
 * Uses a {@link StompEncoder} to encode a message and splits it into parts no
 * larger than the configured
 * {@linkplain #SplittingStompEncoder(StompEncoder, int) buffer size limit}.
 *
 * @author Injae Kim
 * @author Rossen Stoyanchev
 * @since 6.2
 * @see StompEncoder
 */
public class SplittingStompEncoder {

	private final StompEncoder encoder;

	private final int bufferSizeLimit;


	/**
	 * Create a new {@code SplittingStompEncoder}.
	 * @param encoder the {@link StompEncoder} to use
	 * @param bufferSizeLimit the buffer size limit
	 */
	public SplittingStompEncoder(StompEncoder encoder, int bufferSizeLimit) {
		Assert.notNull(encoder, "StompEncoder is required");
		Assert.isTrue(bufferSizeLimit > 0, "Buffer size limit must be greater than 0");
		this.encoder = encoder;
		this.bufferSizeLimit = bufferSizeLimit;
	}


	/**
	 * Encode the given payload and headers to a STOMP frame, and split it into a
	 * list of parts based on the configured buffer size limit. A part does not
	 * end inside a UTF-8 character unless the limit is too small to hold it.
	 * @param headers the STOMP message headers
	 * @param payload the STOMP message payload
	 * @return the parts of the encoded STOMP message
	 */
	public List<byte[]> encode(Map<String, Object> headers, byte[] payload) {
		byte[] result = this.encoder.encode(headers, payload);
		int length = result.length;

		if (length <= this.bufferSizeLimit) {
			return List.of(result);
		}

		List<byte[]> frames = new ArrayList<>();
		int start = 0;
		while (start < length) {
			int end = Math.min(start + this.bufferSizeLimit, length);
			// Do not end a part inside a UTF-8 character, as a part may be sent as text
			int cut = end;
			while (cut > start && cut < length && end - cut < 3 && (result[cut] & 0xC0) == 0x80) {
				cut--;
			}
			if (cut > start) {
				end = cut;
			}
			frames.add(Arrays.copyOfRange(result, start, end));
			start = end;
		}
		return frames;
	}

}
