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

package org.springframework.util.function;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A {@link Supplier} that allows invocation of code that throws a checked exception.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 * @param <T> the type of results supplied by this supplier
 */
public interface ThrowingSupplier<T> extends Supplier<T> {

	/**
	 * Gets a result, possibly throwing a checked exception.
	 * @return a result
	 * @throws Exception on error
	 */
	T getWithException() throws Exception;

	/**
	 * Default {@link Supplier#get()} that wraps any thrown checked exceptions
	 * (by default in a {@link RuntimeException}).
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	default T get() {
		return get(RuntimeException::new);
	}

	/**
	 * Gets a result, wrapping any thrown checked exceptions using the given
	 * {@code exceptionWrapper}.
	 * @param exceptionWrapper {@link BiFunction} that wraps the given message
	 * and checked exception into a runtime exception
	 * @return a result
	 */
	default T get(BiFunction<String, Exception, RuntimeException> exceptionWrapper) {
		try {
			return getWithException();
		}
		catch (RuntimeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw exceptionWrapper.apply(ex.getMessage(), ex);
		}
	}

	/**
	 * Return a new {@link ThrowingSupplier} where the {@link #get()} method
	 * wraps any thrown checked exceptions using the given
	 * {@code exceptionWrapper}.
	 * @param exceptionWrapper {@link BiFunction} that wraps the given message
	 * and checked exception into a runtime exception
	 * @return the replacement {@link ThrowingSupplier} instance
	 */
	default ThrowingSupplier<T> throwing(BiFunction<String, Exception, RuntimeException> exceptionWrapper) {
		return new ThrowingSupplier<>() {
			@Override
			public T getWithException() throws Exception {
				return ThrowingSupplier.this.getWithException();
			}
			@Override
			public T get() {
				return get(exceptionWrapper);
			}
		};
	}

	/**
	 * Lambda friendly convenience method that can be used to create a
	 * {@link ThrowingSupplier} where the {@link #get()} method wraps any checked
	 * exception thrown by the supplied lambda expression or method reference.
	 * <p>This method can be especially useful when working with method references.
	 * It allows you to easily convert a method that throws a checked exception
	 * into an instance compatible with a regular {@link Supplier}.
	 * <p>For example:
	 * <pre class="code">
	 * optional.orElseGet(ThrowingSupplier.of(Example::methodThatCanThrowCheckedException));
	 * </pre>
	 * @param <T> the type of results supplied by this supplier
	 * @param supplier the source supplier
	 * @return a new {@link ThrowingSupplier} instance
	 */
	static <T> ThrowingSupplier<T> of(ThrowingSupplier<T> supplier) {
		return supplier;
	}

	/**
	 * Lambda friendly convenience method that can be used to create
	 * {@link ThrowingSupplier} where the {@link #get()} method wraps any
	 * thrown checked exceptions using the given {@code exceptionWrapper}.
	 * <p>This method can be especially useful when working with method references.
	 * It allows you to easily convert a method that throws a checked exception
	 * into an instance compatible with a regular {@link Supplier}.
	 * <p>For example:
	 * <pre class="code">
	 * optional.orElseGet(ThrowingSupplier.of(Example::methodThatCanThrowCheckedException, IllegalStateException::new));
	 * </pre>
	 * @param <T> the type of results supplied by this supplier
	 * @param supplier the source supplier
	 * @param exceptionWrapper the exception wrapper to use
	 * @return a new {@link ThrowingSupplier} instance
	 */
	static <T> ThrowingSupplier<T> of(ThrowingSupplier<T> supplier,
			BiFunction<String, Exception, RuntimeException> exceptionWrapper) {

		return supplier.throwing(exceptionWrapper);
	}

}
