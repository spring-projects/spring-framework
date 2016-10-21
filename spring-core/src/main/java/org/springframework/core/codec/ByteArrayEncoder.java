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

package org.springframework.core.codec;

import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Encoder for {@code byte} arrays.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class ByteArrayEncoder extends AbstractEncoder<byte[]> {

	public ByteArrayEncoder() {
		super(MimeTypeUtils.ALL);
	}


	@Override
	public boolean canEncode(ResolvableType elementType, MimeType mimeType) {
		Class<?> clazz = elementType.getRawClass();
		return (super.canEncode(elementType, mimeType) && byte[].class.isAssignableFrom(clazz));
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<? extends byte[]> inputStream,
			DataBufferFactory bufferFactory, ResolvableType elementType, MimeType mimeType,
			Map<String, Object> hints) {

		return Flux.from(inputStream).map(bufferFactory::wrap);
	}

}
