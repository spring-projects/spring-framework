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

package org.springframework.reactive.codec.encoder;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.reactivestreams.Publisher;
import reactor.Publishers;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.reactive.codec.decoder.StringDecoder;
import org.springframework.reactive.codec.support.HintUtils;

/**
 * Encode from a String stream to a bytes stream.
 *
 * @author Sebastien Deleuze
 * @see StringDecoder
 */
public class StringEncoder implements MessageToByteEncoder<String> {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


	@Override
	public boolean canEncode(ResolvableType type, MediaType mediaType, Object... hints) {
		return mediaType.isCompatibleWith(MediaType.TEXT_PLAIN)
				&& String.class.isAssignableFrom(type.getRawClass());
	}

	@Override
	public Publisher<ByteBuffer> encode(Publisher<? extends String> elementStream,
			ResolvableType type, MediaType mediaType, Object... hints) {

		final Charset charset = HintUtils.getHintByClass(Charset.class, hints, DEFAULT_CHARSET);
		return Publishers.map(elementStream, s -> ByteBuffer.wrap(s.getBytes(charset)));
	}

}
