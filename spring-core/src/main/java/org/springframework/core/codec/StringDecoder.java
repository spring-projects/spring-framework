/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.codec;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * Decode from a data buffer stream to a {@code String} stream, either splitting
 * or aggregating incoming data chunks to realign along newlines delimiters
 * and produce a stream of strings. This is useful for streaming but is also
 * necessary to ensure that multi-byte characters can be decoded correctly,
 * avoiding split-character issues. The default delimiters used by default are
 * {@code \n} and {@code \r\n} but that can be customized.
 *
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @author Mark Paluch
 * @see CharSequenceEncoder
 * @since 5.0
 */
public final class StringDecoder extends AbstractDataBufferDecoder<String> {

	/**
	 * The default charset to use, i.e. "UTF-8".
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * The default delimiter strings to use, i.e. {@code \r\n} and {@code \n}.
	 */
	public static final List<String> DEFAULT_DELIMITERS = List.of("\r\n", "\n");

	private static final ResolvableType CHAR_BUFFER_TYPE = ResolvableType.forClass(CharBuffer.class);


	private final CharBufferDecoder charBufferDecoder;

	private StringDecoder(List<String> delimiters, boolean stripDelimiter, MimeType... mimeTypes) {
		super(mimeTypes);
		this.charBufferDecoder = CharBufferDecoder.allMimeTypes(delimiters, stripDelimiter);
	}

	@Override
	public void setMaxInMemorySize(int byteCount) {
		super.setMaxInMemorySize(byteCount);
		this.charBufferDecoder.setMaxInMemorySize(byteCount);
	}

	/**
	 * Set the default character set to fall back on if the MimeType does not specify any.
	 * <p>By default this is {@code UTF-8}.
	 *
	 * @param defaultCharset
	 * 		the charset to fall back on
	 * @since 5.2.9
	 */
	public void setDefaultCharset(Charset defaultCharset) {
		this.charBufferDecoder.setDefaultCharset(defaultCharset);
	}

	/**
	 * Return the configured {@link #setDefaultCharset(Charset) defaultCharset}.
	 *
	 * @since 5.2.9
	 */
	public Charset getDefaultCharset() {
		return this.charBufferDecoder.getDefaultCharset();
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		return elementType.resolve() == String.class && super.canDecode(elementType, mimeType);
	}

	@Override
	public Flux<String> decode(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return this.charBufferDecoder.decode(input, CHAR_BUFFER_TYPE, mimeType, hints)
				.map(CharBuffer::toString);
	}

	@Override
	public String decode(DataBuffer dataBuffer, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return this.charBufferDecoder.decode(dataBuffer, CHAR_BUFFER_TYPE, mimeType, hints)
				.toString();
	}

	/**
	 * Create a {@code StringDecoder} for {@code "text/plain"}.
	 */
	public static StringDecoder textPlainOnly() {
		return textPlainOnly(DEFAULT_DELIMITERS, true);
	}

	/**
	 * Create a {@code StringDecoder} for {@code "text/plain"}.
	 *
	 * @param delimiters
	 * 		delimiter strings to use to split the input stream
	 * @param stripDelimiter
	 * 		whether to remove delimiters from the resulting
	 * 		input strings
	 */
	public static StringDecoder textPlainOnly(List<String> delimiters, boolean stripDelimiter) {
		return new StringDecoder(delimiters, stripDelimiter, new MimeType("text", "plain", DEFAULT_CHARSET));
	}

	/**
	 * Create a {@code StringDecoder} that supports all MIME types.
	 */
	public static StringDecoder allMimeTypes() {
		return allMimeTypes(DEFAULT_DELIMITERS, true);
	}

	/**
	 * Create a {@code StringDecoder} that supports all MIME types.
	 *
	 * @param delimiters
	 * 		delimiter strings to use to split the input stream
	 * @param stripDelimiter
	 * 		whether to remove delimiters from the resulting
	 * 		input strings
	 */
	public static StringDecoder allMimeTypes(List<String> delimiters, boolean stripDelimiter) {
		return new StringDecoder(delimiters, stripDelimiter,
				new MimeType("text", "plain", DEFAULT_CHARSET), MimeTypeUtils.ALL);
	}

}
