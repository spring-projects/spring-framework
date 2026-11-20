/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

/**
 * Reusable query in which concrete subclasses must implement the abstract
 * mapRow(ResultSet, int) method to convert each row of the JDBC ResultSet
 * into an object.
 *
 * <p>Simplifies MappingSqlQueryWithParameters API by dropping parameters and
 * context. Most subclasses won't care about parameters. If you don't use
 * contextual information, subclass this instead of MappingSqlQueryWithParameters.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Jean-Pierre Pawlak
 * @param <T> the result type
 * @see MappingSqlQueryWithParameters
 */
public abstract class MappingSqlQuery<T extends @Nullable Object> extends MappingSqlQueryWithParameters<T> {

	/**
	 * Constructor that allows use as a JavaBean.
	 */
	public MappingSqlQuery() {
	}

	/**
	 * Convenient constructor with DataSource and SQL string.
	 * @param ds the DataSource to use to obtain connections
	 * @param sql the SQL to run
	 */
	public MappingSqlQuery(DataSource ds, String sql) {
		super(ds, sql);
	}


	/**
	 * This method is implemented to invoke the simpler mapRow
	 * template method, ignoring parameters.
	 * @see #mapRow(ResultSet, int)
	 */
	@Override
	protected final T mapRow(ResultSet rs, int rowNum, @Nullable Object @Nullable [] parameters, @Nullable Map<?, ?> context)
			throws SQLException {

		return mapRow(rs, rowNum);
	}

	/**
	 * Subclasses must implement this method to convert each row of the
	 * ResultSet into an object of the result type.
	 * <p>Subclasses of this class, as opposed to direct subclasses of
	 * MappingSqlQueryWithParameters, don't need to concern themselves
	 * with the parameters to the execute method of the query object.
	 * @param rs the ResultSet we're working through
	 * @param rowNum row number (from 0) we're up to
	 * @return an object of the result type
	 * @throws SQLException if there's an error extracting data.
	 * Subclasses can simply not catch SQLExceptions, relying on the
	 * framework to clean up.
	 */
	protected abstract T mapRow(ResultSet rs, int rowNum) throws SQLException;

}
