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

package org.springframework.http.codec.multipart;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;

/**
 * Subscribes to a buffer stream and produces a flux of {@link Token} instances.
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
final class MultipartParser extends BaseSubscriber<DataBuffer> {

	private static final byte CR = '\r';

	private static final byte LF = '\n';

	private static final byte[] CR_LF = {CR, LF};

	private static final byte HYPHEN = '-';

	private static final byte[] TWO_HYPHENS = {HYPHEN, HYPHEN};

	private static final String HEADER_ENTRY_SEPARATOR = "\\r\\n";

	private static final Log logger = LogFactory.getLog(MultipartParser.class);

	private final AtomicReference<State> state;

	private final FluxSink<Token> sink;

	private final byte[] boundary;

	private final int maxHeadersSize;

	private final AtomicBoolean requestOutstanding = new AtomicBoolean();


	private MultipartParser(FluxSink<Token> sink, byte[] boundary, int maxHeadersSize) {
		this.sink = sink;
		this.boundary = boundary;
		this.maxHeadersSize = maxHeadersSize;
		this.state = new AtomicReference<>(new PreambleState());
	}

	/**
	 * Parses the given stream of {@link DataBuffer} objects into a stream of {@link Token} objects.
	 * @param buffers the input buffers
	 * @param boundary the multipart boundary, as found in the {@code Content-Type} header
	 * @param maxHeadersSize the maximum buffered header size
	 * @return a stream of parsed tokens
	 */
	public static Flux<Token> parse(Flux<DataBuffer> buffers, byte[] boundary, int maxHeadersSize) {
		return Flux.create(sink -> {
			MultipartParser parser = new MultipartParser(sink, boundary, maxHeadersSize);
			sink.onCancel(parser::onSinkCancel);
			sink.onRequest(n -> parser.requestBuffer());
			buffers.subscribe(parser);
		});
	}

	@Override
	protected void hookOnSubscribe(Subscription subscription) {
		requestBuffer();
	}

	@Override
	protected void hookOnNext(DataBuffer value) {
		this.requestOutstanding.set(false);
		this.state.get().onNext(value);
	}

	@Override
	protected void hookOnComplete() {
		this.state.get().onComplete();
	}

	@Override
	protected void hookOnError(Throwable throwable) {
		State oldState = this.state.getAndSet(DisposedState.INSTANCE);
		oldState.dispose();
		this.sink.error(throwable);
	}

	private void onSinkCancel() {
		State oldState = this.state.getAndSet(DisposedState.INSTANCE);
		oldState.dispose();
		cancel();
	}

	boolean changeState(State oldState, State newState, @Nullable DataBuffer remainder) {
		if (this.state.compareAndSet(oldState, newState)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Changed state: " + oldState + " -> " + newState);
			}
			oldState.dispose();
			if (remainder != null) {
				if (remainder.readableByteCount() > 0) {
					newState.onNext(remainder);
				}
				else {
					DataBufferUtils.release(remainder);
					requestBuffer();
				}
			}
			return true;
		}
		else {
			DataBufferUtils.release(remainder);
			return false;
		}
	}

	void emitHeaders(HttpHeaders headers) {
		if (logger.isTraceEnabled()) {
			logger.trace("Emitting headers: " + headers);
		}
		this.sink.next(new HeadersToken(headers));
	}

	void emitBody(DataBuffer buffer) {
		if (logger.isTraceEnabled()) {
			logger.trace("Emitting body: " + buffer);
		}
		this.sink.next(new BodyToken(buffer));
	}

	void emitError(Throwable t) {
		cancel();
		this.sink.error(t);
	}

	void emitComplete() {
		cancel();
		this.sink.complete();
	}

	private void requestBuffer() {
		if (upstream() != null &&
				!this.sink.isCancelled() &&
				this.sink.requestedFromDownstream() > 0 &&
				this.requestOutstanding.compareAndSet(false, true)) {
			request(1);
		}
	}


	/**
	 * Represents the output of {@link #parse(Flux, byte[], int)}.
	 */
	public abstract static class Token {

		public abstract HttpHeaders headers();

		public abstract DataBuffer buffer();
	}


	/**
	 * Represents a token that contains {@link HttpHeaders}.
	 */
	public final static class HeadersToken extends Token {

		private final HttpHeaders headers;

		public HeadersToken(HttpHeaders headers) {
			this.headers = headers;
		}

		@Override
		public HttpHeaders headers() {
			return this.headers;
		}

		@Override
		public DataBuffer buffer() {
			throw new IllegalStateException();
		}
	}


	/**
	 * Represents a token that contains {@link DataBuffer}.
	 */
	public final static class BodyToken extends Token {

		private final DataBuffer buffer;

		public BodyToken(DataBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public HttpHeaders headers() {
			throw new IllegalStateException();
		}

		@Override
		public DataBuffer buffer() {
			return this.buffer;
		}
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
	 * For malformed messages the flow ends in DISPOSED, and also when the
	 * sink is {@linkplain #onSinkCancel() cancelled}.
	 */
	private interface State {

		void onNext(DataBuffer buf);

		void onComplete();

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


		public PreambleState() {
			this.firstBoundary = DataBufferUtils.matcher(
					MultipartUtils.concat(TWO_HYPHENS, MultipartParser.this.boundary));
		}

		/**
		 * Looks for the first boundary in the given buffer. If found, changes
		 * state to {@link HeadersState}, and passes on the remainder of the
		 * buffer.
		 */
		@Override
		public void onNext(DataBuffer buf) {
			int endIdx = this.firstBoundary.match(buf);
			if (endIdx != -1) {
				if (logger.isTraceEnabled()) {
					logger.trace("First boundary found @" + endIdx + " in " + buf);
				}
				DataBuffer headersBuf = MultipartUtils.sliceFrom(buf, endIdx);
				DataBufferUtils.release(buf);

				changeState(this, new HeadersState(), headersBuf);
			}
			else {
				DataBufferUtils.release(buf);
				requestBuffer();
			}
		}

		@Override
		public void onComplete() {
			if (changeState(this, DisposedState.INSTANCE, null)) {
				emitError(new DecodingException("Could not find first boundary"));
			}
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

		private final DataBufferUtils.Matcher endHeaders = DataBufferUtils.matcher(MultipartUtils.concat(CR_LF, CR_LF));

		private final AtomicInteger byteCount = new AtomicInteger();

		private final List<DataBuffer> buffers = new ArrayList<>();


		/**
		 * First checks whether the multipart boundary leading to this state
		 * was the final boundary, or whether {@link #maxHeadersSize} is
		 * exceeded. Then looks for the header-body boundary
		 * ({@code CR LF CR LF}) in the given buffer. If found, convert
		 * all buffers collected so far into a {@link HttpHeaders} object
		 * and changes to {@link BodyState}, passing the remainder of the
		 * buffer. If the boundary is not found, the buffer is collected.
		 */
		@Override
		public void onNext(DataBuffer buf) {
			long prevCount = this.byteCount.get();
			long count = this.byteCount.addAndGet(buf.readableByteCount());
			if (prevCount < 2 && count >= 2) {
				if (isLastBoundary(buf)) {
					if (logger.isTraceEnabled()) {
						logger.trace("Last boundary found in " + buf);
					}

					if (changeState(this, DisposedState.INSTANCE, buf)) {
						emitComplete();
					}
					return;
				}
			}
			else if (count > MultipartParser.this.maxHeadersSize) {
				if (changeState(this, DisposedState.INSTANCE, buf)) {
					emitError(new DataBufferLimitException("Part headers exceeded the memory usage limit of " +
							MultipartParser.this.maxHeadersSize + " bytes"));
				}
				return;
			}
			int endIdx = this.endHeaders.match(buf);
			if (endIdx != -1) {
				if (logger.isTraceEnabled()) {
					logger.trace("End of headers found @" + endIdx + " in " + buf);
				}
				DataBuffer headerBuf = MultipartUtils.sliceTo(buf, endIdx);
				this.buffers.add(headerBuf);
				DataBuffer bodyBuf = MultipartUtils.sliceFrom(buf, endIdx);
				DataBufferUtils.release(buf);

				emitHeaders(parseHeaders());
				// TODO: no need to check result of changeState, no further statements
				changeState(this, new BodyState(), bodyBuf);
			}
			else {
				this.buffers.add(buf);
				requestBuffer();
			}
		}

		/**
		 * If the given buffer is the first buffer, check whether it starts with {@code --}.
		 * If it is the second buffer, check whether it makes up {@code --} together with the first buffer.
		 */
		private boolean isLastBoundary(DataBuffer buf) {
			return (this.buffers.isEmpty() &&
					buf.readableByteCount() >= 2 &&
					buf.getByte(0) == HYPHEN && buf.getByte(1) == HYPHEN)
					||
					(this.buffers.size() == 1 &&
							this.buffers.get(0).readableByteCount() == 1 &&
							this.buffers.get(0).getByte(0) == HYPHEN &&
							buf.readableByteCount() >= 1 &&
							buf.getByte(0) == HYPHEN);
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
			String string = joined.toString(StandardCharsets.ISO_8859_1);
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
		public void onComplete() {
			if (changeState(this, DisposedState.INSTANCE, null)) {
				emitError(new DecodingException("Could not find end of headers"));
			}
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
	 * data buffers as {@link BodyToken} until the boundary is found (or
	 * rather: {@code CR LF - - boundary}.
	 */
	private final class BodyState implements State {

		private final DataBufferUtils.Matcher boundary;

		private final AtomicReference<DataBuffer> previous = new AtomicReference<>();

		public BodyState() {
			this.boundary = DataBufferUtils.matcher(
					MultipartUtils.concat(CR_LF, TWO_HYPHENS, MultipartParser.this.boundary));
		}

		/**
		 * Checks whether the (end of the) needle {@code CR LF - - boundary}
		 * can be found in {@code buffer}. If found, the needle can overflow into the
		 * previous buffer, so we calculate the length and slice the current
		 * and previous buffers accordingly. We then change to {@link HeadersState}
		 * and pass on the remainder of {@code buffer}. If the needle is not found, we
		 * make {@code buffer} the previous buffer.
		 */
		@Override
		public void onNext(DataBuffer buffer) {
			int endIdx = this.boundary.match(buffer);
			if (endIdx != -1) {
				if (logger.isTraceEnabled()) {
					logger.trace("Boundary found @" + endIdx + " in " + buffer);
				}
				int len = endIdx - buffer.readPosition() - this.boundary.delimiter().length + 1;
				if (len > 0) {
					// buffer contains complete delimiter, let's slice it and flush it
					DataBuffer body = buffer.retainedSlice(buffer.readPosition(), len);
					enqueue(body);
					enqueue(null);
				}
				else if (len < 0) {
					// buffer starts with the end of the delimiter, let's slice the previous buffer and flush it
					DataBuffer previous = this.previous.get();
					int prevLen = previous.readableByteCount() + len;
					if (prevLen > 0) {
						DataBuffer body = previous.retainedSlice(previous.readPosition(), prevLen);
						DataBufferUtils.release(previous);
						this.previous.set(body);
						enqueue(null);
					}
					else {
						DataBufferUtils.release(previous);
						this.previous.set(null);
					}
				}
				else /* if (sliceLength == 0) */ {
					// buffer starts with complete delimiter, flush out the previous buffer
					enqueue(null);
				}

				DataBuffer remainder = MultipartUtils.sliceFrom(buffer, endIdx);
				DataBufferUtils.release(buffer);

				changeState(this, new HeadersState(), remainder);
			}
			else {
				enqueue(buffer);
				requestBuffer();
			}
		}

		/**
		 * Stores the given buffer and sends out the previous buffer.
		 */
		private void enqueue(@Nullable DataBuffer buf) {
			DataBuffer previous = this.previous.getAndSet(buf);
			if (previous != null) {
				emitBody(previous);
			}
		}

		@Override
		public void onComplete() {
			if (changeState(this, DisposedState.INSTANCE, null)) {
				emitError(new DecodingException("Could not find end of body"));
			}
		}

		@Override
		public void dispose() {
			DataBuffer previous = this.previous.getAndSet(null);
			if (previous != null) {
				DataBufferUtils.release(previous);
			}
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
		public void onNext(DataBuffer buf) {
			DataBufferUtils.release(buf);
		}

		@Override
		public void onComplete() {
		}

		@Override
		public String toString() {
			return "DISPOSED";
		}
	}


}
