/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DataBufferWrapper;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.LimitedDataBufferList;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Decode from a data buffer stream to a {@code String} stream. Before decoding, this decoder
 * realigns the incoming data buffers so that each buffer ends with a newline.
 * This is to make sure that multibyte characters are decoded properly, and do not cross buffer
 * boundaries. The default delimiters ({@code \n}, {@code \r\n})can be customized.
 *
 * <p>Partially inspired by Netty's {@code DelimiterBasedFrameDecoder}.
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
	public static final List<String> DEFAULT_DELIMITERS = Arrays.asList("\r\n", "\n");


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

		Flux<DataBuffer> inputFlux = Flux.defer(() -> {
			DataBufferUtils.Matcher matcher = DataBufferUtils.matcher(delimiterBytes);

			@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
			LimitChecker limiter = new LimitChecker(getMaxInMemorySize());

			return Flux.from(input)
					.concatMapIterable(buffer -> endFrameAfterDelimiter(buffer, matcher))
					.doOnNext(limiter)
					.bufferUntil(buffer -> buffer instanceof EndFrameBuffer)
					.map(list -> joinAndStrip(list, this.stripDelimiter))
					.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
		});

		return super.decode(inputFlux, elementType, mimeType, hints);
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

	@Override
	public String decode(DataBuffer dataBuffer, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		Charset charset = getCharset(mimeType);
		CharBuffer charBuffer = charset.decode(dataBuffer.asByteBuffer());
		DataBufferUtils.release(dataBuffer);
		String value = charBuffer.toString();
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
	 * Finds the first match and longest delimiter, {@link EndFrameBuffer} just after it.
	 * @param dataBuffer the buffer to find delimiters in
	 * @param matcher used to find the first delimiters
	 * @return a flux of buffers, containing {@link EndFrameBuffer} after each delimiter that was
	 * found in {@code dataBuffer}. Returns  Flux, because returning List (w/ flatMapIterable)
	 * results in memory leaks due to pre-fetching.
	 */
	private static List<DataBuffer> endFrameAfterDelimiter(DataBuffer dataBuffer, DataBufferUtils.Matcher matcher) {
		List<DataBuffer> result = new ArrayList<>();
		try {
			do {
				int endIdx = matcher.match(dataBuffer);
				if (endIdx != -1) {
					int readPosition = dataBuffer.readPosition();
					int length = (endIdx - readPosition + 1);
					DataBuffer slice = dataBuffer.retainedSlice(readPosition, length);
					result.add(slice);
					result.add(new EndFrameBuffer(matcher.delimiter()));
					dataBuffer.readPosition(endIdx + 1);
				}
				else {
					result.add(DataBufferUtils.retain(dataBuffer));
					break;
				}
			}
			while (dataBuffer.readableByteCount() > 0);
		}
		finally {
			DataBufferUtils.release(dataBuffer);
		}
		return result;
	}

	/**
	 * Joins the given list of buffers. If the list ends with a {@link EndFrameBuffer}, it is
	 * removed. If {@code stripDelimiter} is {@code true} and the resulting buffer ends with
	 * a delimiter, it is removed.
	 * @param dataBuffers the data buffers to join
	 * @param stripDelimiter whether to strip the delimiter
	 * @return the joined buffer
	 */
	private static DataBuffer joinAndStrip(List<DataBuffer> dataBuffers, boolean stripDelimiter) {
		Assert.state(!dataBuffers.isEmpty(), "DataBuffers should not be empty");

		byte[] matchingDelimiter = null;

		int lastIdx = dataBuffers.size() - 1;
		DataBuffer lastBuffer = dataBuffers.get(lastIdx);
		if (lastBuffer instanceof EndFrameBuffer) {
			matchingDelimiter = ((EndFrameBuffer) lastBuffer).delimiter();
			dataBuffers.remove(lastIdx);
		}

		DataBuffer result = dataBuffers.get(0).factory().join(dataBuffers);
		if (stripDelimiter && matchingDelimiter != null) {
			result.writePosition(result.writePosition() - matchingDelimiter.length);
		}
		return result;
	}


	/**
	 * Create a {@code StringDecoder} for {@code "text/plain"}.
	 * @param stripDelimiter this flag is ignored
	 * @deprecated as of Spring 5.0.4, in favor of {@link #textPlainOnly()} or
	 * {@link #textPlainOnly(List, boolean)}
	 */
	@Deprecated
	public static StringDecoder textPlainOnly(boolean stripDelimiter) {
		return textPlainOnly();
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
	 * @param stripDelimiter this flag is ignored
	 * @deprecated as of Spring 5.0.4, in favor of {@link #allMimeTypes()} or
	 * {@link #allMimeTypes(List, boolean)}
	 */
	@Deprecated
	public static StringDecoder allMimeTypes(boolean stripDelimiter) {
		return allMimeTypes();
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


	private static class EndFrameBuffer extends DataBufferWrapper {

		private static final DataBuffer BUFFER = DefaultDataBufferFactory.sharedInstance.wrap(new byte[0]);

		private final byte[] delimiter;

		public EndFrameBuffer(byte[] delimiter) {
			super(BUFFER);
			this.delimiter = delimiter;
		}

		public byte[] delimiter() {
			return this.delimiter;
		}
	}


	private static class LimitChecker implements Consumer<DataBuffer> {

		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		private final LimitedDataBufferList list;

		LimitChecker(int maxInMemorySize) {
			this.list = new LimitedDataBufferList(maxInMemorySize);
		}

		@Override
		public void accept(DataBuffer buffer) {
			if (buffer instanceof EndFrameBuffer) {
				this.list.clear();
			}
			try {
				this.list.add(buffer);
			}
			catch (DataBufferLimitException ex) {
				DataBufferUtils.release(buffer);
				throw ex;
			}
		}
	}

}
