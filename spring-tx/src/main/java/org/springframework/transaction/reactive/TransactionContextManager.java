/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.transaction.reactive;

import java.util.ArrayDeque;
import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import org.springframework.transaction.NoTransactionException;

/**
 * Delegate to register and obtain transactional contexts.
 *
 * <p>Typically used by components that intercept or orchestrate transactional flows
 * such as AOP interceptors or transactional operators.
 *
 * @author Mark Paluch
 * @since 5.2
 * @see TransactionSynchronization
 */
public abstract class TransactionContextManager {

	private TransactionContextManager() {
	}


	/**
	 * Obtain the current {@link TransactionContext} from the subscriber context or the
	 * transactional context holder. Context retrieval fails with NoTransactionException
	 * if no context or context holder is registered.
	 * @return the current {@link TransactionContext}
	 * @throws NoTransactionException if no TransactionContext was found in the subscriber context
	 * or no context found in a holder
	 */
	public static Mono<TransactionContext> currentContext() throws NoTransactionException {
		return Mono.deferContextual(ctx -> {
			if (ctx.hasKey(TransactionContext.class)) {
				return Mono.just(ctx.get(TransactionContext.class));
			}
			if (ctx.hasKey(TransactionContextHolder.class)) {
				TransactionContextHolder holder = ctx.get(TransactionContextHolder.class);
				if (holder.hasContext()) {
					return Mono.just(holder.currentContext());
				}
			}
			return Mono.error(new NoTransactionInContextException());
		});
	}

	/**
	 * Create a {@link TransactionContext} and register it in the subscriber {@link Context}.
	 * @return functional context registration.
	 * @throws IllegalStateException if a transaction context is already associated.
	 * @see Mono#contextWrite(Function)
	 * @see Flux#contextWrite(Function)
	 */
	public static Function<Context, Context> createTransactionContext() {
		return context -> context.put(TransactionContext.class, new TransactionContext());
	}

	/**
	 * Return a {@link Function} to create or associate a new {@link TransactionContext}.
	 * Interaction with transactional resources through
	 * {@link TransactionSynchronizationManager} requires a TransactionContext
	 * to be registered in the subscriber context.
	 * @return functional context registration.
	 */
	public static Function<Context, Context> getOrCreateContext() {
		return context -> {
			TransactionContextHolder holder = context.get(TransactionContextHolder.class);
			if (holder.hasContext()) {
				return context.put(TransactionContext.class, holder.currentContext());
			}
			return context.put(TransactionContext.class, holder.createContext());
		};
	}

	/**
	 * Return a {@link Function} to create or associate a new
	 * {@link TransactionContextHolder}. Creation and release of transactions
	 * within a reactive flow requires a mutable holder that follows a top to
	 * down execution scheme. Reactor's subscriber context follows a down to top
	 * approach regarding mutation visibility.
	 * @return functional context registration.
	 */
	public static Function<Context, Context> getOrCreateContextHolder() {
		return context -> {
			if (!context.hasKey(TransactionContextHolder.class)) {
				return context.put(TransactionContextHolder.class, new TransactionContextHolder(new ArrayDeque<>()));
			}
			return context;
		};
	}


	/**
	 * Stackless variant of {@link NoTransactionException} for reactive flows.
	 */
	@SuppressWarnings("serial")
	private static class NoTransactionInContextException extends NoTransactionException {

		public NoTransactionInContextException() {
			super("No transaction in context");
		}

		@Override
		public synchronized Throwable fillInStackTrace() {
			// stackless exception
			return this;
		}
	}

}
