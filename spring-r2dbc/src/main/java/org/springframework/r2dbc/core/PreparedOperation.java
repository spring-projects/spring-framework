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

package org.springframework.r2dbc.core;

import java.util.function.Supplier;

import org.springframework.r2dbc.core.binding.BindTarget;

/**
 * Extension to {@link QueryOperation} for a prepared SQL query
 * {@link Supplier} with bound parameters. Contains parameter
 * bindings that can be {@link #bindTo bound} to a {@link BindTarget}.
 *
 * <p>Can be executed with {@link org.springframework.r2dbc.core.DatabaseClient}.
 *
 * @author Mark Paluch
 * @since 5.3
 * @param <T> underlying operation source
 * @see org.springframework.r2dbc.core.DatabaseClient#sql(Supplier)
 */
public interface PreparedOperation<T> extends QueryOperation {

	/**
	 * Get the underlying query source.
	 * @return the query source, such as a statement/criteria object
	 */
	T getSource();

	/**
	 * Apply bindings to the supplied {@link BindTarget}.
	 * @param target the target to apply bindings to
	 */
	void bindTo(BindTarget target);

}
