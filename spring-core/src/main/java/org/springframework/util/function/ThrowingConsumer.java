/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.function;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * A {@link Consumer} that allows invocation of code that throws a checked
 * exception.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 * @param <T> the type of the input to the operation
 */
@FunctionalInterface
public interface ThrowingConsumer<T> extends Consumer<T> {

	/**
	 * Performs this operation on the given argument, possibly throwing a
	 * checked exception.
	 * @param t the input argument
	 * @throws Exception on error
	 */
	void acceptWithException(T t) throws Exception;

	/**
	 * Default {@link Consumer#accept(Object)} that wraps any thrown checked
	 * exceptions (by default in a {@link RuntimeException}).
	 * @see java.util.function.Consumer#accept(Object)
	 */
	@Override
	default void accept(T t) {
		accept(t, RuntimeException::new);
	}

	/**
	 * Performs this operation on the given argument, wrapping any thrown
	 * checked exceptions using the given {@code exceptionWrapper}.
	 * @param exceptionWrapper {@link BiFunction} that wraps the given message
	 * and checked exception into a runtime exception
	 */
	default void accept(T t, BiFunction<String, Exception, RuntimeException> exceptionWrapper) {
		try {
			acceptWithException(t);
		}
		catch (RuntimeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw exceptionWrapper.apply(ex.getMessage(), ex);
		}
	}

	/**
	 * Return a new {@link ThrowingConsumer} where the {@link #accept(Object)}
	 * method wraps any thrown checked exceptions using the given
	 * {@code exceptionWrapper}.
	 * @param exceptionWrapper {@link BiFunction} that wraps the given message
	 * and checked exception into a runtime exception
	 * @return the replacement {@link ThrowingConsumer} instance
	 */
	default ThrowingConsumer<T> throwing(BiFunction<String, Exception, RuntimeException> exceptionWrapper) {
		return new ThrowingConsumer<>() {
			@Override
			public void acceptWithException(T t) throws Exception {
				ThrowingConsumer.this.acceptWithException(t);
			}
			@Override
			public void accept(T t) {
				accept(t, exceptionWrapper);
			}
		};
	}

	/**
	 * Lambda friendly convenience method that can be used to create a
	 * {@link ThrowingConsumer} where the {@link #accept(Object)} method wraps
	 * any checked exception thrown by the supplied lambda expression or method
	 * reference.
	 * <p>This method can be especially useful when working with method references.
	 * It allows you to easily convert a method that throws a checked exception
	 * into an instance compatible with a regular {@link Consumer}.
	 * <p>For example:
	 * <pre class="code">
	 * list.forEach(ThrowingConsumer.of(Example::methodThatCanThrowCheckedException));
	 * </pre>
	 * @param <T> the type of the input to the operation
	 * @param consumer the source consumer
	 * @return a new {@link ThrowingConsumer} instance
	 */
	static <T> ThrowingConsumer<T> of(ThrowingConsumer<T> consumer) {
		return consumer;
	}

	/**
	 * Lambda friendly convenience method that can be used to create a
	 * {@link ThrowingConsumer} where the {@link #accept(Object)} method wraps
	 * any thrown checked exceptions using the given {@code exceptionWrapper}.
	 * <p>This method can be especially useful when working with method references.
	 * It allows you to easily convert a method that throws a checked exception
	 * into an instance compatible with a regular {@link Consumer}.
	 * <p>For example:
	 * <pre class="code">
	 * list.forEach(ThrowingConsumer.of(Example::methodThatCanThrowCheckedException, IllegalStateException::new));
	 * </pre>
	 * @param <T> the type of the input to the operation
	 * @param consumer the source consumer
	 * @param exceptionWrapper the exception wrapper to use
	 * @return a new {@link ThrowingConsumer} instance
	 */
	static <T> ThrowingConsumer<T> of(ThrowingConsumer<T> consumer,
			BiFunction<String, Exception, RuntimeException> exceptionWrapper) {

		return consumer.throwing(exceptionWrapper);
	}

}
