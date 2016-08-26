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

import java.nio.ByteBuffer;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Decoder for {@link ByteBuffer}s.
 * <p>ByteBuffer的解码器
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 */
public class ByteBufferDecoder extends AbstractDecoder<ByteBuffer> {

	public ByteBufferDecoder() {
		super(MimeTypeUtils.ALL);
	}


	@Override
	public boolean canDecode(ResolvableType elementType, MimeType mimeType, Object... hints) {
		Class<?> clazz = elementType.getRawClass();
		return (super.canDecode(elementType, mimeType, hints) && ByteBuffer.class.isAssignableFrom(clazz));
	}

	@Override
	public Flux<ByteBuffer> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			MimeType mimeType, Object... hints) {

		return Flux.from(inputStream).map((dataBuffer) -> {
			ByteBuffer copy = ByteBuffer.allocate(dataBuffer.readableByteCount());
			copy.put(dataBuffer.asByteBuffer());
			copy.flip();
			DataBufferUtils.release(dataBuffer);
			return copy;
		});
	}

}
