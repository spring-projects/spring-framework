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

package org.springframework.core.io.buffer;

import java.nio.ByteBuffer;

/**
 * Default implementation of the {@code DataBufferAllocator} interface.
 *
 * <p>This class is rather limited; consider using Netty's
 * {@link io.netty.buffer.ByteBuf} and {@link NettyDataBuffer} for a more comprehensive
 * byte buffer.
 * @author Arjen Poutsma
 */
public class DefaultDataBufferAllocator implements DataBufferAllocator {

	public static final int DEFAULT_INITIAL_CAPACITY = 256;


	private final boolean preferDirect;

	/**
	 * Creates a new {@code DefaultDataBufferAllocator} with default settings.
	 */
	public DefaultDataBufferAllocator() {
		this(false);
	}

	/**
	 * Creates a new {@code DefaultDataBufferAllocator}, indicating whether direct buffers
	 * should be created by {@link #allocateBuffer(int)}.
	 * @param preferDirect {@code true} if direct buffers are to be preferred; {@code
	 * false} otherwise
	 */
	public DefaultDataBufferAllocator(boolean preferDirect) {
		this.preferDirect = preferDirect;
	}

	@Override
	public DataBuffer allocateBuffer() {
		return allocateBuffer(DEFAULT_INITIAL_CAPACITY);
	}

	@Override
	public DefaultDataBuffer allocateBuffer(int initialCapacity) {
		return preferDirect ? allocateDirectBuffer(initialCapacity) :
				allocateHeapBuffer(initialCapacity);
	}

	@Override
	public DefaultDataBuffer allocateHeapBuffer(int initialCapacity) {
		return new DefaultDataBuffer(ByteBuffer.allocate(initialCapacity));
	}

	@Override
	public DefaultDataBuffer allocateDirectBuffer(int initialCapacity) {
		return new DefaultDataBuffer(ByteBuffer.allocateDirect(initialCapacity));
	}

	@Override
	public DataBuffer wrap(ByteBuffer byteBuffer) {
		ByteBuffer sliced = byteBuffer.slice();
		return new DefaultDataBuffer(sliced, 0, byteBuffer.remaining());
	}

	@Override
	public String toString() {
		return "DefaultDataBufferFactory";
	}

}
