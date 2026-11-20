/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core;

import java.util.List;

import org.springframework.r2dbc.core.binding.BindMarkersFactory;
import org.springframework.util.ConcurrentLruCache;

/**
 * SQL translation support allowing the use of named parameters
 * rather than native placeholders.
 *
 * <p>This class expands SQL from named parameters to native
 * style placeholders at execution time. It also allows for expanding
 * a {@link List} of values to the appropriate number of placeholders.
 *
 * <p>References to the same parameter name are substituted with the
 * same bind marker placeholder if a {@link BindMarkersFactory} uses
 * {@link BindMarkersFactory#identifiablePlaceholders() identifiable} placeholders.
 * <p><b>NOTE: An instance of this class is thread-safe once configured.</b>
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 */
class NamedParameterExpander {

	/**
	 * Default maximum number of entries for the SQL cache: 256.
	 */
	public static final int DEFAULT_CACHE_LIMIT = 256;

	/** Cache of original SQL String to ParsedSql representation. */
	private final ConcurrentLruCache<String, ParsedSql> parsedSqlCache =
			new ConcurrentLruCache<>(DEFAULT_CACHE_LIMIT, NamedParameterUtils::parseSqlStatement);


	/**
	 * Obtain a parsed representation of the given SQL statement.
	 * <p>The default implementation uses an LRU cache with an upper limit of 256 entries.
	 * @param sql the original SQL statement
	 * @return a representation of the parsed SQL statement
	 */
	private ParsedSql getParsedSql(String sql) {
		return this.parsedSqlCache.get(sql);
	}

	/**
	 * Parse the SQL statement and locate any placeholders or named parameters.
	 * Named parameters are substituted for a native placeholder, and any
	 * select list is expanded to the required number of placeholders. Select
	 * lists may contain an array of objects, and in that case the placeholders
	 * will be grouped and enclosed with parentheses. This allows for the use of
	 * "expression lists" in the SQL statement like:
	 * <pre class="code">
	 * select id, name, state from table where (name, age) in (('John', 35), ('Ann', 50))
	 * </pre>
	 * <p>The parameter values passed in are used to determine the number of
	 * placeholders to be used for a select list. Select lists should be limited
	 * to 100 or fewer elements. A larger number of elements is not guaranteed to be
	 * supported by the database and is strictly vendor-dependent.
	 * @param sql the original SQL statement
	 * @param bindMarkersFactory the bind marker factory
	 * @param paramSource the source for named parameters
	 * @return the expanded sql that accepts bind parameters and allows for execution
	 * without further translation wrapped as {@link PreparedOperation}.
	 */
	public PreparedOperation<String> expand(
			String sql, BindMarkersFactory bindMarkersFactory, BindParameterSource paramSource) {

		ParsedSql parsedSql = getParsedSql(sql);
		return NamedParameterUtils.substituteNamedParameters(parsedSql, bindMarkersFactory, paramSource);
	}

	/**
	 * Parse the SQL statement and locate any placeholders or named parameters.
	 * Named parameters are returned as result of this method invocation.
	 * @return the parameter names
	 */
	public List<String> getParameterNames(String sql) {
		return getParsedSql(sql).getParameterNames();
	}

}
