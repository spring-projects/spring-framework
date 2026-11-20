/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.io.buffer;

/**
 * Contract to find delimiter(s) against one or more data buffers that can
 * be passed one at a time to the {@link #match(DataBuffer)} method.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
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
