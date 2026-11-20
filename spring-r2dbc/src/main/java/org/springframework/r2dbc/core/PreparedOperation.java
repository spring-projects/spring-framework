/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
