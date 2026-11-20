/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.object;

import org.jspecify.annotations.Nullable;

import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;
import org.springframework.util.Assert;

/**
 * Operation object representing an SQL-based operation such as a query or update,
 * as opposed to a stored procedure.
 *
 * <p>Configures a {@link org.springframework.jdbc.core.PreparedStatementCreatorFactory}
 * based on the declared parameters.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public abstract class SqlOperation extends RdbmsOperation {

	/**
	 * Object enabling us to create PreparedStatementCreators efficiently,
	 * based on this class's declared parameters.
	 */
	private @Nullable PreparedStatementCreatorFactory preparedStatementFactory;

	/** Parsed representation of the SQL statement. */
	private @Nullable ParsedSql cachedSql;

	/** Monitor for locking the cached representation of the parsed SQL statement. */
	private final Object parsedSqlMonitor = new Object();


	/**
	 * Overridden method to configure the PreparedStatementCreatorFactory
	 * based on our declared parameters.
	 */
	@Override
	protected final void compileInternal() {
		this.preparedStatementFactory = new PreparedStatementCreatorFactory(resolveSql(), getDeclaredParameters());
		this.preparedStatementFactory.setResultSetType(getResultSetType());
		this.preparedStatementFactory.setUpdatableResults(isUpdatableResults());
		this.preparedStatementFactory.setReturnGeneratedKeys(isReturnGeneratedKeys());
		if (getGeneratedKeysColumnNames() != null) {
			this.preparedStatementFactory.setGeneratedKeysColumnNames(getGeneratedKeysColumnNames());
		}

		onCompileInternal();
	}

	/**
	 * Hook method that subclasses may override to post-process compilation.
	 * This implementation does nothing.
	 * @see #compileInternal
	 */
	protected void onCompileInternal() {
	}

	/**
	 * Obtain a parsed representation of this operation's SQL statement.
	 * <p>Typically used for named parameter parsing.
	 */
	protected ParsedSql getParsedSql() {
		synchronized (this.parsedSqlMonitor) {
			if (this.cachedSql == null) {
				this.cachedSql = NamedParameterUtils.parseSqlStatement(resolveSql());
			}
			return this.cachedSql;
		}
	}


	/**
	 * Return a PreparedStatementSetter to perform an operation
	 * with the given parameters.
	 * @param params the parameter array (may be {@code null})
	 */
	protected final PreparedStatementSetter newPreparedStatementSetter(@Nullable Object @Nullable [] params) {
		Assert.state(this.preparedStatementFactory != null, "No PreparedStatementFactory available");
		return this.preparedStatementFactory.newPreparedStatementSetter(params);
	}

	/**
	 * Return a PreparedStatementCreator to perform an operation
	 * with the given parameters.
	 * @param params the parameter array (may be {@code null})
	 */
	protected final PreparedStatementCreator newPreparedStatementCreator(@Nullable Object @Nullable [] params) {
		Assert.state(this.preparedStatementFactory != null, "No PreparedStatementFactory available");
		return this.preparedStatementFactory.newPreparedStatementCreator(params);
	}

	/**
	 * Return a PreparedStatementCreator to perform an operation
	 * with the given parameters.
	 * @param sqlToUse the actual SQL statement to use (if different from
	 * the factory's, for example because of named parameter expanding)
	 * @param params the parameter array (may be {@code null})
	 */
	protected final PreparedStatementCreator newPreparedStatementCreator(String sqlToUse, @Nullable Object @Nullable [] params) {
		Assert.state(this.preparedStatementFactory != null, "No PreparedStatementFactory available");
		return this.preparedStatementFactory.newPreparedStatementCreator(sqlToUse, params);
	}

}
