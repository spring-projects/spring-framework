/*
 * Copyright 2002-present the original author or authors.
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
 * A {@link Runnable} that allows invocation of code that throws a checked exception.
 *
 * @author Hosam Aly
 */
public interface ThrowingRunnable extends Runnable {

    /**
     * Gets a result, possibly throwing a checked exception.
     * @throws Exception on error
     */
    void runWithException() throws Exception;

    /**
     * Default {@link Runnable#run()} that wraps any thrown checked exceptions
     * (by default in a {@link RuntimeException}).
     * @see java.lang.Runnable#run()
     */
    @Override
    default void run() {
        run(RuntimeException::new);
    }

    /**
     * Gets a result, wrapping any thrown checked exceptions using the given
     * {@code exceptionWrapper}.
     * @param exceptionWrapper {@link BiFunction} that wraps the given message
     * and checked exception into a runtime exception
     */
    default void run(BiFunction<String, Exception, RuntimeException> exceptionWrapper) {
        try {
            runWithException();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exceptionWrapper.apply(ex.getMessage(), ex);
        }
    }

    /**
     * Return a new {@link ThrowingRunnable} where the {@link #run()} method
     * wraps any thrown checked exceptions using the given
     * {@code exceptionWrapper}.
     * @param exceptionWrapper {@link BiFunction} that wraps the given message
     * and checked exception into a runtime exception
     * @return the replacement {@link ThrowingRunnable} instance
     */
    default ThrowingRunnable throwing(BiFunction<String, Exception, RuntimeException> exceptionWrapper) {
        return new ThrowingRunnable() {
            @Override
            public void runWithException() throws Exception {
                ThrowingRunnable.this.runWithException();
            }

            @Override
            public void run() {
                run(exceptionWrapper);
            }
        };
    }

    /**
     * Lambda friendly convenience method that can be used to create a
     * {@link ThrowingRunnable} where the {@link #run()} method wraps any checked
     * exception thrown by the supplied lambda expression or method reference.
     * <p>This method can be especially useful when working with method references.
     * It allows you to easily convert a method that throws a checked exception
     * into an instance compatible with a regular {@link Runnable}.
     * <p>For example:
     * <pre class="code">
     * optional.orElseGet(ThrowingRunnable.of(Example::methodThatCanThrowCheckedException));
     * </pre>
     * @param runnable the source runnable
     * @return a new {@link ThrowingRunnable} instance
     */
    static ThrowingRunnable of(ThrowingRunnable runnable) {
        return runnable;
    }

    /**
     * Lambda friendly convenience method that can be used to create
     * {@link ThrowingRunnable} where the {@link #run()} method wraps any
     * thrown checked exceptions using the given {@code exceptionWrapper}.
     * <p>This method can be especially useful when working with method references.
     * It allows you to easily convert a method that throws a checked exception
     * into an instance compatible with a regular {@link Runnable}.
     * <p>For example:
     * <pre class="code">
     * optional.orElseGet(ThrowingRunnable.of(Example::methodThatCanThrowCheckedException, IllegalStateException::new));
     * </pre>
     * @param runnable the source runnable
     * @param exceptionWrapper the exception wrapper to use
     * @return a new {@link ThrowingRunnable} instance
     */
    static ThrowingRunnable of(
            ThrowingRunnable runnable, BiFunction<String, Exception, RuntimeException> exceptionWrapper) {

        return runnable.throwing(exceptionWrapper);
    }
}