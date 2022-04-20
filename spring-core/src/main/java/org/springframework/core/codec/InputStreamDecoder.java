package org.springframework.core.codec;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/**
 * Decoder that translates data buffers to an {@link InputStream}.
 */
public class InputStreamDecoder extends AbstractDataBufferDecoder<InputStream> {

	public static final String FAIL_FAST = InputStreamDecoder.class.getName() + ".FAIL_FAST";

	public InputStreamDecoder() {
		super(MimeTypeUtils.ALL);
	}

	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		return (elementType.resolve() == InputStream.class && super.canDecode(elementType, mimeType));
	}

	@Override
	public InputStream decode(DataBuffer dataBuffer, ResolvableType elementType,
							  @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		if (logger.isDebugEnabled()) {
			logger.debug(Hints.getLogPrefix(hints) + "Reading " + dataBuffer.readableByteCount() + " bytes");
		}
		return dataBuffer.asInputStream(true);
	}

	@Override
	public Mono<InputStream> decodeToMono(Publisher<DataBuffer> input, ResolvableType elementType,
										  @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		boolean failFast = hints == null || Boolean.TRUE.equals(hints.getOrDefault(FAIL_FAST, Boolean.TRUE));
		FlowBufferInputStream inputStream = new FlowBufferInputStream(getMaxInMemorySize(), failFast);
		Flux.from(input).subscribe(inputStream);

		return Mono.just(inputStream);
	}

	static class FlowBufferInputStream extends InputStream implements Subscriber<DataBuffer> {

		private static final Object END = new Object();

		private final AtomicBoolean closed = new AtomicBoolean();

		private final BlockingQueue<Object> backlog;

		private final int maximumMemorySize;

		private final boolean failFast;

		private final AtomicInteger buffered = new AtomicInteger();

		@Nullable
		private InputStreamWithSize current = new InputStreamWithSize(0, InputStream.nullInputStream());

		@Nullable
		private Subscription subscription;

		FlowBufferInputStream(int maximumMemorySize, boolean failFast) {
			this.backlog = new LinkedBlockingDeque<>();
			this.maximumMemorySize = maximumMemorySize;
			this.failFast = failFast;
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			this.subscription = subscription;
			if (this.closed.get()) {
				subscription.cancel();
			} else {
				subscription.request(1);
			}
		}

		@Override
		public void onNext(DataBuffer buffer) {
			if (this.closed.get()) {
				DataBufferUtils.release(buffer);
				return;
			}
			int readableByteCount = buffer.readableByteCount();
			int current = this.buffered.addAndGet(readableByteCount);
			if (current < this.maximumMemorySize) {
				this.subscription.request(1);
			}
			InputStream stream = buffer.asInputStream(true);
			this.backlog.add(new InputStreamWithSize(readableByteCount, stream));
			if (this.closed.get()) {
				DataBufferUtils.release(buffer);
			}
		}

		@Override
		public void onError(Throwable throwable) {
			if (failFast) {
				Object next;
				while ((next = this.backlog.poll()) != null) {
					if (next instanceof InputStreamWithSize) {
						try {
							((InputStreamWithSize) next).inputStream.close();
						} catch (Throwable t) {
							throwable.addSuppressed(t);
						}
					}
				}
			}
			this.backlog.add(throwable);
		}

		@Override
		public void onComplete() {
			this.backlog.add(END);
		}

		private boolean forward() throws IOException {
			this.current.inputStream.close();
			try {
				Object next = this.backlog.take();
				if (next == END) {
					this.current = null;
					return true;
				} else if (next instanceof RuntimeException) {
					close();
					throw (RuntimeException) next;
				} else if (next instanceof IOException) {
					close();
					throw (IOException) next;
				} else if (next instanceof Throwable) {
					close();
					throw new IllegalStateException((Throwable) next);
				} else {
					int buffer = buffered.addAndGet(-this.current.size);
					if (buffer < this.maximumMemorySize) {
						this.subscription.request(1);
					}
					this.current = (InputStreamWithSize) next;
					return false;
				}
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public int read() throws IOException {
			if (this.closed.get()) {
				throw new IOException("closed");
			} else if (this.current == null) {
				return -1;
			}
			int read;
			while ((read = this.current.inputStream.read()) == -1) {
				if (forward()) {
					return -1;
				}
			}
			return read;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			Objects.checkFromIndexSize(off, len, b.length);
			if (this.closed.get()) {
				throw new IOException("closed");
			} else if (this.current == null) {
				return -1;
			} else if (len == 0) {
				return 0;
			}
			int sum = 0;
			do {
				int read = this.current.inputStream.read(b, off + sum, len - sum);
				if (read == -1) {
					if (sum > 0 && this.backlog.isEmpty()) {
						return sum;
					} else if (forward()) {
						return sum == 0 ? -1 : sum;
					}
				} else {
					sum += read;
				}
			} while (sum < len);
			return sum;
		}

		@Override
		public int available() throws IOException {
			if (this.closed.get()) {
				throw new IOException("closed");
			} else if (this.current == null) {
				return 0;
			}
			int available = this.current.inputStream.available();
			for (Object value : this.backlog) {
				if (value instanceof InputStreamWithSize) {
					available += ((InputStreamWithSize) value).inputStream.available();
				} else {
					break;
				}
			}
			return available;
		}

		@Override
		public void close() throws IOException {
			if (this.closed.compareAndSet(false, true)) {
				if (this.subscription != null) {
					this.subscription.cancel();
				}
				IOException exception = null;
				if (this.current != null) {
					try {
						this.current.inputStream.close();
					} catch (IOException e) {
						exception = e;
					}
				}
				for (Object value : this.backlog) {
					if (value instanceof InputStreamWithSize) {
						try {
							((InputStreamWithSize) value).inputStream.close();
						} catch (IOException e) {
							if (exception == null) {
								exception = e;
							} else {
								exception.addSuppressed(e);
							}
						}
					}
				}
				if (exception != null) {
					throw exception;
				}
			}
		}
	}

	static class InputStreamWithSize {

		final int size;

		final InputStream inputStream;

		InputStreamWithSize(int size, InputStream inputStream) {
			this.size = size;
			this.inputStream = inputStream;
		}
	}
}