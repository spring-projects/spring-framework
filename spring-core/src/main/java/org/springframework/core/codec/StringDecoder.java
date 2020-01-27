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

	private final ConcurrentMap<Charset, byte[][]> delimitersCache = new ConcurrentHashMap<>();


	private StringDecoder(List<String> delimiters, boolean stripDelimiter, MimeType... mimeTypes) {
		super(mimeTypes);
		Assert.notEmpty(delimiters, "'delimiters' must not be empty");
		this.delimiters = new ArrayList<>(delimiters);
		this.stripDelimiter = stripDelimiter;
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
			if (getMaxInMemorySize() != -1) {

				// Passing limiter into endFrameAfterDelimiter helps to ensure that in case of one DataBuffer
				// containing multiple lines, the limit is checked and raised  immediately without accumulating
				// subsequent lines. This is necessary because concatMapIterable doesn't respect doOnDiscard.
				// When reactor-core#1925 is resolved, we could replace bufferUntil with:

				//	.windowUntil(buffer -> buffer instanceof EndFrameBuffer)
				//	.concatMap(fluxes -> fluxes.collect(() -> new LimitedDataBufferList(getMaxInMemorySize()), LimitedDataBufferList::add))

				LimitedDataBufferList limiter = new LimitedDataBufferList(getMaxInMemorySize());

				return Flux.from(input)
						.concatMapIterable(buffer -> endFrameAfterDelimiter(buffer, matcher, limiter))
						.bufferUntil(buffer -> buffer instanceof EndFrameBuffer)
						.map(buffers -> joinAndStrip(buffers, this.stripDelimiter))
						.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
			}
			else {

				// When the decoder is unlimited (-1), concatMapIterable will cache buffers that may not
				// be released if cancel is signalled before they are turned into String lines
				// (see test maxInMemoryLimitReleasesUnprocessedLinesWhenUnlimited).
				// When reactor-core#1925 is resolved, the workaround can be removed and the entire
				// else clause possibly dropped.

				ConcatMapIterableDiscardWorkaroundCache cache = new ConcatMapIterableDiscardWorkaroundCache();

				return Flux.from(input)
						.concatMapIterable(buffer -> cache.addAll(endFrameAfterDelimiter(buffer, matcher, null)))
						.doOnNext(cache)
						.doOnCancel(cache)
						.bufferUntil(buffer -> buffer instanceof EndFrameBuffer)
						.map(buffers -> joinAndStrip(buffers, this.stripDelimiter))
						.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
			}
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

	private static Charset getCharset(@Nullable MimeType mimeType) {
		if (mimeType != null && mimeType.getCharset() != null) {
			return mimeType.getCharset();
		}
		else {
			return DEFAULT_CHARSET;
		}
	}

	/**
	 * Finds the first match and longest delimiter, {@link EndFrameBuffer} just after it.
	 *
	 * @param dataBuffer the buffer to find delimiters in
	 * @param matcher used to find the first delimiters
	 * @param limiter to enforce maxInMemorySize with
	 * @return a flux of buffers, containing {@link EndFrameBuffer} after each delimiter that was
	 * found in {@code dataBuffer}. Returns  Flux, because returning List (w/ flatMapIterable)
	 * results in memory leaks due to pre-fetching.
	 */
	private static List<DataBuffer> endFrameAfterDelimiter(
			DataBuffer dataBuffer, DataBufferUtils.Matcher matcher, @Nullable LimitedDataBufferList limiter) {

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
					if (limiter != null) {
						limiter.add(slice); // enforce the limit
						limiter.clear();
					}
				}
				else {
					result.add(DataBufferUtils.retain(dataBuffer));
					if (limiter != null) {
						limiter.add(dataBuffer);
					}
					break;
				}
			}
			while (dataBuffer.readableByteCount() > 0);
		}
		catch (DataBufferLimitException ex) {
			if (limiter != null) {
				limiter.releaseAndClear();
			}
			throw ex;
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
	private static DataBuffer joinAndStrip(List<DataBuffer> dataBuffers,
			boolean stripDelimiter) {

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
	 * @param ignored ignored
	 * @deprecated as of Spring 5.0.4, in favor of {@link #textPlainOnly()} or
	 * {@link #textPlainOnly(List, boolean)}
	 */
	@Deprecated
	public static StringDecoder textPlainOnly(boolean ignored) {
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
	 * @param ignored ignored
	 * @deprecated as of Spring 5.0.4, in favor of {@link #allMimeTypes()} or
	 * {@link #allMimeTypes(List, boolean)}
	 */
	@Deprecated
	public static StringDecoder allMimeTypes(boolean ignored) {
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

		private static final DataBuffer BUFFER = new DefaultDataBufferFactory().wrap(new byte[0]);

		private byte[] delimiter;


		public EndFrameBuffer(byte[] delimiter) {
			super(BUFFER);
			this.delimiter = delimiter;
		}

		public byte[] delimiter() {
			return this.delimiter;
		}

	}


	private class ConcatMapIterableDiscardWorkaroundCache implements Consumer<DataBuffer>, Runnable {

		private final List<DataBuffer> buffers = new ArrayList<>();


		public List<DataBuffer> addAll(List<DataBuffer> buffersToAdd) {
			this.buffers.addAll(buffersToAdd);
			return buffersToAdd;
		}

		@Override
		public void accept(DataBuffer dataBuffer) {
			this.buffers.remove(dataBuffer);
		}

		@Override
		public void run() {
			this.buffers.forEach(buffer -> {
				try {
					DataBufferUtils.release(buffer);
				}
				catch (Throwable ex) {
					// Keep going..
				}
			});
		}
	}

}
