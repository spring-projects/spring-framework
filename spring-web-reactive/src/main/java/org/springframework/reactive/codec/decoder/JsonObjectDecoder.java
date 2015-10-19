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

package org.springframework.reactive.codec.decoder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.reactive.codec.encoder.JsonObjectEncoder;

import reactor.Publishers;
import reactor.fn.Function;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Decode an arbitrary split byte stream representing JSON objects to a byte stream
 * where each chunk is a well-formed JSON object.
 *
 * This class does not do any real parsing or validation. A sequence of bytes is considered a JSON object/array
 * if it contains a matching number of opening and closing braces/brackets.
 *
 * Based on <a href=https://github.com/netty/netty/blob/master/codec/src/main/java/io/netty/handler/codec/json/JsonObjectDecoder.java">Netty {@code JsonObjectDecoder}</a>
 *
 * @author Sebastien Deleuze
 * @see JsonObjectEncoder
 */
public class JsonObjectDecoder implements ByteToMessageDecoder<ByteBuffer> {

	private static final int ST_CORRUPTED = -1;
	private static final int ST_INIT = 0;
	private static final int ST_DECODING_NORMAL = 1;
	private static final int ST_DECODING_ARRAY_STREAM = 2;

	private final int maxObjectLength;
	private final boolean streamArrayElements;

	public JsonObjectDecoder() {
		// 1 MB
		this(1024 * 1024);
	}

	public JsonObjectDecoder(int maxObjectLength) {
		this(maxObjectLength, true);
	}

	public JsonObjectDecoder(boolean streamArrayElements) {
		this(1024 * 1024, streamArrayElements);
	}

	/**
	 * @param maxObjectLength   maximum number of bytes a JSON object/array may use (including braces and all).
	 *                             Objects exceeding this length are dropped and an {@link IllegalStateException}
	 *                             is thrown.
	 * @param streamArrayElements   if set to true and the "top level" JSON object is an array, each of its entries
	 *                                  is passed through the pipeline individually and immediately after it was fully
	 *                                  received, allowing for arrays with "infinitely" many elements.
	 *
	 */
	public JsonObjectDecoder(int maxObjectLength, boolean streamArrayElements) {
		if (maxObjectLength < 1) {
			throw new IllegalArgumentException("maxObjectLength must be a positive int");
		}
		this.maxObjectLength = maxObjectLength;
		this.streamArrayElements = streamArrayElements;
	}


	@Override
	public boolean canDecode(ResolvableType type, MediaType mediaType, Object... hints) {
		return mediaType.isCompatibleWith(MediaType.APPLICATION_JSON);
	}

	@Override
	public Publisher<ByteBuffer> decode(Publisher<ByteBuffer> inputStream, ResolvableType type, MediaType mediaType, Object... hints) {

		return Publishers.flatMap(inputStream, new Function<ByteBuffer, Publisher<? extends ByteBuffer>>() {

			int openBraces;
			int idx;
			int state;
			boolean insideString;
			ByteBuf in;
			Integer wrtIdx;

			@Override
			public Publisher<? extends ByteBuffer> apply(ByteBuffer b) {
				List<ByteBuffer> chunks = new ArrayList<>();

				if (in == null) {
					in = Unpooled.copiedBuffer(b);
					wrtIdx = in.writerIndex();
				}
				else {
					in = Unpooled.copiedBuffer(in, Unpooled.copiedBuffer(b));
					wrtIdx = in.writerIndex();
				}
				if (state == ST_CORRUPTED) {
					in.skipBytes(in.readableBytes());
					return Publishers.error(new IllegalStateException("Corrupted stream"));
				}

				if (wrtIdx > maxObjectLength) {
					// buffer size exceeded maxObjectLength; discarding the complete buffer.
					in.skipBytes(in.readableBytes());
					reset();
					return Publishers.error(new IllegalStateException(
					  "object length exceeds " + maxObjectLength + ": " +
						wrtIdx +
						" bytes discarded"));
				}

				for (/* use current idx */; idx < wrtIdx; idx++) {
					byte c = in.getByte(idx);
					if (state == ST_DECODING_NORMAL) {
						decodeByte(c, in, idx);

						// All opening braces/brackets have been closed. That's enough to conclude
						// that the JSON object/array is complete.
						if (openBraces == 0) {
							ByteBuf json = extractObject(in, in.readerIndex(),
									idx + 1 - in.readerIndex());
							if (json != null) {
								chunks.add(json.nioBuffer());
							}

							// The JSON object/array was extracted => discard the bytes from
							// the input buffer.
							in.readerIndex(idx + 1);
							// Reset the object state to get ready for the next JSON object/text
							// coming along the byte stream.
							reset();
						}
					}
					else if (state == ST_DECODING_ARRAY_STREAM) {
						decodeByte(c, in, idx);

						if (!insideString && (openBraces == 1 && c == ',' ||
								openBraces == 0 && c == ']')) {
							// skip leading spaces. No range check is needed and the loop will terminate
							// because the byte at position idx is not a whitespace.
							for (int i = in.readerIndex(); Character.isWhitespace(in.getByte(i)); i++) {
								in.skipBytes(1);
							}

							// skip trailing spaces.
							int idxNoSpaces = idx - 1;
							while (idxNoSpaces >= in.readerIndex() &&
									Character.isWhitespace(in.getByte(idxNoSpaces))) {
								idxNoSpaces--;
							}

							ByteBuf json = extractObject(in, in.readerIndex(),
									idxNoSpaces + 1 - in.readerIndex());
							if (json != null) {
								chunks.add(json.nioBuffer());
							}

							in.readerIndex(idx + 1);

							if (c == ']') {
								reset();
							}
						}
						// JSON object/array detected. Accumulate bytes until all braces/brackets are closed.
					}
					else if (c == '{' || c == '[') {
						initDecoding(c, streamArrayElements);

						if (state == ST_DECODING_ARRAY_STREAM) {
							// Discard the array bracket
							in.skipBytes(1);
						}
						// Discard leading spaces in front of a JSON object/array.
					}
					else if (Character.isWhitespace(c)) {
						in.skipBytes(1);
					}
					else {
						state = ST_CORRUPTED;
						return Publishers.error(new IllegalStateException(
						  "invalid JSON received at byte position " + idx +
							": " + ByteBufUtil.hexDump(in)));
					}
				}

				if (in.readableBytes() == 0) {
					idx = 0;
				}
				return Publishers.from(chunks);
			}

			/**
			 * Override this method if you want to filter the json objects/arrays that get passed through the pipeline.
			 */
			@SuppressWarnings("UnusedParameters")
			protected ByteBuf extractObject(ByteBuf buffer, int index, int length) {
				return buffer.slice(index, length).retain();
			}

			private void decodeByte(byte c, ByteBuf in, int idx) {
				if ((c == '{' || c == '[') && !insideString) {
					openBraces++;
				}
				else if ((c == '}' || c == ']') && !insideString) {
					openBraces--;
				}
				else if (c == '"') {
					// start of a new JSON string. It's necessary to detect strings as they may
					// also contain braces/brackets and that could lead to incorrect results.
					if (!insideString) {
						insideString = true;
						// If the double quote wasn't escaped then this is the end of a string.
					}
					else if (in.getByte(idx - 1) != '\\') {
						insideString = false;
					}
				}
			}

			private void initDecoding(byte openingBrace, boolean streamArrayElements) {
				openBraces = 1;
				if (openingBrace == '[' && streamArrayElements) {
					state = ST_DECODING_ARRAY_STREAM;
				}
				else {
					state = ST_DECODING_NORMAL;
				}
			}

			private void reset() {
				insideString = false;
				state = ST_INIT;
				openBraces = 0;
			}

		});
	}

}
