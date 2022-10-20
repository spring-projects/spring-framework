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

import java.util.function.Supplier;

/**
 * Interface declaring a query operation that can be represented
 * with a query string. This interface is typically implemented
 * by classes representing an SQL operation such as {@code SELECT},
 * {@code INSERT}, and such.
 *
 * @author Mark Paluch
 * @since 5.3
 * @see PreparedOperation
 */
@FunctionalInterface
public interface QueryOperation extends Supplier<String> {

	/**
	 * Return the string-representation of this operation to
	 * be used with {@link io.r2dbc.spi.Statement} creation.
	 * @return the operation as SQL string
	 * @see io.r2dbc.spi.Connection#createStatement(String)
	 */
	String toQuery();

	@Override
	default String get() {
		return toQuery();
	}

}
