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

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.support.DataBufferUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Decode from a bytes stream to a String stream.
 *
 * <p>By default, this decoder will split the received {@link DataBuffer}s along newline
 * characters ({@code \r\n}), but this can be changed by passing {@code false} as
 * constructor argument.
 *
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @author Mark Paluch
 * @see StringEncoder
 */
public class StringDecoder extends AbstractDecoder<String> {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private final boolean splitOnNewline;

	/**
	 * Create a {@code StringDecoder} that decodes a bytes stream to a String stream
	 *
	 * <p>By default, this decoder will split along new lines.
	 */
	public StringDecoder() {
		this(true);
	}

	/**
	 * Create a {@code StringDecoder} that decodes a bytes stream to a String stream
	 *
	 * @param splitOnNewline whether this decoder should split the received data buffers
	 * along newline characters
	 */
	public StringDecoder(boolean splitOnNewline) {
		super(new MimeType("text", "*", DEFAULT_CHARSET), MimeTypeUtils.ALL);
		this.splitOnNewline = splitOnNewline;
	}

	@Override
	public boolean canDecode(ResolvableType type, MimeType mimeType, Object... hints) {
		return super.canDecode(type, mimeType, hints) &&
				String.class.equals(type.getRawClass());
	}

	@Override
	public Flux<String> decode(Publisher<DataBuffer> inputStream, ResolvableType type,
			MimeType mimeType, Object... hints) {
		Flux<DataBuffer> inputFlux = Flux.from(inputStream);
		if (this.splitOnNewline) {
			inputFlux = inputFlux.flatMap(dataBuffer -> {
				List<DataBuffer> tokens =
						DataBufferUtils.tokenize(dataBuffer, b -> b == '\n' || b == '\r');
				return Flux.fromIterable(tokens);
			});
		}
		Charset charset = getCharset(mimeType);
		return inputFlux.map(dataBuffer -> {
			CharBuffer charBuffer = charset.decode(dataBuffer.asByteBuffer());
			DataBufferUtils.release(dataBuffer);
			return charBuffer.toString();
		});
	}

	private Charset getCharset(MimeType mimeType) {
		if (mimeType != null && mimeType.getCharset() != null) {
			return mimeType.getCharset();
		}
		else {
			return DEFAULT_CHARSET;
		}
	}

}
