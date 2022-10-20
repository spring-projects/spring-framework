/*
 * Copyright 2002-2020 the original author or authors.
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
