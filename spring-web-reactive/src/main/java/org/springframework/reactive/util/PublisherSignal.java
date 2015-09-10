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

package org.springframework.reactive.util;

import org.springframework.util.Assert;

/**
 * Represents a signal value object, useful for wrapping signals as published by a {@link
 * #Publisher()}. Mostly used to store signals in buffers.
 * @author Arjen Poutsma
 */
public abstract class PublisherSignal<T> {

	protected PublisherSignal() {
	}

	/**
	 * Indicates whether this signal is an data signal, i.e. if {@link #data()} can be
	 * called safely.
	 * @return {@code true} if this signal contains data; {@code false} otherwise
	 */
	public boolean isData() {
		return false;
	}

	/**
	 * Returns the data contained in this signal. Can only be safely called after {@link
	 * #isData()} returns {@code true}.
	 * @return the data
	 * @throws IllegalStateException if this signal does not contain data
	 */
	public T data() {
		throw new IllegalStateException();
	}

	/**
	 * Indicates whether this signal is an error signal, i.e. if {@link #error()} can be
	 * called safely.
	 * @return {@code true} if this signal contains an error; {@code false} otherwise
	 */
	public boolean isError() {
		return false;
	}

	/**
	 * Returns the error contained in this signal. Can only be safely called after {@link
	 * #isError()} returns {@code true}.
	 * @return the error
	 * @throws IllegalStateException if this signal does not contain an error
	 */
	public Throwable error() {
		throw new IllegalStateException();
	}

	/**
	 * Indicates whether this signal completes the stream.
	 * @return {@code true} if this signal completes the stream; {@code false} otherwise
	 */
	public boolean isComplete() {
		return false;
	}

	/**
	 * Creates a new data signal with the given {@code t}.
	 * @param t the data to base the signal on
	 * @return the newly created signal
	 */
	public static <T> PublisherSignal<T> data(T t) {
		Assert.notNull(t, "'t' must not be null");
		return new DataSignal<>(t);
	}

	/**
	 * Creates a new error signal with the given {@code Throwable}.
	 * @param t the exception to base the signal on
	 * @return the newly created signal
	 */
	public static <T> PublisherSignal<T> error(Throwable t) {
		Assert.notNull(t, "'t' must not be null");
		return new ErrorSignal<>(t);
	}

	/**
	 * Returns the complete signal, typically the last signal in a stream.
	 */
	@SuppressWarnings("unchecked")
	public static <T> PublisherSignal<T> complete() {
		return (PublisherSignal<T>)ON_COMPLETE;
	}

	private static final class DataSignal<T> extends PublisherSignal<T> {

		private final T data;

		public DataSignal(T data) {
			this.data = data;
		}

		@Override
		public boolean isData() {
			return true;
		}

		@Override
		public T data() {
			return data;
		}
	}

	private static final class ErrorSignal<T> extends PublisherSignal<T> {

		private final Throwable error;

		public ErrorSignal(Throwable error) {
			this.error = error;
		}

		@Override
		public boolean isError() {
			return true;
		}

		@Override
		public Throwable error() {
			return error;
		}

	}

	@SuppressWarnings("rawtypes")
	private static final PublisherSignal ON_COMPLETE = new PublisherSignal() {

		@Override
		public boolean isComplete() {
			return true;
		}

	};

}
