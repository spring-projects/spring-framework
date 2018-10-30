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

package org.springframework.core.io.buffer;

import java.nio.ByteBuffer;
import java.util.List;

import org.springframework.util.Assert;

/**
 * Default implementation of the {@code DataBufferFactory} interface. Allows for
 * specification of the default initial capacity at construction time, as well
 * as whether heap-based or direct buffers are to be preferred.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class DefaultDataBufferFactory implements DataBufferFactory {

	/**
	 * The default capacity when none is specified.
	 * @see #DefaultDataBufferFactory()
	 * @see #DefaultDataBufferFactory(boolean)
	 */
	public static final int DEFAULT_INITIAL_CAPACITY = 256;


	private final boolean preferDirect;

	private final int defaultInitialCapacity;


	/**
	 * Creates a new {@code DefaultDataBufferFactory} with default settings.
	 */
	public DefaultDataBufferFactory() {
		this(false);
	}

	/**
	 * Creates a new {@code DefaultDataBufferFactory}, indicating whether direct
	 * buffers should be created by {@link #allocateBuffer()} and
	 * {@link #allocateBuffer(int)}.
	 * @param preferDirect {@code true} if direct buffers are to be preferred;
	 * {@code false} otherwise
	 */
	public DefaultDataBufferFactory(boolean preferDirect) {
		this(preferDirect, DEFAULT_INITIAL_CAPACITY);
	}

	/**
	 * Creates a new {@code DefaultDataBufferFactory}, indicating whether direct
	 * buffers should be created by {@link #allocateBuffer()} and
	 * {@link #allocateBuffer(int)}, and what the capacity is to be used for
	 * {@link #allocateBuffer()}.
	 * @param preferDirect {@code true} if direct buffers are to be preferred;
	 * {@code false} otherwise
	 */
	public DefaultDataBufferFactory(boolean preferDirect, int defaultInitialCapacity) {
		Assert.isTrue(defaultInitialCapacity > 0, "'defaultInitialCapacity' should be larger than 0");
		this.preferDirect = preferDirect;
		this.defaultInitialCapacity = defaultInitialCapacity;
	}


	@Override
	public DefaultDataBuffer allocateBuffer() {
		return allocateBuffer(this.defaultInitialCapacity);
	}

	@Override
	public DefaultDataBuffer allocateBuffer(int initialCapacity) {
		ByteBuffer byteBuffer = (this.preferDirect ?
				ByteBuffer.allocateDirect(initialCapacity) :
				ByteBuffer.allocate(initialCapacity));

		return DefaultDataBuffer.fromEmptyByteBuffer(this, byteBuffer);
	}

	@Override
	public DefaultDataBuffer wrap(ByteBuffer byteBuffer) {
		ByteBuffer sliced = byteBuffer.slice();
		return DefaultDataBuffer.fromFilledByteBuffer(this, sliced);
	}

	@Override
	public DataBuffer wrap(byte[] bytes) {
		ByteBuffer wrapper = ByteBuffer.wrap(bytes);
		return DefaultDataBuffer.fromFilledByteBuffer(this, wrapper);
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation creates a single {@link DefaultDataBuffer} to contain the data
	 * in {@code dataBuffers}.
	 */
	@Override
	public DataBuffer join(List<? extends DataBuffer> dataBuffers) {
		Assert.notEmpty(dataBuffers, "'dataBuffers' must not be empty");

		int capacity = dataBuffers.stream()
				.mapToInt(DataBuffer::readableByteCount)
				.sum();
		DefaultDataBuffer dataBuffer = allocateBuffer(capacity);
		DataBuffer result = dataBuffers.stream()
				.map(o -> (DataBuffer) o)
				.reduce(dataBuffer, DataBuffer::write);
		dataBuffers.forEach(DataBufferUtils::release);
		return result;
	}

	@Override
	public String toString() {
		return "DefaultDataBufferFactory (preferDirect=" + this.preferDirect + ")";
	}

}
