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
import reactor.Flux;
import reactor.io.buffer.Buffer;

import org.springframework.core.ResolvableType;
import org.springframework.util.MimeType;

/**
 * Decode from a bytes stream to a String stream.
 *
 * @author Sebastien Deleuze
 * @see StringEncoder
 */
public class StringDecoder extends AbstractDecoder<String> {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	public StringDecoder() {
		super(new MimeType("text", "plain", DEFAULT_CHARSET));
	}

	@Override
	public boolean canDecode(ResolvableType type, MimeType mimeType, Object... hints) {
		return super.canDecode(type, mimeType, hints)
				&& String.class.isAssignableFrom(type.getRawClass());
	}

	@Override
	public Flux<String> decode(Publisher<ByteBuffer> inputStream, ResolvableType type,
			MimeType mimeType, Object... hints) {

		Charset charset;
		if (mimeType != null && mimeType.getCharSet() != null) {
			charset = mimeType.getCharSet();
		}
		else {
			 charset = DEFAULT_CHARSET;
		}
		return Flux.from(inputStream).map(content -> new String(new Buffer(content).asBytes(), charset));
	}

}
