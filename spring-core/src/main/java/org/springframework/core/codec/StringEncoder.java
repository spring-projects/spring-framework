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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.util.MimeType;

/**
 * Encode from a String stream to a bytes stream.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see StringDecoder
 */
public class StringEncoder extends AbstractEncoder<String> {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	public StringEncoder() {
		super(new MimeType("text", "plain", DEFAULT_CHARSET));
	}


	@Override
	public boolean canEncode(ResolvableType elementType, MimeType mimeType, Object... hints) {
		Class<?> clazz = elementType.getRawClass();
		return (super.canEncode(elementType, mimeType, hints) && String.class.equals(clazz));
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<? extends String> inputStream,
			DataBufferFactory bufferFactory, ResolvableType elementType, MimeType mimeType,
			Object... hints) {
		Charset charset;
		if (mimeType != null && mimeType.getCharset() != null) {
			charset = mimeType.getCharset();
		}
		else {
			 charset = DEFAULT_CHARSET;
		}
		return Flux.from(inputStream).map(s -> {
			byte[] bytes = s.getBytes(charset);
			DataBuffer dataBuffer = bufferFactory.allocateBuffer(bytes.length);
			dataBuffer.write(bytes);
			return dataBuffer;
		});
	}

}
