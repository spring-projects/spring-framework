/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.util.Assert;

/**
 * DataBuffer implementation created by {@link LeakAwareDataBufferFactory}.
 *
 * @author Arjen Poutsma
 */
class LeakAwareDataBuffer extends DataBufferWrapper implements PooledDataBuffer {

	private final AssertionError leakError;

	private final LeakAwareDataBufferFactory dataBufferFactory;


	LeakAwareDataBuffer(DataBuffer delegate, LeakAwareDataBufferFactory dataBufferFactory) {
		super(delegate);
		Assert.notNull(dataBufferFactory, "DataBufferFactory must not be null");
		this.dataBufferFactory = dataBufferFactory;
		this.leakError = createLeakError(delegate);
	}

	private static AssertionError createLeakError(DataBuffer delegate) {
		String message = String.format("DataBuffer leak detected: {%s} has not been released.%n" +
				"Stack trace of buffer allocation statement follows:",
				delegate);
		AssertionError result = new AssertionError(message);
		// remove first four irrelevant stack trace elements
		StackTraceElement[] oldTrace = result.getStackTrace();
		StackTraceElement[] newTrace = new StackTraceElement[oldTrace.length - 4];
		System.arraycopy(oldTrace, 4, newTrace, 0, oldTrace.length - 4);
		result.setStackTrace(newTrace);
		return result;
	}

	AssertionError leakError() {
		return this.leakError;
	}


	@Override
	public boolean isAllocated() {
		DataBuffer delegate = dataBuffer();
		return delegate instanceof PooledDataBuffer &&
				((PooledDataBuffer) delegate).isAllocated();
	}

	@Override
	public PooledDataBuffer retain() {
		DataBuffer delegate = dataBuffer();
		if (delegate instanceof PooledDataBuffer) {
			((PooledDataBuffer) delegate).retain();
		}
		return this;
	}

	@Override
	public boolean release() {
		DataBuffer delegate = dataBuffer();
		if (delegate instanceof PooledDataBuffer) {
			((PooledDataBuffer) delegate).release();
		}
		return isAllocated();
	}

	@Override
	public LeakAwareDataBufferFactory factory() {
		return this.dataBufferFactory;
	}

	@Override
	public String toString() {
		return String.format("LeakAwareDataBuffer (%s)", dataBuffer());
	}
}
