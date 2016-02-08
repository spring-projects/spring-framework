/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.io.buffer.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.Assert;

/**
 * @author Arjen Poutsma
 */
class DataBufferPublisherInputStream extends InputStream {

	private final AtomicBoolean completed = new AtomicBoolean();

	private final Iterator<DataBuffer> queue;

	private InputStream currentStream;

	/**
	 * Creates a new {@code ByteArrayPublisherInputStream} based on the given publisher.
	 * @param publisher the publisher to use
	 */
	public DataBufferPublisherInputStream(Publisher<DataBuffer> publisher) {
		this(publisher, 1);
	}

	/**
	 * Creates a new {@code ByteArrayPublisherInputStream} based on the given publisher.
	 * @param publisher the publisher to use
	 * @param requestSize the {@linkplain Subscription#request(long) request size} to use
	 * on the publisher bound to Integer MAX
	 */
	public DataBufferPublisherInputStream(Publisher<DataBuffer> publisher,
			int requestSize) {
		Assert.notNull(publisher, "'publisher' must not be null");

		this.queue = Flux.from(publisher).toIterable(requestSize).iterator();
	}

	@Override
	public int available() throws IOException {
		if (completed.get()) {
			return 0;
		}
		InputStream is = currentStream();
		return is != null ? is.available() : 0;
	}

	@Override
	public int read() throws IOException {
		if (completed.get()) {
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
		if (completed.get()) {
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
				// if upstream Publisher has completed, then complete() and return null,
				if (!this.queue.hasNext()) {
					this.completed.set(true);
					return null;
				}
				// next() blocks until next
				// but that's OK since this is a *blocking* InputStream
				DataBuffer signal = this.queue.next();
				this.currentStream = signal.asInputStream();
				return this.currentStream;
			}
		}
		catch (Throwable error) {
			this.completed.set(true);
			throw new IOException(error);
		}
	}


}
