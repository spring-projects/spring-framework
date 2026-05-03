/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.http.converter.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageConversionException;

/**
 * Read a Multipart message as a byte stream and parse its content
 * and signals them to the {@link PartListener}.
 *
 * @author Brian Clozel
 * @author Arjen Poutsma
 */
final class MultipartParser {

	private static final Log logger = LogFactory.getLog(MultipartParser.class);

	private final int maxHeadersSize;

	private final int bufferSize;

	/**
	 * Create a new multipart parser instance.
	 *
	 * @param maxHeadersSize the maximum buffered header size
	 * @param bufferSize     the size of the reading buffer
	 */
	MultipartParser(int maxHeadersSize, int bufferSize) {
		this.maxHeadersSize = maxHeadersSize;
		this.bufferSize = bufferSize;
	}

	/**
	 * Parses the given stream of bytes into events published to the {@link PartListener}.
	 * @param input          the input stream
	 * @param boundary       the multipart boundary, as found in the {@code Content-Type} header
	 * @param headersCharset the charset to use for decoding headers
	 * @param listener       a listener for parsed tokens
	 */
	public void parse(InputStream input, byte[] boundary, Charset headersCharset, PartListener listener) {

		InternalParser internalParser = new InternalParser(boundary, headersCharset, listener);
		try {
			while (true) {
				byte[] read = input.readNBytes(this.bufferSize);
				if (read.length == 0) {
					break;
				}
				internalParser.state.data(DefaultDataBufferFactory.sharedInstance.wrap(read));
			}
			internalParser.state.complete();
		}
		catch (IOException ex) {
			internalParser.state.dispose();
			listener.onError(new HttpMessageConversionException("Could not decode multipart message", ex));
		}
	}

	private final class InternalParser {

		private final byte[] boundary;

		private final Charset headersCharset;

		private final PartListener listener;

		private State state;

		InternalParser(byte[] boundary, Charset headersCharset, PartListener listener) {
			this.boundary = boundary;
			this.headersCharset = headersCharset;
			this.listener = listener;
			this.state = new PreambleState();
		}

		void changeState(State newState, @Nullable DataBuffer remainder) {
			if (logger.isTraceEnabled()) {
				logger.trace("Changed state: " + this.state + " -> " + newState);
			}
			this.state.dispose();
			this.state = newState;
			if (remainder != null) {
				if (remainder.readableByteCount() > 0) {
					newState.data(remainder);
				}
				else {
					DataBufferUtils.release(remainder);
				}
			}
		}

		/**
		 * Concatenates the given array of byte arrays.
		 */
		private static byte[] concat(byte[]... byteArrays) {
			int len = 0;
			for (byte[] byteArray : byteArrays) {
				len += byteArray.length;
			}
			byte[] result = new byte[len];
			len = 0;
			for (byte[] byteArray : byteArrays) {
				System.arraycopy(byteArray, 0, result, len, byteArray.length);
				len += byteArray.length;
			}
			return result;
		}

		/**
		 * Represents the internal state of the {@link MultipartParser}.
		 * The flow for well-formed multipart messages is shown below:
		 * <p><pre>
		 *     PREAMBLE
		 *         |
		 *         v
		 *  +-->HEADERS--->DISPOSED
		 *  |      |
		 *  |      v
		 *  +----BODY
		 *  </pre>
		 * For malformed messages the flow ends in DISPOSED.
		 */
		private interface State {

			byte[] CR_LF = {'\r', '\n'};

			byte HYPHEN = '-';

			byte[] TWO_HYPHENS = {HYPHEN, HYPHEN};

			String HEADER_ENTRY_SEPARATOR = "\\r\\n";

			void data(DataBuffer buf);

			void complete();

			default void dispose() {
			}
		}

		/**
		 * The initial state of the parser. Looks for the first boundary of the
		 * multipart message. Note that the first boundary is not necessarily
		 * prefixed with {@code CR LF}; only the prefix {@code --} is required.
		 */
		private final class PreambleState implements State {

			private final DataBufferUtils.Matcher firstBoundary;


			PreambleState() {
				this.firstBoundary = DataBufferUtils.matcher(concat(TWO_HYPHENS, InternalParser.this.boundary));
			}

			/**
			 * Looks for the first boundary in the given buffer. If found, changes
			 * state to {@link HeadersState}, and passes on the remainder of the
			 * buffer.
			 */
			@Override
			public void data(DataBuffer buf) {
				int endIdx = this.firstBoundary.match(buf);
				if (endIdx != -1) {
					if (logger.isTraceEnabled()) {
						logger.trace("First boundary found @" + endIdx + " in " + buf);
					}
					DataBuffer preambleBuffer = buf.split(endIdx + 1);
					DataBufferUtils.release(preambleBuffer);
					changeState(new HeadersState(), buf);
				}
				else {
					DataBufferUtils.release(buf);
				}
			}

			@Override
			public void complete() {
				changeState(DisposedState.INSTANCE, null);
				InternalParser.this.listener.onError(new HttpMessageConversionException("Could not find first boundary"));
			}

			@Override
			public String toString() {
				return "PREAMBLE";
			}

		}

		/**
		 * The state of the parser dealing with part headers. Parses header
		 * buffers into a {@link HttpHeaders} instance, making sure that
		 * the amount does not exceed {@link #maxHeadersSize}.
		 */
		private final class HeadersState implements State {

			private final DataBufferUtils.Matcher endHeaders = DataBufferUtils.matcher(concat(CR_LF, CR_LF));

			private final List<DataBuffer> buffers = new ArrayList<>();

			private int byteCount;


			/**
			 * First checks whether the multipart boundary leading to this state
			 * was the final boundary. Then looks for the header-body boundary
			 * ({@code CR LF CR LF}) in the given buffer. If found, checks whether
			 * the size of all header buffers does not exceed {@link #maxHeadersSize},
			 * converts all buffers collected so far into a {@link HttpHeaders} object
			 * and changes to {@link BodyState}, passing the remainder of the
			 * buffer. If the boundary is not found, the buffer is collected if
			 * its size does not exceed {@link #maxHeadersSize}.
			 */
			@Override
			public void data(DataBuffer buf) {
				if (isLastBoundary(buf)) {
					if (logger.isTraceEnabled()) {
						logger.trace("Last boundary found in " + buf);
					}
					changeState(DisposedState.INSTANCE, buf);
					InternalParser.this.listener.onComplete();
					return;
				}
				int endIdx = this.endHeaders.match(buf);
				if (endIdx != -1) {
					if (logger.isTraceEnabled()) {
						logger.trace("End of headers found @" + endIdx + " in " + buf);
					}
					this.byteCount += endIdx;
					if (belowMaxHeaderSize(this.byteCount)) {
						DataBuffer headerBuf = buf.split(endIdx + 1);
						this.buffers.add(headerBuf);
						emitHeaders();
						changeState(new BodyState(), buf);
					}
				}
				else {
					this.byteCount += buf.readableByteCount();
					if (belowMaxHeaderSize(this.byteCount)) {
						this.buffers.add(buf);
					}
				}
			}

			private void emitHeaders() {
				HttpHeaders headers = parseHeaders();
				if (logger.isTraceEnabled()) {
					logger.trace("Emitting headers: " + headers);
				}
				InternalParser.this.listener.onHeaders(headers);
			}

			/**
			 * If the given buffer is the first buffer, check whether it starts with {@code --}.
			 * If it is the second buffer, check whether it makes up {@code --} together with the first buffer.
			 */
			private boolean isLastBoundary(DataBuffer buf) {
				return (this.buffers.isEmpty() &&
						buf.readableByteCount() >= 2 &&
						buf.getByte(0) == HYPHEN && buf.getByte(1) == HYPHEN) ||
						(this.buffers.size() == 1 &&
								this.buffers.get(0).readableByteCount() == 1 &&
								this.buffers.get(0).getByte(0) == HYPHEN &&
								buf.readableByteCount() >= 1 &&
								buf.getByte(0) == HYPHEN);
			}

			/**
			 * Checks whether the given {@code count} is below or equal to {@link #maxHeadersSize}
			 * and throws a {@link DataBufferLimitException} if not.
			 */
			private boolean belowMaxHeaderSize(long count) {
				if (count <= MultipartParser.this.maxHeadersSize) {
					return true;
				}
				else {
					InternalParser.this.listener.onError(
							new HttpMessageConversionException("Part headers exceeded the memory usage limit of " +
									MultipartParser.this.maxHeadersSize + " bytes"));
					return false;
				}
			}

			/**
			 * Parses the list of buffers into a {@link HttpHeaders} instance.
			 * Converts the joined buffers into a string using ISO=8859-1, and parses
			 * that string into key and values.
			 */
			private HttpHeaders parseHeaders() {
				if (this.buffers.isEmpty()) {
					return HttpHeaders.EMPTY;
				}
				DataBuffer joined = this.buffers.get(0).factory().join(this.buffers);
				this.buffers.clear();
				String string = joined.toString(InternalParser.this.headersCharset);
				DataBufferUtils.release(joined);
				String[] lines = string.split(HEADER_ENTRY_SEPARATOR);
				HttpHeaders result = new HttpHeaders();
				for (String line : lines) {
					int idx = line.indexOf(':');
					if (idx != -1) {
						String name = line.substring(0, idx);
						String value = line.substring(idx + 1);
						while (value.startsWith(" ")) {
							value = value.substring(1);
						}
						result.add(name, value);
					}
				}
				return result;
			}

			@Override
			public void complete() {
				changeState(DisposedState.INSTANCE, null);
				InternalParser.this.listener.onError(new HttpMessageConversionException("Could not find end of headers"));
			}

			@Override
			public void dispose() {
				this.buffers.forEach(DataBufferUtils::release);
			}

			@Override
			public String toString() {
				return "HEADERS";
			}

		}

		/**
		 * The state of the parser dealing with multipart bodies. Relays
		 * data buffers as {@link PartListener#onBody(DataBuffer, boolean)}
		 * until the boundary is found (or rather: {@code CR LF - - boundary}).
		 */
		private final class BodyState implements State {

			private final DataBufferUtils.Matcher boundaryMatcher;

			private final int boundaryLength;

			private final Deque<DataBuffer> queue = new ArrayDeque<>();

			public BodyState() {
				byte[] delimiter = concat(CR_LF, TWO_HYPHENS, InternalParser.this.boundary);
				this.boundaryMatcher = DataBufferUtils.matcher(delimiter);
				this.boundaryLength = delimiter.length;
			}

			/**
			 * Checks whether the (end of the) needle {@code CR LF - - boundary}
			 * can be found in {@code buffer}. If found, the needle can overflow into the
			 * previous buffer, so we calculate the length and slice the current
			 * and previous buffers accordingly. We then change to {@link HeadersState}
			 * and pass on the remainder of {@code buffer}. If the needle is not found, we
			 * enqueue {@code buffer}.
			 */
			@Override
			public void data(DataBuffer buffer) {
				int endIdx = this.boundaryMatcher.match(buffer);
				if (endIdx != -1) {
					DataBuffer boundaryBuffer = buffer.split(endIdx + 1);
					if (logger.isTraceEnabled()) {
						logger.trace("Boundary found @" + endIdx + " in " + buffer);
					}
					int len = endIdx - this.boundaryLength + 1 - boundaryBuffer.readPosition();
					if (len > 0) {
						// whole boundary in buffer.
						// slice off the body part, and flush
						DataBuffer body = boundaryBuffer.split(len);
						DataBufferUtils.release(boundaryBuffer);
						enqueue(body);
						flush();
					}
					else if (len < 0) {
						// boundary spans multiple buffers, and we've just found the end
						// iterate over buffers in reverse order
						DataBufferUtils.release(boundaryBuffer);
						DataBuffer prev;
						while ((prev = this.queue.pollLast()) != null) {
							int prevByteCount = prev.readableByteCount();
							int prevLen = prevByteCount + len;
							if (prevLen >= 0) {
								// slice body part of previous buffer, and flush it
								DataBuffer body = prev.split(prevLen + prev.readPosition());
								DataBufferUtils.release(prev);
								enqueue(body);
								flush();
								break;
							}
							else {
								// previous buffer only contains boundary bytes
								DataBufferUtils.release(prev);
								len += prevByteCount;
							}
						}
					}
					else /* if (len == 0) */ {
						// buffer starts with complete delimiter, flush out the previous buffers
						DataBufferUtils.release(boundaryBuffer);
						flush();
					}

					changeState(new HeadersState(), buffer);
				}
				else {
					enqueue(buffer);
				}
			}

			/**
			 * Store the given buffer. Emit buffers that cannot contain boundary bytes,
			 * by iterating over the queue in reverse order, and summing buffer sizes.
			 * The first buffer that passes the boundary length and subsequent buffers
			 * are emitted (in the correct, non-reverse order).
			 */
			private void enqueue(DataBuffer buf) {
				this.queue.add(buf);

				int len = 0;
				Deque<DataBuffer> emit = new ArrayDeque<>();
				for (Iterator<DataBuffer> iterator = this.queue.descendingIterator(); iterator.hasNext(); ) {
					DataBuffer previous = iterator.next();
					if (len > this.boundaryLength) {
						// addFirst to negate iterating in reverse order
						emit.addFirst(previous);
						iterator.remove();
					}
					len += previous.readableByteCount();
				}
				emit.forEach(buffer -> InternalParser.this.listener.onBody(buffer, false));
			}

			private void flush() {
				for (Iterator<DataBuffer> iterator = this.queue.iterator(); iterator.hasNext(); ) {
					DataBuffer buffer = iterator.next();
					boolean last = !iterator.hasNext();
					InternalParser.this.listener.onBody(buffer, last);
				}
				this.queue.clear();
			}

			@Override
			public void complete() {
				changeState(DisposedState.INSTANCE, null);
				String msg = "Could not find end of body (␍␊--" +
						new String(InternalParser.this.boundary, StandardCharsets.UTF_8) +
						")";
				InternalParser.this.listener.onError(new HttpMessageConversionException(msg));
			}

			@Override
			public void dispose() {
				this.queue.forEach(DataBufferUtils::release);
				this.queue.clear();
			}

			@Override
			public String toString() {
				return "BODY";
			}
		}

		/**
		 * The state of the parser when finished, either due to seeing the final
		 * boundary or to a malformed message. Releases all incoming buffers.
		 */
		private static final class DisposedState implements State {

			public static final DisposedState INSTANCE = new DisposedState();

			private DisposedState() {
			}

			@Override
			public void data(DataBuffer buf) {
				DataBufferUtils.release(buf);
			}

			@Override
			public void complete() {
			}

			@Override
			public String toString() {
				return "DISPOSED";
			}
		}

	}


	/**
	 * Listen for part events while parsing the inbound stream of data.
	 */
	interface PartListener {

		/**
		 * Handle {@link HttpHeaders} for a part.
		 */
		void onHeaders(HttpHeaders headers);

		/**
		 * Handle a piece of data for a body part.
		 * @param buffer a chunk of body
		 * @param last   whether this is the last chunk for the part
		 */
		void onBody(DataBuffer buffer, boolean last);

		/**
		 * Handle the completion event for the Multipart message.
		 */
		void onComplete();

		/**
		 * Handle any error thrown during the parsing phase.
		 */
		void onError(Throwable error);
	}

}
