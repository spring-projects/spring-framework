/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.IntPredicate;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
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
			return Flux.from(input)
					.concatMapIterable(buffer -> endFrameAfterDelimiter(buffer, matcher))
					.bufferUntil(buffer -> buffer instanceof EndFrameBuffer)
					.map(buffers -> joinAndStrip(buffers, this.stripDelimiter))
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
	 * @return a flux of buffers, containing {@link EndFrameBuffer} after each delimiter that was
	 * found in {@code dataBuffer}. Returns  Flux, because returning List (w/ flatMapIterable)
	 * results in memory leaks due to pre-fetching.
	 */
	private static List<DataBuffer> endFrameAfterDelimiter(DataBuffer dataBuffer, DataBufferUtils.Matcher matcher) {
		List<DataBuffer> result = new ArrayList<>();
		do {
			int endIdx = matcher.match(dataBuffer);
			if (endIdx != -1) {
				int readPosition = dataBuffer.readPosition();
				int length = endIdx - readPosition + 1;
				result.add(dataBuffer.retainedSlice(readPosition, length));
				result.add(new EndFrameBuffer(matcher.delimiter()));
				dataBuffer.readPosition(endIdx + 1);
			}
			else {
				result.add(DataBufferUtils.retain(dataBuffer));
				break;
			}
		}
		while (dataBuffer.readableByteCount() > 0);

		DataBufferUtils.release(dataBuffer);
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


	private static class EndFrameBuffer implements DataBuffer {

		private static final DataBuffer BUFFER = new DefaultDataBufferFactory().wrap(new byte[0]);

		private byte[] delimiter;


		public EndFrameBuffer(byte[] delimiter) {
			this.delimiter = delimiter;
		}

		public byte[] delimiter() {
			return this.delimiter;
		}

		@Override
		public DataBufferFactory factory() {
			return BUFFER.factory();
		}

		@Override
		public int indexOf(IntPredicate predicate, int fromIndex) {
			return BUFFER.indexOf(predicate, fromIndex);
		}

		@Override
		public int lastIndexOf(IntPredicate predicate, int fromIndex) {
			return BUFFER.lastIndexOf(predicate, fromIndex);
		}

		@Override
		public int readableByteCount() {
			return BUFFER.readableByteCount();
		}

		@Override
		public int writableByteCount() {
			return BUFFER.writableByteCount();
		}

		@Override
		public int capacity() {
			return BUFFER.capacity();
		}

		@Override
		public DataBuffer capacity(int capacity) {
			return BUFFER.capacity(capacity);
		}

		@Override
		public DataBuffer ensureCapacity(int capacity) {
			return BUFFER.ensureCapacity(capacity);
		}

		@Override
		public int readPosition() {
			return BUFFER.readPosition();
		}

		@Override
		public DataBuffer readPosition(int readPosition) {
			return BUFFER.readPosition(readPosition);
		}

		@Override
		public int writePosition() {
			return BUFFER.writePosition();
		}

		@Override
		public DataBuffer writePosition(int writePosition) {
			return BUFFER.writePosition(writePosition);
		}

		@Override
		public byte getByte(int index) {
			return BUFFER.getByte(index);
		}

		@Override
		public byte read() {
			return BUFFER.read();
		}

		@Override
		public DataBuffer read(byte[] destination) {
			return BUFFER.read(destination);
		}

		@Override
		public DataBuffer read(byte[] destination, int offset, int length) {
			return BUFFER.read(destination, offset, length);
		}

		@Override
		public DataBuffer write(byte b) {
			return BUFFER.write(b);
		}

		@Override
		public DataBuffer write(byte[] source) {
			return BUFFER.write(source);
		}

		@Override
		public DataBuffer write(byte[] source, int offset, int length) {
			return BUFFER.write(source, offset, length);
		}

		@Override
		public DataBuffer write(DataBuffer... buffers) {
			return BUFFER.write(buffers);
		}

		@Override
		public DataBuffer write(ByteBuffer... buffers) {
			return BUFFER.write(buffers);
		}

		@Override
		public DataBuffer write(CharSequence charSequence, Charset charset) {
			return BUFFER.write(charSequence, charset);
		}

		@Override
		public DataBuffer slice(int index, int length) {
			return BUFFER.slice(index, length);
		}

		@Override
		public DataBuffer retainedSlice(int index, int length) {
			return BUFFER.retainedSlice(index, length);
		}

		@Override
		public ByteBuffer asByteBuffer() {
			return BUFFER.asByteBuffer();
		}

		@Override
		public ByteBuffer asByteBuffer(int index, int length) {
			return BUFFER.asByteBuffer(index, length);
		}

		@Override
		public InputStream asInputStream() {
			return BUFFER.asInputStream();
		}

		@Override
		public InputStream asInputStream(boolean releaseOnClose) {
			return BUFFER.asInputStream(releaseOnClose);
		}

		@Override
		public OutputStream asOutputStream() {
			return BUFFER.asOutputStream();
		}
	}


}
