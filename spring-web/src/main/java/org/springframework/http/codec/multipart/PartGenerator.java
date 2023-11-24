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

package org.springframework.http.codec.multipart;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.scheduler.Scheduler;
import reactor.util.context.Context;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.FastByteArrayOutputStream;

/**
 * Subscribes to a token stream (i.e. the result of
 * {@link MultipartParser#parse(Flux, byte[], int, Charset)}), and produces a flux of {@link Part} objects.
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
final class PartGenerator extends BaseSubscriber<MultipartParser.Token> {

	private static final Log logger = LogFactory.getLog(PartGenerator.class);

	private final AtomicReference<State> state = new AtomicReference<>(new InitialState());

	private final AtomicBoolean requestOutstanding = new AtomicBoolean();

	private final MonoSink<Part> sink;

	private final int maxInMemorySize;

	private final long maxDiskUsagePerPart;

	private final Mono<Path> fileStorageDirectory;

	private final Scheduler blockingOperationScheduler;


	private PartGenerator(MonoSink<Part> sink, int maxInMemorySize, long maxDiskUsagePerPart,
			Mono<Path> fileStorageDirectory, Scheduler blockingOperationScheduler) {

		this.sink = sink;
		this.maxInMemorySize = maxInMemorySize;
		this.maxDiskUsagePerPart = maxDiskUsagePerPart;
		this.fileStorageDirectory = fileStorageDirectory;
		this.blockingOperationScheduler = blockingOperationScheduler;
	}

	/**
	 * Creates parts from a given stream of tokens.
	 */
	public static Mono<Part> createPart(Flux<MultipartParser.Token> tokens, int maxInMemorySize,
			long maxDiskUsagePerPart, Mono<Path> fileStorageDirectory, Scheduler blockingOperationScheduler) {

		return Mono.create(sink -> {
			PartGenerator generator = new PartGenerator(sink, maxInMemorySize, maxDiskUsagePerPart,
					fileStorageDirectory, blockingOperationScheduler);

			sink.onCancel(generator);
			sink.onRequest(l -> generator.requestToken());
			tokens.subscribe(generator);
		});
	}

	@Override
	public Context currentContext() {
		return Context.of(this.sink.contextView());
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
			newPart(state, token.headers());
		}
		else {
			state.body(token.buffer());
		}
	}

	private void newPart(State currentState, HttpHeaders headers) {
		if (MultipartUtils.isFormField(headers)) {
			changeState(currentState, new FormFieldState(headers));
			requestToken();
		}
		else {
			changeState(currentState, new InMemoryState(headers));
			requestToken();
		}
	}

	@Override
	protected void hookOnComplete() {
		this.state.get().onComplete();
	}

	@Override
	protected void hookOnError(Throwable throwable) {
		this.state.get().error(throwable);
		changeStateInternal(DisposedState.INSTANCE);
		this.sink.error(throwable);
	}

	@Override
	public void dispose() {
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
		this.sink.success(part);
	}

	void emitError(Throwable t) {
		cancel();
		this.sink.error(t);
	}

	void requestToken() {
		if (upstream() != null &&
				this.state.get().canRequest() &&
				this.requestOutstanding.compareAndSet(false, true)) {
			request(1);
		}
	}

	/**
	 * Represents the internal state of the {@link PartGenerator} for
	 * creating a single {@link Part}.
	 * {@link State} instances are stateful, and created when a new
	 * {@link MultipartParser.HeadersToken} is accepted (see
	 * {@link #newPart(State, HttpHeaders)}).
	 * The following rules determine which state the creator will have:
	 * <ol>
	 * <li>If the part is a {@linkplain MultipartUtils#isFormField(HttpHeaders) form field},
	 * the creator will be in the {@link FormFieldState}.</li>
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
		 */
		void onComplete();

		/**
		 * Invoked when an error has been received.
		 */
		default void error(Throwable throwable) {
		}

		/**
		 * Indicates whether the current state is ready to accept a new token.
		 */
		default boolean canRequest() {
			return true;
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
		public void onComplete() {
		}

		@Override
		public String toString() {
			return "INITIAL";
		}
	}


	/**
	 * The creator state when a {@linkplain MultipartUtils#isFormField(HttpHeaders) form field} is received.
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
		public void onComplete() {
			byte[] bytes = this.value.toByteArrayUnsafe();
			String value = new String(bytes, MultipartUtils.charset(this.headers));
			emitPart(DefaultParts.formFieldPart(this.headers, value));
		}

		@Override
		public String toString() {
			return "FORM-FIELD";
		}

	}


	/**
	 * The creator state when not handling a form field.
	 * Stores all received buffers in a queue.
	 * If the byte count exceeds {@link #maxInMemorySize}, the creator state
	 * is changed to {@link CreateFileState}, and eventually to
	 * {@link WritingFileState}.
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
		public void onComplete() {
			emitMemoryPart();
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
		public void onComplete() {
			this.completed = true;
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
					newState.onComplete();
				}
			}
			else {
				MultipartUtils.closeChannel(newState.channel);
				MultipartUtils.deleteFile(newState.file);
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

		private volatile boolean deleteOnDispose = true;


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
				this.deleteOnDispose = false;
				WritingFileState newState = new WritingFileState(this);
				if (changeState(this, newState)) {
					newState.writeBuffer(dataBuffer);
				}
				else {
					MultipartUtils.closeChannel(this.channel);
					MultipartUtils.deleteFile(this.file);
					DataBufferUtils.release(dataBuffer);
				}
			}
			else {
				MultipartUtils.closeChannel(this.channel);
				MultipartUtils.deleteFile(this.file);
				DataBufferUtils.release(dataBuffer);
				emitError(new DataBufferLimitException(
						"Part exceeded the disk usage limit of " + PartGenerator.this.maxDiskUsagePerPart +
								" bytes"));
			}
		}

		@Override
		public void onComplete() {
			MultipartUtils.closeChannel(this.channel);
			this.deleteOnDispose = false;
			emitPart(DefaultParts.part(this.headers, this.file, PartGenerator.this.blockingOperationScheduler));
		}

		@Override
		public void dispose() {
			if (this.closeOnDispose) {
				MultipartUtils.closeChannel(this.channel);
			}
			if (this.deleteOnDispose) {
				MultipartUtils.deleteFile(this.file);
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

		private volatile boolean disposed;


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
		public void onComplete() {
			this.completed = true;
			State state = PartGenerator.this.state.get();
			// writeComplete might have changed our state to IdleFileState
			if (state != this) {
				state.onComplete();
			}
			else {
				this.completed = true;
			}
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
			if (this.disposed) {
				newState.dispose();
			}
			else if (changeState(this, newState)) {
				if (this.completed) {
					newState.onComplete();
				}
				else {
					requestToken();
				}
			}
			else {
				MultipartUtils.closeChannel(this.channel);
				MultipartUtils.deleteFile(this.file);
			}
		}

		@SuppressWarnings("BlockingMethodInNonBlockingContext")
		private Mono<Void> writeInternal(DataBuffer dataBuffer) {
			try {
				try (DataBuffer.ByteBufferIterator iterator = dataBuffer.readableByteBuffers()) {
					while (iterator.hasNext()) {
						ByteBuffer byteBuffer = iterator.next();
						while (byteBuffer.hasRemaining()) {
							this.channel.write(byteBuffer);
						}
					}
				}
				return Mono.empty();
			}
			catch (IOException ex) {
				MultipartUtils.closeChannel(this.channel);
				MultipartUtils.deleteFile(this.file);
				return Mono.error(ex);
			}
			finally {
				DataBufferUtils.release(dataBuffer);
			}
		}

		@Override
		public boolean canRequest() {
			return false;
		}

		@Override
		public void dispose() {
			this.disposed = true;
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
		public void onComplete() {
		}

		@Override
		public String toString() {
			return "DISPOSED";
		}

	}

}
