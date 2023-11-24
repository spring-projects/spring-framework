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

package org.springframework.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

import org.springframework.util.Assert;

/**
 * Convenience utilities for working with {@link java.util.concurrent.Future}
 * and implementations.
 *
 * @author Arjen Poutsma
 * @since 6.0
 */
public abstract class FutureUtils {

	/**
	 * Return a new {@code CompletableFuture} that is asynchronously completed
	 * by a task running in the {@link ForkJoinPool#commonPool()} with
	 * the value obtained by calling the given {@code Callable}.
	 * @param callable a function that returns the value to be used, or throws
	 * an exception
	 * @return the new CompletableFuture
	 * @see CompletableFuture#supplyAsync(Supplier)
	 */
	public static <T> CompletableFuture<T> callAsync(Callable<T> callable) {
		Assert.notNull(callable, "Callable must not be null");

		CompletableFuture<T> result = new CompletableFuture<>();
		return result.completeAsync(toSupplier(callable, result));
	}

	/**
	 * Return a new {@code CompletableFuture} that is asynchronously completed
	 * by a task running in the given executor with the value obtained
	 * by calling the given {@code Callable}.
	 * @param callable a function that returns the value to be used, or throws
	 * an exception
	 * @param executor the executor to use for asynchronous execution
	 * @return the new CompletableFuture
	 * @see CompletableFuture#supplyAsync(Supplier, Executor)
	 */
	public static <T> CompletableFuture<T> callAsync(Callable<T> callable, Executor executor) {
		Assert.notNull(callable, "Callable must not be null");
		Assert.notNull(executor, "Executor must not be null");

		CompletableFuture<T> result = new CompletableFuture<>();
		return result.completeAsync(toSupplier(callable, result), executor);
	}

	private static <T> Supplier<T> toSupplier(Callable<T> callable, CompletableFuture<T> result) {
		return () -> {
			try {
				return callable.call();
			}
			catch (Exception ex) {
				// wrap the exception just like CompletableFuture::supplyAsync does
				result.completeExceptionally((ex instanceof CompletionException) ? ex : new CompletionException(ex));
				return null;
			}
		};
	}

}
