/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.io.buffer.support;

import java.nio.charset.Charset;

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

	/**
	 * Dump all the bytes in the given data buffer, and returns them as a string.
	 * <p>Note that this method reads the entire buffer into the heap,  which might
	 * consume a lot of memory.
	 * @param buffer the data buffer to dump the string contents of
	 * @param charset the charset of the data
	 * @return the string representation of the given data buffer
	 */
	public static String dumpString(DataBuffer buffer, Charset charset) {
		Assert.notNull(charset, "'charset' must not be null");
		byte[] bytes = dumpBytes(buffer);
		return new String(bytes, charset);
	}

}
