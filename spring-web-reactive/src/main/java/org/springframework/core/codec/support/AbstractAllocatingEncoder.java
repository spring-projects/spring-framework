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

package org.springframework.core.codec.support;

import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * @author Arjen Poutsma
 */
public abstract class AbstractAllocatingEncoder<T> extends AbstractEncoder<T> {

	private final DataBufferAllocator allocator;

	public AbstractAllocatingEncoder(DataBufferAllocator allocator,
			MimeType... supportedMimeTypes) {
		super(supportedMimeTypes);
		Assert.notNull(allocator, "'allocator' must not be null");

		this.allocator = allocator;
	}

	public DataBufferAllocator allocator() {
		return allocator;
	}

}
