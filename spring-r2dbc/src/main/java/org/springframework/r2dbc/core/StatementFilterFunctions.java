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

import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.reactivestreams.Publisher;

/**
 * Collection of default {@link StatementFilterFunction}s.
 *
 * @author Mark Paluch
 * @since 5.3
 */
enum StatementFilterFunctions implements StatementFilterFunction {

	EMPTY_FILTER;


	@Override
	public Publisher<? extends Result> filter(Statement statement, ExecuteFunction next) {
		return next.execute(statement);
	}

	/**
	 * Return an empty {@link StatementFilterFunction} that delegates to {@link ExecuteFunction}.
	 * @return an empty {@link StatementFilterFunction} that delegates to {@link ExecuteFunction}.
	 */
	public static StatementFilterFunction empty() {
		return EMPTY_FILTER;
	}

}
