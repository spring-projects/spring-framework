package org.springframework.rx.io;/*
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

import java.io.IOException;
import java.io.InputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.reactivestreams.Publisher;

import org.springframework.rx.util.BlockingByteBufQueue;
import org.springframework.rx.util.BlockingByteBufQueueSubscriber;
import org.springframework.util.Assert;

/**
 * @author Arjen Poutsma
 */
public class ByteBufPublisherInputStream extends InputStream {

	private final BlockingByteBufQueue queue;

	private ByteBufInputStream currentStream;

	public ByteBufPublisherInputStream(Publisher<ByteBuf> publisher) {
		Assert.notNull(publisher, "'publisher' must not be null");

		this.queue = new BlockingByteBufQueue();
		publisher.subscribe(new BlockingByteBufQueueSubscriber(this.queue));
	}

	ByteBufPublisherInputStream(BlockingByteBufQueue queue) {
		Assert.notNull(queue, "'queue' must not be null");
		this.queue = queue;
	}

	@Override
	public int available() throws IOException {
		InputStream is = currentStream();
		return is != null ? is.available() : 0;
	}

	@Override
	public int read() throws IOException {
		InputStream is = currentStream();
		while (is != null) {
			int ch = is.read();
			if (ch != -1) {
				return ch;
			} else {
				is = currentStream();
			}
		}
		return -1;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
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
			else if (this.queue.isComplete()) {
				return null;
			}
			else if (this.queue.isHeadBuffer()) {
				ByteBuf current = this.queue.pollBuffer();
				this.currentStream = new ByteBufInputStream(current);
				return this.currentStream;
			}
			else if (this.queue.isHeadError()) {
				Throwable t = this.queue.pollError();
				throw toIOException(t);
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		return null;
	}

	private static IOException toIOException(Throwable t) {
		return t instanceof IOException ? (IOException) t : new IOException(t);
	}

}
