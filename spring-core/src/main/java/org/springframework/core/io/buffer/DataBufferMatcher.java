/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.core.io.buffer;

/**
 * Contract to find delimiter(s) against one or more data buffers that can
 * be passed one at a time to the {@link #match(DataBuffer)} method.
 *
 * @author Arjen Poutsma
 * @since 7.1
 * @see #of(byte[]...)
 * @see #match(DataBuffer)
 */
public interface DataBufferMatcher {

	/**
	 * Return a {@link DataBufferMatcher} for the given delimiters.
	 * @param delimiters the delimiter bytes to find
	 * @return the matcher
	 */
	static DataBufferMatcher of(byte[]... delimiters) {
		return DataBuffers.matcher(delimiters);
	}

	/**
	 * Find the first matching delimiter and return the index of the last
	 * byte of the delimiter, or {@code -1} if not found.
	 */
	int match(DataBuffer dataBuffer);

	/**
	 * Return the delimiter from the last invocation of {@link #match(DataBuffer)}.
	 */
	byte[] delimiter();

	/**
	 * Reset the state of this matcher.
	 */
	void reset();

}
