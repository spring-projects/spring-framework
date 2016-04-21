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

import java.io.InputStream;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.support.DataBufferUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * A decoder for {@link Resource}s.
 *
 * @author Arjen Poutsma
 */
public class ResourceDecoder extends AbstractDecoder<Resource> {

	public ResourceDecoder() {
		super(MimeTypeUtils.ALL);
	}

	@Override
	public boolean canDecode(ResolvableType type, MimeType mimeType, Object... hints) {
		Class<?> clazz = type.getRawClass();
		return (InputStreamResource.class.equals(clazz) ||
				clazz.isAssignableFrom(ByteArrayResource.class)) &&
				super.canDecode(type, mimeType, hints);
	}

	@Override
	public Flux<Resource> decode(Publisher<DataBuffer> inputStream, ResolvableType type,
			MimeType mimeType, Object... hints) {
		Class<?> clazz = type.getRawClass();

		Flux<DataBuffer> body = Flux.from(inputStream);

		if (InputStreamResource.class.equals(clazz)) {
			InputStream is = DataBufferUtils.toInputStream(body);
			return Flux.just(new InputStreamResource(is));
		}
		else if (clazz.isAssignableFrom(ByteArrayResource.class)) {
			Mono<DataBuffer> singleBuffer = body.reduce(DataBuffer::write);
			return Flux.from(singleBuffer.map(buffer -> {
				byte[] bytes = new byte[buffer.readableByteCount()];
				buffer.read(bytes);
				return new ByteArrayResource(bytes);
			}));
		}
		else {
			return Flux.error(new IllegalStateException(
					"Unsupported resource class: " + clazz));
		}
	}
}
