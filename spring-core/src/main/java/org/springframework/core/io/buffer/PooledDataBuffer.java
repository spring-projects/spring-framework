/*
 * Copyright 2002-2020 the original author or authors.
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
 * Extension of {@link DataBuffer} that allows for buffer that share
 * a memory pool. Introduces methods for reference counting.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface PooledDataBuffer extends DataBuffer {

	/**
	 * Return {@code true} if this buffer is allocated;
	 * {@code false} if it has been deallocated.
	 * @since 5.1
	 */
	boolean isAllocated();

	/**
	 * Increase the reference count for this buffer by one.
	 * @return this buffer
	 */
	PooledDataBuffer retain();

	/**
	 * Associate the given hint with the data buffer for debugging purposes.
	 * @return this buffer
	 * @since 5.3.2
	 */
	PooledDataBuffer touch(Object hint);

	/**
	 * Decrease the reference count for this buffer by one,
	 * and deallocate it once the count reaches zero.
	 * @return {@code true} if the buffer was deallocated;
	 * {@code false} otherwise
	 */
	boolean release();

}
