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

import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Enumeration;
import java.util.Iterator;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.Assert;

/**
 * Utility class for working with {@link DataBuffer}s.
 *
 * @author Arjen Poutsma
 */
public abstract class DataBufferUtils {

	/**
	 * Returns the given {@link DataBuffer} as a {@link Flux} of bytes.
	 * @param buffer the buffer to return the bytes of
	 * @return the bytes as a flux
	 */
	public static Flux<Byte> toPublisher(DataBuffer buffer) {
		Assert.notNull(buffer, "'buffer' must not be null");

		byte[] bytes = new byte[buffer.readableByteCount()];
		buffer.read(bytes);

		Byte[] bytesObjects = box(bytes);

		return Flux.fromArray(bytesObjects);
	}

	private static Byte[] box(byte[] bytes) {
		Byte[] bytesObjects = new Byte[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			bytesObjects[i] = bytes[i];
		}
		return bytesObjects;
	}

	/**
	 * Returns the given data buffer publisher as a blocking input stream, streaming over
	 * all underlying buffers when available.
	 * @param publisher the publisher to create the input stream for
	 * @return the input stream
	 */
	public static InputStream toInputStream(Publisher<DataBuffer> publisher) {
		Iterable<InputStream> streams = Flux.from(publisher).
				map(DataBuffer::asInputStream).
				toIterable(1);

		Enumeration<InputStream> enumeration =
				new IteratorEnumeration<InputStream>(streams.iterator());

		return new SequenceInputStream(enumeration);
	}

	/**
	 * Enumeration wrapping an Iterator.
	 */
	// TODO: move to CollectionUtils when we merge with Spring Framework?
	private static class IteratorEnumeration<T> implements Enumeration<T> {

		private final Iterator<T> iterator;

		public IteratorEnumeration(Iterator<T> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasMoreElements() {
			return this.iterator.hasNext();
		}

		@Override
		public T nextElement() {
			return this.iterator.next();
		}
	}

}
