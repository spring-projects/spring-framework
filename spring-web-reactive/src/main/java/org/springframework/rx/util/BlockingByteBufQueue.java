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

package org.springframework.rx.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import io.netty.buffer.ByteBuf;

import org.springframework.util.Assert;

/**
 * A {@link BlockingQueue} aimed at working with {@code Publisher<ByteBuf>} instances.
 * Mainly meant to bridge between reactive and non-reactive APIs, such as blocking
 * streams.
 *
 * <p>Typically, this class will be used by two threads: one thread to put new elements on
 * the stack by calling {@link #putBuffer(ByteBuf)}, possibly {@link #putError(Throwable)}
 * and finally {@link #complete()}. The other thread will read elements by calling {@link
 * #isHeadBuffer()} and {@link #isHeadError()}, while keeping an eye on {@link
 * #isComplete()}.
 *
 * @author Arjen Poutsma
 */
public class BlockingByteBufQueue {

	private final BlockingQueue<Element> queue = new LinkedBlockingQueue<Element>();

	/**
	 * Inserts the specified buffer into this queue, waiting if necessary for space to
	 * become available.
	 * @param buffer the buffer to add
	 */
	public void putBuffer(ByteBuf buffer) throws InterruptedException {
		Assert.notNull(buffer, "'buffer' must not be null");
		Assert.state(!isComplete(), "Cannot put buffers in queue after complete()");
		this.queue.put(new ByteBufElement(buffer));
	}

	/**
	 * Inserts the specified error into this queue, waiting if necessary for space to
	 * become available.
	 * @param error the error to add
	 */
	public void putError(Throwable error) throws InterruptedException {
		Assert.notNull(error, "'error' must not be null");
		Assert.state(!isComplete(), "Cannot put errors in queue after complete()");
		this.queue.put(new ErrorElement(error));
	}

	/**
	 * Marks the queue as complete.
	 */
	public void complete() throws InterruptedException {
		this.queue.put(COMPLETE);
	}

	/**
	 * Indicates whether the current head of this queue is a {@link ByteBuf}.
	 * @return {@code true} if the current head is a buffer; {@code false} otherwise
	 */
	public boolean isHeadBuffer() {
		Element element = this.queue.peek();
		return element instanceof ByteBufElement;
	}

	/**
	 * Indicates whether the current head of this queue is a {@link Throwable}.
	 * @return {@code true} if the current head is an error; {@code false} otherwise
	 */
	public boolean isHeadError() {
		Element element = this.queue.peek();
		return element instanceof ErrorElement;
	}

	/**
	 * Indicates whether there are more buffers or errors in this queue.
	 * @return {@code true} if there more elements in this queue; {@code false} otherwise
	 */
	public boolean isComplete() {
		Element element = this.queue.peek();
		return COMPLETE == element;
	}

	/**
	 * Retrieves and removes the buffer head of this queue. Should only be called after
	 * {@link #isHeadBuffer()} returns {@code true}.
	 * @return the head of the queue, as buffer
	 * @throws IllegalStateException if the current head of this queue is not a buffer
	 * @see #isHeadBuffer()
	 */
	public ByteBuf pollBuffer() throws InterruptedException {
		Element element = this.queue.take();
		return element != null ? element.getBuffer() : null;
	}

	/**
	 * Retrieves and removes the buffer error of this queue. Should only be called after
	 * {@link #isHeadError()} returns {@code true}.
	 * @return the head of the queue, as error
	 * @throws IllegalStateException if the current head of this queue is not a error
	 * @see #isHeadError()
	 */
	public Throwable pollError() throws InterruptedException {
		Element element = this.queue.take();
		return element != null ? element.getError() : null;
	}

	/**
	 * Removes all of the elements from this collection
	 */
	public void clear() {
		this.queue.clear();
	}

	private interface Element {

		ByteBuf getBuffer();

		Throwable getError();
	}

	private static class ByteBufElement implements Element {

		private final ByteBuf buffer;

		public ByteBufElement(ByteBuf buffer) {
			if (buffer == null) {
				throw new IllegalArgumentException("'buffer' should not be null");
			}
			this.buffer = buffer;
		}

		@Override
		public ByteBuf getBuffer() {
			return this.buffer;
		}

		@Override
		public Throwable getError() {
			throw new IllegalStateException("No error on top of the queue");
		}

	}

	private static class ErrorElement implements Element {

		private final Throwable error;

		public ErrorElement(Throwable error) {
			if (error == null) {
				throw new IllegalArgumentException("'error' should not be null");
			}
			this.error = error;
		}

		@Override
		public ByteBuf getBuffer() {
			throw new IllegalStateException("No ByteBuf on top of the queue");
		}

		@Override
		public Throwable getError() {
			return this.error;
		}
	}

	private static final Element COMPLETE = new Element() {
		@Override
		public ByteBuf getBuffer() {
			throw new IllegalStateException("No ByteBuf on top of the queue");
		}

		@Override
		public Throwable getError() {
			throw new IllegalStateException("No error on top of the queue");
		}
	};
}
