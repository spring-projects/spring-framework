/*
 * Copyright 2002-2022 the original author or authors.
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
 * Extension of {@link DataBuffer} that allows for buffers that can be used
 * in a {@code try}-with-resources statement.

 * @author Arjen Poutsma
 * @since 6.0
 */
public interface CloseableDataBuffer extends DataBuffer, AutoCloseable {

	/**
	 * Closes this data buffer, freeing any resources.
	 * @throws IllegalStateException if this buffer has already been closed
	 */
	@Override
	void close();

}
