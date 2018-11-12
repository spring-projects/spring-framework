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
import java.util.ArrayList;
import java.util.List;

import org.junit.After;

import org.springframework.util.Assert;

/**
 * Implementation of the {@code DataBufferFactory} interface that keep track of memory leaks.
 * Useful for unit tests that handle data buffers. Simply call {@link #checkForLeaks()} in
 * a JUnit {@link After} method, and any buffers have not been released will result in an
 * {@link AssertionError}.
 * <pre class="code">
 * public class MyUnitTest {
 *
 * 	private final LeakAwareDataBufferFactory bufferFactory =
 * 	  new LeakAwareDataBufferFactory();
 *
 *  &#064;Test
 * 	public void doSomethingWithBufferFactory() {
 * 		...
 * 	}
 *
 * 	&#064;After
 * 	public void checkForLeaks() {
 * 		bufferFactory.checkForLeaks();
 * 	}
 *
 * }
 * </pre>
 * @author Arjen Poutsma
 */
public class LeakAwareDataBufferFactory implements DataBufferFactory {

	private final DataBufferFactory delegate;

	private final List<LeakAwareDataBuffer> created = new ArrayList<>();


	/**
	 * Creates a new {@code LeakAwareDataBufferFactory} by wrapping a
	 * {@link DefaultDataBufferFactory}.
	 */
	public LeakAwareDataBufferFactory() {
		this(new DefaultDataBufferFactory());
	}

	/**
	 * Creates a new {@code LeakAwareDataBufferFactory} by wrapping the given delegate.
	 * @param delegate the delegate buffer factory to wrap.
	 */
	public LeakAwareDataBufferFactory(DataBufferFactory delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}

	/**
	 * Checks whether all of the data buffers allocated by this factory have also been released.
	 * If not, then an {@link AssertionError} is thrown. Typically used from a JUnit {@link After}
	 * method.
	 */
	public void checkForLeaks() {
		this.created.stream()
				.filter(LeakAwareDataBuffer::isAllocated)
				.findFirst()
				.map(LeakAwareDataBuffer::leakError)
				.ifPresent(leakError -> {
					throw leakError;
				});
	}

	@Override
	public LeakAwareDataBuffer allocateBuffer() {
		LeakAwareDataBuffer dataBuffer =
				new LeakAwareDataBuffer(this.delegate.allocateBuffer(), this);
		this.created.add(dataBuffer);
		return dataBuffer;
	}

	@Override
	public LeakAwareDataBuffer allocateBuffer(int initialCapacity) {
		LeakAwareDataBuffer dataBuffer =
				new LeakAwareDataBuffer(this.delegate.allocateBuffer(initialCapacity), this);
		this.created.add(dataBuffer);
		return dataBuffer;
	}

	@Override
	public DataBuffer wrap(ByteBuffer byteBuffer) {
		return this.delegate.wrap(byteBuffer);
	}

	@Override
	public DataBuffer wrap(byte[] bytes) {
		return this.delegate.wrap(bytes);
	}

	@Override
	public DataBuffer join(List<? extends DataBuffer> dataBuffers) {
		return new LeakAwareDataBuffer(this.delegate.join(dataBuffers), this);
	}

}
