/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.testfixture.io.buffer;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.Assert;

/**
 * Utility class for working with {@link DataBuffer}s in tests.
 *
 * <p>Note that this class is in the {@code test} tree of the project:
 * the methods contained herein are not suitable for production code bases.
 *
 * @author Arjen Poutsma
 */
public abstract class DataBufferTestUtils {

	/**
	 * Dump all the bytes in the given data buffer, and returns them as a byte array.
	 * <p>Note that this method reads the entire buffer into the heap,  which might
	 * consume a lot of memory.
	 * @param buffer the data buffer to dump the bytes of
	 * @return the bytes in the given data buffer
	 */
	public static byte[] dumpBytes(DataBuffer buffer) {
		Assert.notNull(buffer, "'buffer' must not be null");
		byte[] bytes = new byte[buffer.readableByteCount()];
		buffer.read(bytes);
		return bytes;
	}

}
