/*
 * Copyright 2002-2022 the original author or authors.
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

/**
 * A {@link BiFunction} that allows invocation of code that throws a checked
 * exception.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * @param <R> the type of the result of the function
 */
public interface ThrowingBiFunction<T, U, R> extends BiFunction<T, U, R> {

	/**
	 * Applies this function to the given argument, possibly throwing a checked
	 * exception.
	 * @param t the first function argument
	 * @param u the second function argument
	 * @return the function result
	 * @throws Exception on error
	 */
	R applyWithException(T t, U u) throws Exception;

	/**
	 * Default {@link BiFunction#apply(Object, Object)} that wraps any thrown
	 * checked exceptions (by default in a {@link RuntimeException}).
	 * @param t the first function argument
	 * @param u the second function argument
	 * @return the function result
	 * @see java.util.function.BiFunction#apply(Object, Object)
	 */
	@Override
	default R apply(T t, U u) {
		return apply(t, u, RuntimeException::new);
	}

	/**
	 * Applies this function to the given argument, wrapping any thrown checked
	 * exceptions using the given {@code exceptionWrapper}.
	 * @param t the first function argument
	 * @param u the second function argument
	 * @param exceptionWrapper {@link BiFunction} that wraps the given message
	 * and checked exception into a runtime exception
	 * @return a result
	 */
	default R apply(T t, U u, BiFunction<String, Exception, RuntimeException> exceptionWrapper) {
		try {
			return applyWithException(t, u);
		}
		catch (RuntimeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw exceptionWrapper.apply(ex.getMessage(), ex);
		}
	}

	/**
	 * Return a new {@link ThrowingBiFunction} where the
	 * {@link #apply(Object, Object)} method wraps any thrown checked exceptions
	 * using the given {@code exceptionWrapper}.
	 * @param exceptionWrapper {@link BiFunction} that wraps the given message
	 * and checked exception into a runtime exception
	 * @return the replacement {@link ThrowingBiFunction} instance
	 */
	default ThrowingBiFunction<T, U, R> throwing(BiFunction<String, Exception, RuntimeException> exceptionWrapper) {
		return new ThrowingBiFunction<>() {

			@Override
			public R applyWithException(T t, U u) throws Exception {
				return ThrowingBiFunction.this.applyWithException(t, u);
			}

			@Override
			public R apply(T t, U u) {
				return apply(t, u, exceptionWrapper);
			}

		};
	}

	/**
	 * Lambda friendly convenience method that can be used to create a
	 * {@link ThrowingBiFunction} where the {@link #apply(Object, Object)}
	 * method wraps any checked exception thrown by the supplied lambda expression
	 * or method reference.
	 * <p>This method can be especially useful when working with method references.
	 * It allows you to easily convert a method that throws a checked exception
	 * into an instance compatible with a regular {@link BiFunction}.
	 * <p>For example:
	 * <pre class="code">
	 * map.replaceAll(ThrowingBiFunction.of(Example::methodThatCanThrowCheckedException));
	 * </pre>
	 * @param <T> the type of the first argument to the function
	 * @param <U> the type of the second argument to the function
	 * @param <R> the type of the result of the function
	 * @param function the source function
	 * @return a new {@link ThrowingFunction} instance
	 */
	static <T, U, R> ThrowingBiFunction<T, U, R> of(ThrowingBiFunction<T, U, R> function) {
		return function;
	}

	/**
	 * Lambda friendly convenience method that can be used to create a
	 * {@link ThrowingBiFunction} where the {@link #apply(Object, Object)}
	 * method wraps any thrown checked exceptions using the given
	 * {@code exceptionWrapper}.
	 * <p>This method can be especially useful when working with method references.
	 * It allows you to easily convert a method that throws a checked exception
	 * into an instance compatible with a regular {@link BiFunction}.
	 * <p>For example:
	 * <pre class="code">
	 * map.replaceAll(ThrowingBiFunction.of(Example::methodThatCanThrowCheckedException, IllegalStateException::new));
	 * </pre>
	 * @param <T> the type of the first argument to the function
	 * @param <U> the type of the second argument to the function
	 * @param <R> the type of the result of the function
	 * @param function the source function
	 * @param exceptionWrapper the exception wrapper to use
	 * @return a new {@link ThrowingFunction} instance
	 */
	static <T, U, R> ThrowingBiFunction<T, U, R> of(ThrowingBiFunction<T, U, R> function,
			BiFunction<String, Exception, RuntimeException> exceptionWrapper) {

		return function.throwing(exceptionWrapper);
	}

}
