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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.FastByteArrayOutputStream;

/**
 * Subscribes to a token stream (i.e. the result of
 * {@link MultipartParser#parse(Flux, byte[], int)}, and produces a flux of {@link Part} objects.
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
final class PartGenerator extends BaseSubscriber<MultipartParser.Token> {

	private static final Log logger = LogFactory.getLog(PartGenerator.class);

	private final AtomicReference<State> state = new AtomicReference<>(new InitialState());

	private final AtomicInteger partCount = new AtomicInteger();

	private final AtomicBoolean requestOutstanding = new AtomicBoolean();

	private final FluxSink<Part> sink;

	private final int maxParts;

	private final boolean streaming;

	private final int maxInMemorySize;

	private final long maxDiskUsagePerPart;

	private final Mono<Path> fileStorageDirectory;

	private final Scheduler blockingOperationScheduler;


	private PartGenerator(FluxSink<Part> sink, int maxParts, int maxInMemorySize, long maxDiskUsagePerPart,
			boolean streaming, Mono<Path> fileStorageDirectory, Scheduler blockingOperationScheduler) {

		this.sink = sink;
		this.maxParts = maxParts;
		this.maxInMemorySize = maxInMemorySize;
		this.maxDiskUsagePerPart = maxDiskUsagePerPart;
		this.streaming = streaming;
		this.fileStorageDirectory = fileStorageDirectory;
		this.blockingOperationScheduler = blockingOperationScheduler;
	}

	/**
	 * Creates parts from a given stream of tokens.
	 */
	public static Flux<Part> createParts(Flux<MultipartParser.Token> tokens, int maxParts, int maxInMemorySize,
			long maxDiskUsagePerPart, boolean streaming, Mono<Path> fileStorageDirectory,
			Scheduler blockingOperationScheduler) {

		return Flux.create(sink -> {
			PartGenerator generator = new PartGenerator(sink, maxParts, maxInMemorySize, maxDiskUsagePerPart, streaming,
					fileStorageDirectory, blockingOperationScheduler);

			sink.onCancel(generator::onSinkCancel);
			sink.onRequest(l -> generator.requestToken());
			tokens.subscribe(generator);
		});
	}

	@Override
	protected void hookOnSubscribe(Subscription subscription) {
		requestToken();
	}

	@Override
	protected void hookOnNext(MultipartParser.Token token) {
		this.requestOutstanding.set(false);
		State state = this.state.get();
		if (token instanceof MultipartParser.HeadersToken) {
			// finish previous part
			state.partComplete(false);

			if (tooManyParts()) {
				return;
			}

			newPart(state, token.headers());
		}
		else {
			state.body(token.buffer());
		}
	}

	private void newPart(State currentState, HttpHeaders headers) {
		if (isFormField(headers)) {
			changeStateInternal(new FormFieldState(headers));
			requestToken();
		}
		else if (!this.streaming) {
			changeStateInternal(new InMemoryState(headers));
			requestToken();
		}
		else {
			Flux<DataBuffer> streamingContent = Flux.create(contentSink -> {
				State newState = new StreamingState(contentSink);
				if (changeState(currentState, newState)) {
					contentSink.onRequest(l -> requestToken());
					requestToken();
				}
			});
			emitPart(DefaultParts.part(headers, streamingContent));
		}
	}

	@Override
	protected void hookOnComplete() {
		this.state.get().partComplete(true);
	}

	@Override
	protected void hookOnError(Throwable throwable) {
		this.state.get().error(throwable);
		changeStateInternal(DisposedState.INSTANCE);
		this.sink.error(throwable);
	}

	private void onSinkCancel() {
		changeStateInternal(DisposedState.INSTANCE);
		cancel();
	}

	boolean changeState(State oldState, State newState) {
		if (this.state.compareAndSet(oldState, newState)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Changed state: " + oldState + " -> " + newState);
			}
			oldState.dispose();
			return true;
		}
		else {
			logger.warn("Could not switch from " + oldState +
					" to " + newState + "; current state:"
				+ this.state.get());
			return false;
		}
	}

	private void changeStateInternal(State newState) {
		if (this.state.get() == DisposedState.INSTANCE) {
			return;
		}
		State oldState = this.state.getAndSet(newState);
		if (logger.isTraceEnabled()) {
			logger.trace("Changed state: " + oldState + " -> " + newState);
		}
		oldState.dispose();
	}

	void emitPart(Part part) {
		if (logger.isTraceEnabled()) {
			logger.trace("Emitting: " + part);
		}
		this.sink.next(part);
	}

	void emitComplete() {
		this.sink.complete();
	}


	void emitError(Throwable t) {
		cancel();
		this.sink.error(t);
	}

	void requestToken() {
		if (upstream() != null &&
				!this.sink.isCancelled() &&
				this.sink.requestedFromDownstream() > 0 &&
				this.requestOutstanding.compareAndSet(false, true)) {
			request(1);
		}
	}

	private boolean tooManyParts() {
		int count = this.partCount.incrementAndGet();
		if (this.maxParts > 0 && count > this.maxParts) {
			emitError(new DecodingException("Too many parts (" + count + "/" + this.maxParts + " allowed)"));
			return true;
		}
		else {
			return false;
		}
	}

	private static boolean isFormField(HttpHeaders headers) {
		MediaType contentType = headers.getContentType();
		return (contentType == null || MediaType.TEXT_PLAIN.equalsTypeAndSubtype(contentType))
				&& headers.getContentDisposition().getFilename() == null;
	}

	/**
	 * Represents the internal state of the {@link PartGenerator} for
	 * creating a single {@link Part}.
	 * {@link State} instances are stateful, and created when a new
	 * {@link MultipartParser.HeadersToken} is accepted (see
	 * {@link #newPart(State, HttpHeaders)}.
	 * The following rules determine which state the creator will have:
	 * <ol>
	 * <li>If the part is a {@linkplain #isFormField(HttpHeaders) form field},
	 * the creator will be in the {@link FormFieldState}.</li>
	 * <li>If {@linkplain #streaming} is enabled, the creator will be in the
	 * {@link StreamingState}.</li>
	 * <li>Otherwise, the creator will initially be in the
	 * {@link InMemoryState}, but will switch over to {@link CreateFileState}
	 * when the part byte count exceeds {@link #maxInMemorySize},
	 * then to {@link WritingFileState} (to write the memory contents),
	 * and finally {@link IdleFileState}, which switches back to
	 * {@link WritingFileState} when more body data comes in.</li>
	 * </ol>
	 */
	private interface State {

		/**
		 * Invoked when a {@link MultipartParser.BodyToken} is received.
		 */
		void body(DataBuffer dataBuffer);

		/**
		 * Invoked when all tokens for the part have been received.
		 * @param finalPart {@code true} if this was the last part (and
		 * {@link #emitComplete()} should be called; {@code false} otherwise
		 */
		void partComplete(boolean finalPart);

		/**
		 * Invoked when an error has been received.
		 */
		default void error(Throwable throwable) {
		}

		/**
		 * Cleans up any state.
		 */
		default void dispose() {
		}
	}


	/**
	 * The initial state of the creator. Throws an exception for {@link #body(DataBuffer)}.
	 */
	private final class InitialState implements State {

		private InitialState() {
		}

		@Override
		public void body(DataBuffer dataBuffer) {
			DataBufferUtils.release(dataBuffer);
			emitError(new IllegalStateException("Body token not expected"));
		}

		@Override
		public void partComplete(boolean finalPart) {
			if (finalPart) {
				emitComplete();
			}
		}

		@Override
		public String toString() {
			return "INITIAL";
		}
	}


	/**
	 * The creator state when a {@linkplain #isFormField(HttpHeaders) form field} is received.
	 * Stores all body buffers in memory (up until {@link #maxInMemorySize}).
	 */
	private final class FormFieldState implements State {

		private final FastByteArrayOutputStream value = new FastByteArrayOutputStream();

		private final HttpHeaders headers;

		public FormFieldState(HttpHeaders headers) {
			this.headers = headers;
		}

		@Override
		public void body(DataBuffer dataBuffer) {
			int size = this.value.size() + dataBuffer.readableByteCount();
			if (PartGenerator.this.maxInMemorySize == -1 ||
					size < PartGenerator.this.maxInMemorySize) {
				store(dataBuffer);
				requestToken();
			}
			else {
				DataBufferUtils.release(dataBuffer);
				emitError(new DataBufferLimitException("Form field value exceeded the memory usage limit of " +
						PartGenerator.this.maxInMemorySize + " bytes"));
			}
		}

		private void store(DataBuffer dataBuffer) {
			try {
				byte[] bytes = new byte[dataBuffer.readableByteCount()];
				dataBuffer.read(bytes);
				this.value.write(bytes);
			}
			catch (IOException ex) {
				emitError(ex);
			}
			finally {
				DataBufferUtils.release(dataBuffer);
			}
		}

		@Override
		public void partComplete(boolean finalPart) {
			byte[] bytes = this.value.toByteArrayUnsafe();
			String value = new String(bytes, MultipartUtils.charset(this.headers));
			emitPart(DefaultParts.formFieldPart(this.headers, value));
			if (finalPart) {
				emitComplete();
			}
		}

		@Override
		public String toString() {
			return "FORM-FIELD";
		}

	}


	/**
	 * The creator state when {@link #streaming} is {@code true} (and not
	 * handling a form field). Relays all received buffers to a sink.
	 */
	private final class StreamingState implements State {

		private final FluxSink<DataBuffer> bodySink;

		public StreamingState(FluxSink<DataBuffer> bodySink) {
			this.bodySink = bodySink;
		}

		@Override
		public void body(DataBuffer dataBuffer) {
			if (!this.bodySink.isCancelled()) {
				this.bodySink.next(dataBuffer);
				if (this.bodySink.requestedFromDownstream() > 0) {
					requestToken();
				}
			}
			else {
				DataBufferUtils.release(dataBuffer);
				// even though the body sink is canceled, the (outer) part sink
				// might not be, so request another token
				requestToken();
			}
		}

		@Override
		public void partComplete(boolean finalPart) {
			if (!this.bodySink.isCancelled()) {
				this.bodySink.complete();
			}
			if (finalPart) {
				emitComplete();
			}
		}

		@Override
		public void error(Throwable throwable) {
			if (!this.bodySink.isCancelled()) {
				this.bodySink.error(throwable);
			}
		}

		@Override
		public String toString() {
			return "STREAMING";
		}

	}


	/**
	 * The creator state when {@link #streaming} is {@code false} (and not
	 * handling a form field). Stores all received buffers in a queue.
	 * If the byte count exceeds {@link #maxInMemorySize}, the creator state
	 * is changed to {@link CreateFileState}, and eventually to
	 * {@link CreateFileState}.
	 */
	private final class InMemoryState implements State {

		private final AtomicLong byteCount = new AtomicLong();

		private final Queue<DataBuffer> content = new ConcurrentLinkedQueue<>();

		private final HttpHeaders headers;

		private volatile boolean releaseOnDispose = true;


		public InMemoryState(HttpHeaders headers) {
			this.headers = headers;
		}

		@Override
		public void body(DataBuffer dataBuffer) {
			long prevCount = this.byteCount.get();
			long count = this.byteCount.addAndGet(dataBuffer.readableByteCount());
			if (PartGenerator.this.maxInMemorySize == -1 ||
					count <= PartGenerator.this.maxInMemorySize) {
				storeBuffer(dataBuffer);
			}
			else if (prevCount <= PartGenerator.this.maxInMemorySize) {
				switchToFile(dataBuffer, count);
			}
			else {
				DataBufferUtils.release(dataBuffer);
				emitError(new IllegalStateException("Body token not expected"));
			}
		}

		private void storeBuffer(DataBuffer dataBuffer) {
			this.content.add(dataBuffer);
			requestToken();
		}

		private void switchToFile(DataBuffer current, long byteCount) {
			List<DataBuffer> content = new ArrayList<>(this.content);
			content.add(current);
			this.releaseOnDispose = false;

			CreateFileState newState = new CreateFileState(this.headers, content, byteCount);
			if (changeState(this, newState)) {
				newState.createFile();
			}
			else {
				content.forEach(DataBufferUtils::release);
			}
		}

		@Override
		public void partComplete(boolean finalPart) {
			emitMemoryPart();
			if (finalPart) {
				emitComplete();
			}
		}

		private void emitMemoryPart() {
			byte[] bytes = new byte[(int) this.byteCount.get()];
			int idx = 0;
			for (DataBuffer buffer : this.content) {
				int len = buffer.readableByteCount();
				buffer.read(bytes, idx, len);
				idx += len;
				DataBufferUtils.release(buffer);
			}
			this.content.clear();
			Flux<DataBuffer> content = Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
			emitPart(DefaultParts.part(this.headers, content));
		}

		@Override
		public void dispose() {
			if (this.releaseOnDispose) {
				this.content.forEach(DataBufferUtils::release);
			}
		}

		@Override
		public String toString() {
			return "IN-MEMORY";
		}

	}


	/**
	 * The creator state when waiting for a temporary file to be created.
	 * {@link InMemoryState} initially switches to this state when the byte
	 * count exceeds {@link #maxInMemorySize}, and then calls
	 * {@link #createFile()} to switch to {@link WritingFileState}.
	 */
	private final class CreateFileState implements State {

		private final HttpHeaders headers;

		private final Collection<DataBuffer> content;

		private final long byteCount;

		private volatile boolean completed;

		private volatile boolean finalPart;

		private volatile boolean releaseOnDispose = true;


		public CreateFileState(HttpHeaders headers, Collection<DataBuffer> content, long byteCount) {
			this.headers = headers;
			this.content = content;
			this.byteCount = byteCount;
		}

		@Override
		public void body(DataBuffer dataBuffer) {
			DataBufferUtils.release(dataBuffer);
			emitError(new IllegalStateException("Body token not expected"));
		}

		@Override
		public void partComplete(boolean finalPart) {
			this.completed = true;
			this.finalPart = finalPart;
		}

		public void createFile() {
			PartGenerator.this.fileStorageDirectory
					.map(this::createFileState)
					.subscribeOn(PartGenerator.this.blockingOperationScheduler)
					.subscribe(this::fileCreated, PartGenerator.this::emitError);
		}

		private WritingFileState createFileState(Path directory) {
			try {
				Path tempFile = Files.createTempFile(directory, null, ".multipart");
				if (logger.isTraceEnabled()) {
					logger.trace("Storing multipart data in file " + tempFile);
				}
				WritableByteChannel channel = Files.newByteChannel(tempFile, StandardOpenOption.WRITE);
				return new WritingFileState(this, tempFile, channel);
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Could not create temp file in " + directory, ex);
			}
		}

		private void fileCreated(WritingFileState newState) {
			this.releaseOnDispose = false;

			if (changeState(this, newState)) {

				newState.writeBuffers(this.content);

				if (this.completed) {
					newState.partComplete(this.finalPart);
				}
			}
			else {
				MultipartUtils.closeChannel(newState.channel);
				this.content.forEach(DataBufferUtils::release);
			}
		}

		@Override
		public void dispose() {
			if (this.releaseOnDispose) {
				this.content.forEach(DataBufferUtils::release);
			}
		}

		@Override
		public String toString() {
			return "CREATE-FILE";
		}


	}

	private final class IdleFileState implements State {

		private final HttpHeaders headers;

		private final Path file;

		private final WritableByteChannel channel;

		private final AtomicLong byteCount;

		private volatile boolean closeOnDispose = true;


		public IdleFileState(WritingFileState state) {
			this.headers = state.headers;
			this.file = state.file;
			this.channel = state.channel;
			this.byteCount = state.byteCount;
		}

		@Override
		public void body(DataBuffer dataBuffer) {
			long count = this.byteCount.addAndGet(dataBuffer.readableByteCount());
			if (PartGenerator.this.maxDiskUsagePerPart == -1 || count <= PartGenerator.this.maxDiskUsagePerPart) {

				this.closeOnDispose = false;
				WritingFileState newState = new WritingFileState(this);
				if (changeState(this, newState)) {
					newState.writeBuffer(dataBuffer);
				}
				else {
					MultipartUtils.closeChannel(this.channel);
					DataBufferUtils.release(dataBuffer);
				}
			}
			else {
				DataBufferUtils.release(dataBuffer);
				emitError(new DataBufferLimitException(
						"Part exceeded the disk usage limit of " + PartGenerator.this.maxDiskUsagePerPart +
								" bytes"));
			}
		}

		@Override
		public void partComplete(boolean finalPart) {
			MultipartUtils.closeChannel(this.channel);
			Flux<DataBuffer> content = partContent();
			emitPart(DefaultParts.part(this.headers, content));
			if (finalPart) {
				emitComplete();
			}
		}

		private Flux<DataBuffer> partContent() {
			return DataBufferUtils
					.readByteChannel(
							() -> Files.newByteChannel(this.file, StandardOpenOption.READ),
							DefaultDataBufferFactory.sharedInstance, 1024)
					.subscribeOn(PartGenerator.this.blockingOperationScheduler);
		}

		@Override
		public void dispose() {
			if (this.closeOnDispose) {
				MultipartUtils.closeChannel(this.channel);
			}
		}


		@Override
		public String toString() {
			return "IDLE-FILE";
		}

	}

	private final class WritingFileState implements State {


		private final HttpHeaders headers;

		private final Path file;

		private final WritableByteChannel channel;

		private final AtomicLong byteCount;

		private volatile boolean completed;

		private volatile boolean finalPart;


		public WritingFileState(CreateFileState state, Path file, WritableByteChannel channel) {
			this.headers = state.headers;
			this.file = file;
			this.channel = channel;
			this.byteCount = new AtomicLong(state.byteCount);
		}

		public WritingFileState(IdleFileState state) {
			this.headers = state.headers;
			this.file = state.file;
			this.channel = state.channel;
			this.byteCount = state.byteCount;
		}

		@Override
		public void body(DataBuffer dataBuffer) {
			DataBufferUtils.release(dataBuffer);
			emitError(new IllegalStateException("Body token not expected"));
		}

		@Override
		public void partComplete(boolean finalPart) {
			this.completed = true;
			this.finalPart = finalPart;
		}

		public void writeBuffer(DataBuffer dataBuffer) {
			Mono.just(dataBuffer)
					.flatMap(this::writeInternal)
					.subscribeOn(PartGenerator.this.blockingOperationScheduler)
					.subscribe(null,
					PartGenerator.this::emitError,
					this::writeComplete);
		}

		public void writeBuffers(Iterable<DataBuffer> dataBuffers) {
			Flux.fromIterable(dataBuffers)
					.concatMap(this::writeInternal)
					.then()
					.subscribeOn(PartGenerator.this.blockingOperationScheduler)
					.subscribe(null,
							PartGenerator.this::emitError,
							this::writeComplete);
		}

		private void writeComplete() {
			IdleFileState newState = new IdleFileState(this);
			if (this.completed) {
				newState.partComplete(this.finalPart);
			}
			else if (changeState(this, newState)) {
				requestToken();
			}
			else {
				MultipartUtils.closeChannel(this.channel);
			}
		}

		@SuppressWarnings("BlockingMethodInNonBlockingContext")
		private Mono<Void> writeInternal(DataBuffer dataBuffer) {
			try {
				ByteBuffer byteBuffer = dataBuffer.asByteBuffer();
				while (byteBuffer.hasRemaining()) {
					this.channel.write(byteBuffer);
				}
				return Mono.empty();
			}
			catch (IOException ex) {
				return Mono.error(ex);
			}
			finally {
				DataBufferUtils.release(dataBuffer);
			}
		}

		@Override
		public String toString() {
			return "WRITE-FILE";
		}
	}


	private static final class DisposedState implements State {

		public static final DisposedState INSTANCE = new DisposedState();

		private DisposedState() {
		}

		@Override
		public void body(DataBuffer dataBuffer) {
			DataBufferUtils.release(dataBuffer);
		}

		@Override
		public void partComplete(boolean finalPart) {
		}

		@Override
		public String toString() {
			return "DISPOSED";
		}

	}

}
