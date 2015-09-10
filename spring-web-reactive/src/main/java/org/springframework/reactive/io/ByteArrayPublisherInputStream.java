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

package org.springframework.reactive.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.reactive.util.PublisherSignal;
import org.springframework.util.Assert;

/**
 * {@code InputStream} implementation based on a byte array {@link Publisher}.
 * @author Arjen Poutsma
 */
public class ByteArrayPublisherInputStream extends InputStream {

	private final BlockingQueue<PublisherSignal<byte[]>> queue =
			new LinkedBlockingQueue<>();

	private ByteArrayInputStream currentStream;

	private boolean completed;


	/**
	 * Creates a new {@code ByteArrayPublisherInputStream} based on the given publisher.
	 * @param publisher the publisher to use
	 */
	public ByteArrayPublisherInputStream(Publisher<byte[]> publisher) {
		this(publisher, 1);
	}

	/**
	 * Creates a new {@code ByteArrayPublisherInputStream} based on the given publisher.
	 * @param publisher the publisher to use
	 * @param requestSize the {@linkplain Subscription#request(long) request size} to use
	 * on the publisher
	 */
	public ByteArrayPublisherInputStream(Publisher<byte[]> publisher, long requestSize) {
		Assert.notNull(publisher, "'publisher' must not be null");

		publisher.subscribe(new BlockingQueueSubscriber(requestSize));
	}


	@Override
	public int available() throws IOException {
		if (completed) {
			return 0;
		}
		InputStream is = currentStream();
		return is != null ? is.available() : 0;
	}

	@Override
	public int read() throws IOException {
		if (completed) {
			return -1;
		}
		InputStream is = currentStream();
		while (is != null) {
			int ch = is.read();
			if (ch != -1) {
				return ch;
			}
			else {
				is = currentStream();
			}
		}
		return -1;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (completed) {
			return -1;
		}
		InputStream is = currentStream();
		if (is == null) {
			return -1;
		}
		else if (b == null) {
			throw new NullPointerException();
		}
		else if (off < 0 || len < 0 || len > b.length - off) {
			throw new IndexOutOfBoundsException();
		}
		else if (len == 0) {
			return 0;
		}
		do {
			int n = is.read(b, off, len);
			if (n > 0) {
				return n;
			}
			else {
				is = currentStream();
			}
		}
		while (is != null);

		return -1;
	}

	private InputStream currentStream() throws IOException {
		try {
			if (this.currentStream != null && this.currentStream.available() > 0) {
				return this.currentStream;
			}
			else {
				// take() blocks, but that's OK since this is a *blocking* InputStream
				PublisherSignal<byte[]> signal = this.queue.take();

				if (signal.isData()) {
					byte[] data = signal.data();
					this.currentStream = new ByteArrayInputStream(data);
					return this.currentStream;
				}
				else if (signal.isComplete()) {
					this.completed = true;
					return null;
				}
				else if (signal.isError()) {
					Throwable error = signal.error();
					this.completed = true;
					if (error instanceof IOException) {
						throw (IOException) error;
					}
					else {
						throw new IOException(error);
					}
				}
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		throw new IOException();
	}

	private class BlockingQueueSubscriber implements Subscriber<byte[]> {

		private final long requestSize;

		private Subscription subscription;

		public BlockingQueueSubscriber(long requestSize) {
			this.requestSize = requestSize;
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			this.subscription = subscription;

			this.subscription.request(this.requestSize);
		}

		@Override
		public void onNext(byte[] bytes) {
			try {
				queue.put(PublisherSignal.data(bytes));
				this.subscription.request(requestSize);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}

		@Override
		public void onError(Throwable t) {
			try {
				queue.put(PublisherSignal.error(t));
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}

		@Override
		public void onComplete() {
			try {
				queue.put(PublisherSignal.complete());
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

}
