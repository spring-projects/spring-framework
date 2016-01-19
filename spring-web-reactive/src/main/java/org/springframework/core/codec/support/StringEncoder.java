/*
 * Copyright 2002-2015 the original author or authors.
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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.util.MimeType;

/**
 * Encode from a String stream to a bytes stream.
 *
 * @author Sebastien Deleuze
 * @see StringDecoder
 */
public class StringEncoder extends AbstractEncoder<String> {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


	public StringEncoder() {
		super(new MimeType("text", "plain", DEFAULT_CHARSET));
	}


	@Override
	public boolean canEncode(ResolvableType type, MimeType mimeType, Object... hints) {
		Class<?> clazz = type.getRawClass();
		return (super.canEncode(type, mimeType, hints) && String.class.isAssignableFrom(clazz));
	}

	@Override
	public Flux<ByteBuffer> encode(Publisher<? extends String> elementStream,
			ResolvableType type, MimeType mimeType, Object... hints) {

		Charset charset;
		if (mimeType != null && mimeType.getCharSet() != null) {
			charset = mimeType.getCharSet();
		}
		else {
			 charset = DEFAULT_CHARSET;
		}
		return Flux.from(elementStream).map(s -> ByteBuffer.wrap(s.getBytes(charset)));
	}

}
