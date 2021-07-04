/*
 * Copyright 2002-2018 the original author or authors.
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

import java.nio.ByteBuffer;
import java.util.List;

/**
 * A factory for {@link DataBuffer DataBuffers}, allowing for allocation and
 * wrapping of data buffers.
 *
 * @author Arjen Poutsma
 * @since 5.0
 * @see DataBuffer
 */
public interface DataBufferFactory {

	/**
	 * Allocate a data buffer of a default initial capacity. Depending on the
	 * underlying implementation and its configuration, this will be heap-based
	 * or direct buffer.
	 * @return the allocated buffer
	 */
	DataBuffer allocateBuffer();

	/**
	 * Allocate a data buffer of the given initial capacity. Depending on the
	 * underlying implementation and its configuration, this will be heap-based
	 * or direct buffer.
	 * @param initialCapacity the initial capacity of the buffer to allocate
	 * @return the allocated buffer
	 */
	DataBuffer allocateBuffer(int initialCapacity);

	/**
	 * Wrap the given {@link ByteBuffer} in a {@code DataBuffer}. Unlike
	 * {@linkplain #allocateBuffer(int) allocating}, wrapping does not use new memory.
	 * @param byteBuffer the NIO byte buffer to wrap
	 * @return the wrapped buffer
	 */
	DataBuffer wrap(ByteBuffer byteBuffer);

	/**
	 * Wrap the given {@code byte} array in a {@code DataBuffer}. Unlike
 	 * {@linkplain #allocateBuffer(int) allocating}, wrapping does not use new memory.
	 * @param bytes the byte array to wrap
	 * @return the wrapped buffer
	 */
	DataBuffer wrap(byte[] bytes);

	/**
	 * Return a new {@code DataBuffer} composed of the {@code dataBuffers} elements joined together.
	 * Depending on the implementation, the returned buffer may be a single buffer containing all
	 * data of the provided buffers, or it may be a true composite that contains references to the
	 * buffers.
	 * <p>Note that the given data buffers do <strong>not</strong> have to be released, as they are
	 * released as part of the returned composite.
	 * @param dataBuffers the data buffers to be composed
	 * @return a buffer that is composed from the {@code dataBuffers} argument
	 * @since 5.0.3
	 */
	DataBuffer join(List<? extends DataBuffer> dataBuffers);

}
