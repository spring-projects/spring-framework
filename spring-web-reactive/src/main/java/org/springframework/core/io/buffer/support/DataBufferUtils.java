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

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.Assert;

/**
 * @author Arjen Poutsma
 */
public abstract class DataBufferUtils {

	public static Flux<Byte> toPublisher(DataBuffer buffer) {
		Assert.notNull(buffer, "'buffer' must not be null");

		byte[] bytes1 = new byte[buffer.readableByteCount()];
		buffer.read(bytes1);

		Byte[] bytes2 = new Byte[bytes1.length];
		for (int i = 0; i < bytes1.length; i++) {
			bytes2[i] = bytes1[i];
		}
		return Flux.fromArray(bytes2);
	}

	public static InputStream toInputStream(Publisher<DataBuffer> publisher) {
		return new DataBufferPublisherInputStream(publisher);
	}

}
