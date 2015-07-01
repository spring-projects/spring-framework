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

import org.springframework.util.Assert;

/**
 * A {@link BlockingQueue} aimed at working with {@code Publisher<ByteBuf>} instances.
 * Mainly meant to bridge between reactive and non-reactive APIs, such as blocking
 * streams.
 *
 * <p>Typically, this class will be used by two threads: one thread to put new elements on
 * the stack by calling {@link #putSignal(Object)}, possibly {@link #putError(Throwable)}
 * and finally {@link #complete()}. The other thread will read elements by calling {@link
 * #isHeadSignal()}/{@link #pollSignal()} and {@link #isHeadError()}/{@link #pollError()},
 * while keeping an eye on {@link #isComplete()}.
 * @author Arjen Poutsma
 */
public class BlockingSignalQueue<T> {

	private final BlockingQueue<Signal<T>> queue = new LinkedBlockingQueue<Signal<T>>();


	/**
	 * Inserts the specified signal into this queue, waiting if necessary for space to
	 * become available.
	 * @param t the signal to add
	 */
	public void putSignal(T t) throws InterruptedException {
		Assert.notNull(t, "'t' must not be null");
		Assert.state(!isComplete(), "Cannot put signal in queue after complete()");
		this.queue.put(new OnNext<T>(t));
	}

	/**
	 * Inserts the specified error into this queue, waiting if necessary for space to
	 * become available.
	 * @param error the error to add
	 */
	public void putError(Throwable error) throws InterruptedException {
		Assert.notNull(error, "'error' must not be null");
		Assert.state(!isComplete(), "Cannot putSignal errors in queue after complete()");
		this.queue.put(new OnError<T>(error));
	}

	/**
	 * Marks the queue as complete.
	 */
	@SuppressWarnings("unchecked")
	public void complete() throws InterruptedException {
		this.queue.put(OnComplete.INSTANCE);
	}

	/**
	 * Indicates whether the current head of this queue is a signal.
	 * @return {@code true} if the current head is a signal; {@code false} otherwise
	 */
	public boolean isHeadSignal() {
		Signal signal = this.queue.peek();
		return signal != null && signal.isOnNext();
	}

	/**
	 * Indicates whether the current head of this queue is a {@link Throwable}.
	 * @return {@code true} if the current head is an error; {@code false} otherwise
	 */
	public boolean isHeadError() {
		Signal signal = this.queue.peek();
		return signal != null && signal.isOnError();
	}

	/**
	 * Indicates whether there are more buffers or errors in this queue.
	 * @return {@code true} if there more elements in this queue; {@code false} otherwise
	 */
	public boolean isComplete() {
		Signal signal = this.queue.peek();
		return signal != null && signal.isComplete();
	}

	/**
	 * Retrieves and removes the signal head of this queue. Should only be called after
	 * {@link #isHeadSignal()} returns {@code true}.
	 * @return the head of the queue
	 * @throws IllegalStateException if the current head of this queue is not a buffer
	 * @see #isHeadSignal()
	 */
	public T pollSignal() throws InterruptedException {
		Signal<T> signal = this.queue.take();
		return signal != null ? signal.next() : null;
	}

	/**
	 * Retrieves and removes the buffer error of this queue. Should only be called after
	 * {@link #isHeadError()} returns {@code true}.
	 * @return the head of the queue, as error
	 * @throws IllegalStateException if the current head of this queue is not a error
	 * @see #isHeadError()
	 */
	public Throwable pollError() throws InterruptedException {
		Signal signal = this.queue.take();
		return signal != null ? signal.error() : null;
	}

	private interface Signal<T> {

		boolean isOnNext();

		T next();

		boolean isOnError();

		Throwable error();

		boolean isComplete();
	}

	private static class OnNext<T> implements Signal<T> {

		private final T next;

		public OnNext(T next) {
			Assert.notNull(next, "'next' must not be null");
			this.next = next;
		}

		@Override
		public boolean isOnNext() {
			return true;
		}

		@Override
		public T next() {
			return next;
		}

		@Override
		public boolean isOnError() {
			return false;
		}

		@Override
		public Throwable error() {
			throw new IllegalStateException();
		}

		@Override
		public boolean isComplete() {
			return false;
		}
	}

	private static final class OnError<T> implements Signal<T> {

		private final Throwable error;

		public OnError(Throwable error) {
			Assert.notNull(error, "'error' must not be null");
			this.error = error;
		}

		@Override
		public boolean isOnError() {
			return true;
		}

		@Override
		public Throwable error() {
			return error;
		}

		@Override
		public boolean isOnNext() {
			return false;
		}

		@Override
		public T next() {
			throw new IllegalStateException();
		}

		@Override
		public boolean isComplete() {
			return false;
		}
	}

	private static class OnComplete<T> implements Signal<T> {

		private static final OnComplete INSTANCE = new OnComplete();

		private OnComplete() {
		}

		@Override
		public boolean isComplete() {
			return true;
		}

		@Override
		public boolean isOnNext() {
			return false;
		}

		@Override
		public T next() {
			throw new IllegalStateException();
		}

		@Override
		public boolean isOnError() {
			return false;
		}

		@Override
		public Throwable error() {
			throw new IllegalStateException();
		}

	}

}
