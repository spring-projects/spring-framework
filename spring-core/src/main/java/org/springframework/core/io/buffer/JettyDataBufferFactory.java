/*
 * Copyright 2002-2024 the original author or authors.
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

import org.eclipse.jetty.io.Content;

/**
 * Implementation of the {@code DataBufferFactory} interface that creates
 * {@link JettyDataBuffer} instances.
 *
 * @author Arjen Poutsma
 * @since 6.2
 */
public class JettyDataBufferFactory implements DataBufferFactory {

	private final DefaultDataBufferFactory delegate;


	/**
	 * Creates a new {@code JettyDataBufferFactory} with default settings.
	 */
	public JettyDataBufferFactory() {
		this(false);
	}

	/**
	 * Creates a new {@code JettyDataBufferFactory}, indicating whether direct
	 * buffers should be created by {@link #allocateBuffer()} and
	 * {@link #allocateBuffer(int)}.
	 * @param preferDirect {@code true} if direct buffers are to be preferred;
	 * {@code false} otherwise
	 */
	public JettyDataBufferFactory(boolean preferDirect) {
		this(preferDirect, DefaultDataBufferFactory.DEFAULT_INITIAL_CAPACITY);
	}

	/**
	 * Creates a new {@code JettyDataBufferFactory}, indicating whether direct
	 * buffers should be created by {@link #allocateBuffer()} and
	 * {@link #allocateBuffer(int)}, and what the capacity is to be used for
	 * {@link #allocateBuffer()}.
	 * @param preferDirect {@code true} if direct buffers are to be preferred;
	 * {@code false} otherwise
	 */
	public JettyDataBufferFactory(boolean preferDirect, int defaultInitialCapacity) {
		this.delegate = new DefaultDataBufferFactory(preferDirect, defaultInitialCapacity);
	}


	@Override
	@Deprecated
	public JettyDataBuffer allocateBuffer() {
		DefaultDataBuffer delegate = this.delegate.allocateBuffer();
		return new JettyDataBuffer(this, delegate);
	}

	@Override
	public JettyDataBuffer allocateBuffer(int initialCapacity) {
		DefaultDataBuffer delegate = this.delegate.allocateBuffer(initialCapacity);
		return new JettyDataBuffer(this, delegate);
	}

	@Override
	public JettyDataBuffer wrap(ByteBuffer byteBuffer) {
		DefaultDataBuffer delegate = this.delegate.wrap(byteBuffer);
		return new JettyDataBuffer(this, delegate);
	}

	@Override
	public JettyDataBuffer wrap(byte[] bytes) {
		DefaultDataBuffer delegate = this.delegate.wrap(bytes);
		return new JettyDataBuffer(this, delegate);
	}

	public JettyDataBuffer wrap(Content.Chunk chunk) {
		ByteBuffer byteBuffer = chunk.getByteBuffer();
		DefaultDataBuffer delegate = this.delegate.wrap(byteBuffer);
		return new JettyDataBuffer(this, delegate, chunk);
	}

	@Override
	public JettyDataBuffer join(List<? extends DataBuffer> dataBuffers) {
		DefaultDataBuffer delegate = this.delegate.join(dataBuffers);
		return new JettyDataBuffer(this, delegate);
	}

	@Override
	public boolean isDirect() {
		return this.delegate.isDirect();
	}
}
