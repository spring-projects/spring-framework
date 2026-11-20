/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core;

import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.reactivestreams.Publisher;

import org.springframework.util.Assert;

/**
 * Represents a function that filters an {@link ExecuteFunction}.
 *
 * <p>The filter is executed when a {@link org.reactivestreams.Subscriber} subscribes
 * to the {@link Publisher} returned by the {@link DatabaseClient}.
 *
 * <p>StatementFilterFunctions are typically used to specify additional details on
 * the Statement objects such as {@code fetchSize} or key generation.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.3
 * @see ExecuteFunction
 */
@FunctionalInterface
public interface StatementFilterFunction {

	/**
	 * An empty {@link StatementFilterFunction} that delegates to {@link ExecuteFunction}.
	 */
	StatementFilterFunction EMPTY_FILTER = (statement, next) -> next.execute(statement);


	/**
	 * Apply this filter to the given {@link Statement} and {@link ExecuteFunction}.
	 * <p>The given {@code ExecuteFunction} represents the next entity in the chain,
	 * to be invoked via {@link ExecuteFunction#execute(Statement)} in order to
	 * proceed with the execution, or not invoked to short-circuit the chain.
	 * @param statement the current {@link Statement}
	 * @param next the next {@code ExecuteFunction} in the chain
	 * @return the filtered {@link Result}s.
	 */
	Publisher<? extends Result> filter(Statement statement, ExecuteFunction next);

	/**
	 * Return a composed filter function that first applies this filter and then
	 * applies the given {@code afterFilter}.
	 * @param afterFilter the filter to apply after this filter
	 * @return the composed filter.
	 */
	default StatementFilterFunction andThen(StatementFilterFunction afterFilter) {
		Assert.notNull(afterFilter, "StatementFilterFunction must not be null");
		return (request, next) -> filter(request, afterRequest -> afterFilter.filter(afterRequest, next));
	}

}
