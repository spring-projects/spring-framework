/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core;

import java.util.function.BiFunction;

import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.reactivestreams.Publisher;

/**
 * Represents a function that executes a {@link Statement} for a (delayed)
 * {@link Result} stream.
 *
 * <p>Note that discarded {@link Result} objects must be consumed according
 * to the R2DBC spec via either {@link Result#getRowsUpdated()} or
 * {@link Result#map(BiFunction)}.
 *
 * <p>Typically, implementations invoke the {@link Statement#execute()} method
 * to initiate execution of the statement object.
 *
 * For example:
 * <p><pre class="code">
 * DatabaseClient.builder()
 *		.executeFunction(statement -&gt; statement.execute())
 * 		.build();
 * </pre>
 *
 * @author Mark Paluch
 * @since 5.3
 * @see Statement#execute()
 */
@FunctionalInterface
public interface ExecuteFunction {

	/**
	 * Execute the given {@link Statement} for a stream of {@link Result} objects.
	 * @param statement the request to execute
	 * @return the delayed result stream
	 */
	Publisher<? extends Result> execute(Statement statement);

}
