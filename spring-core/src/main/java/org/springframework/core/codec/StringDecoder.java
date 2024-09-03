/*
 * Copyright 2002-2023 the original author or authors.
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.LimitedDataBufferList;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

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
 * @since 5.0
 * @see CharSequenceEncoder
 */
public final class StringDecoder extends AbstractDataBufferDecoder<String> {

	/** The default charset to use, i.e. "UTF-8". */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/** The default delimiter strings to use, i.e. {@code \r\n} and {@code \n}. */
	public static final List<String> DEFAULT_DELIMITERS = List.of("\r\n", "\n");


	private final List<String> delimiters;

	private final boolean stripDelimiter;

	private Charset defaultCharset = DEFAULT_CHARSET;

	private final ConcurrentMap<Charset, byte[][]> delimitersCache = new ConcurrentHashMap<>();


	private StringDecoder(List<String> delimiters, boolean stripDelimiter, MimeType... mimeTypes) {
		super(mimeTypes);
		Assert.notEmpty(delimiters, "'delimiters' must not be empty");
		this.delimiters = new ArrayList<>(delimiters);
		this.stripDelimiter = stripDelimiter;
	}


	/**
	 * Set the default character set to fall back on if the MimeType does not specify any.
	 * <p>By default this is {@code UTF-8}.
	 * @param defaultCharset the charset to fall back on
	 * @since 5.2.9
	 */
	public void setDefaultCharset(Charset defaultCharset) {
		this.defaultCharset = defaultCharset;
	}

	/**
	 * Return the configured {@link #setDefaultCharset(Charset) defaultCharset}.
	 * @since 5.2.9
	 */
	public Charset getDefaultCharset() {
		return this.defaultCharset;
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		return (elementType.resolve() == String.class && super.canDecode(elementType, mimeType));
	}

	@Override
	public Flux<String> decode(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		byte[][] delimiterBytes = getDelimiterBytes(mimeType);

		LimitedDataBufferList chunks = new LimitedDataBufferList(getMaxInMemorySize());
		DataBufferUtils.Matcher matcher = DataBufferUtils.matcher(delimiterBytes);

		return Flux.from(input)
				.concatMapIterable(buffer -> processDataBuffer(buffer, matcher, chunks))
				.concatWith(Mono.defer(() -> {
					if (chunks.isEmpty()) {
						return Mono.empty();
					}
					DataBuffer lastBuffer = chunks.get(0).factory().join(chunks);
					chunks.clear();
					return Mono.just(lastBuffer);
				}))
				.doFinally(signalType -> chunks.releaseAndClear())
				.doOnDiscard(DataBuffer.class, DataBufferUtils::release)
				.map(buffer -> decode(buffer, elementType, mimeType, hints));
	}

	private byte[][] getDelimiterBytes(@Nullable MimeType mimeType) {
		return this.delimitersCache.computeIfAbsent(getCharset(mimeType), charset -> {
			byte[][] result = new byte[this.delimiters.size()][];
			for (int i = 0; i < this.delimiters.size(); i++) {
				result[i] = this.delimiters.get(i).getBytes(charset);
			}
			return result;
		});
	}

	private Collection<DataBuffer> processDataBuffer(
			DataBuffer buffer, DataBufferUtils.Matcher matcher, LimitedDataBufferList chunks) {

		boolean release = true;
		try {
			List<DataBuffer> result = null;
			do {
				int endIndex = matcher.match(buffer);
				if (endIndex == -1) {
					chunks.add(buffer);
					release = false;
					break;
				}
				DataBuffer split = buffer.split(endIndex + 1);
				if (result == null) {
					result = new ArrayList<>();
				}
				int delimiterLength = matcher.delimiter().length;
				if (chunks.isEmpty()) {
					if (this.stripDelimiter) {
						split.writePosition(split.writePosition() - delimiterLength);
					}
					result.add(split);
				}
				else {
					chunks.add(split);
					DataBuffer joined = buffer.factory().join(chunks);
					if (this.stripDelimiter) {
						joined.writePosition(joined.writePosition() - delimiterLength);
					}
					result.add(joined);
					chunks.clear();
				}
			}
			while (buffer.readableByteCount() > 0);
			return (result != null ? result : Collections.emptyList());
		}
		finally {
			if (release) {
				DataBufferUtils.release(buffer);
			}
		}
	}

	@Override
	public String decode(DataBuffer dataBuffer, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		Charset charset = getCharset(mimeType);
		String value = dataBuffer.toString(charset);
		DataBufferUtils.release(dataBuffer);
		LogFormatUtils.traceDebug(logger, traceOn -> {
			String formatted = LogFormatUtils.formatValue(value, !traceOn);
			return Hints.getLogPrefix(hints) + "Decoded " + formatted;
		});
		return value;
	}

	private Charset getCharset(@Nullable MimeType mimeType) {
		if (mimeType != null && mimeType.getCharset() != null) {
			return mimeType.getCharset();
		}
		else {
			return getDefaultCharset();
		}
	}

	/**
	 * Create a {@code StringDecoder} for {@code "text/plain"}.
	 */
	public static StringDecoder textPlainOnly() {
		return textPlainOnly(DEFAULT_DELIMITERS, true);
	}

	/**
	 * Create a {@code StringDecoder} for {@code "text/plain"}.
	 * @param delimiters delimiter strings to use to split the input stream
	 * @param stripDelimiter whether to remove delimiters from the resulting
	 * input strings
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
	 * @param delimiters delimiter strings to use to split the input stream
	 * @param stripDelimiter whether to remove delimiters from the resulting
	 * input strings
	 */
	public static StringDecoder allMimeTypes(List<String> delimiters, boolean stripDelimiter) {
		return new StringDecoder(delimiters, stripDelimiter,
				new MimeType("text", "plain", DEFAULT_CHARSET), MimeTypeUtils.ALL);
	}

}
