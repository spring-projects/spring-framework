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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.r2dbc.core.binding.BindMarkersFactory;


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
 */
class NamedParameterExpander {

	/**
	 * Default maximum number of entries for the SQL cache: 256.
	 */
	public static final int DEFAULT_CACHE_LIMIT = 256;


	private volatile int cacheLimit = DEFAULT_CACHE_LIMIT;

	private final Log logger = LogFactory.getLog(getClass());

	/**
	 * Cache of original SQL String to ParsedSql representation.
	 */
	@SuppressWarnings("serial")
	private final Map<String, ParsedSql> parsedSqlCache = new LinkedHashMap<String, ParsedSql>(
			DEFAULT_CACHE_LIMIT, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, ParsedSql> eldest) {
			return size() > getCacheLimit();
		}
	};


	/**
	 * Create a new enabled instance of {@link NamedParameterExpander}.
	 */
	public NamedParameterExpander() {}


	/**
	 * Specify the maximum number of entries for the SQL cache. Default is 256.
	 */
	public void setCacheLimit(int cacheLimit) {
		this.cacheLimit = cacheLimit;
	}

	/**
	 * Return the maximum number of entries for the SQL cache.
	 */
	public int getCacheLimit() {
		return this.cacheLimit;
	}

	/**
	 * Obtain a parsed representation of the given SQL statement.
	 * <p>
	 * The default implementation uses an LRU cache with an upper limit of 256 entries.
	 *
	 * @param sql the original SQL statement
	 * @return a representation of the parsed SQL statement
	 */
	private ParsedSql getParsedSql(String sql) {

		if (getCacheLimit() <= 0) {
			return NamedParameterUtils.parseSqlStatement(sql);
		}

		synchronized (this.parsedSqlCache) {

			ParsedSql parsedSql = this.parsedSqlCache.get(sql);
			if (parsedSql == null) {

				parsedSql = NamedParameterUtils.parseSqlStatement(sql);
				this.parsedSqlCache.put(sql, parsedSql);
			}
			return parsedSql;
		}
	}

	/**
	 * Parse the SQL statement and locate any placeholders or named parameters.
	 * Named parameters are substituted for a native placeholder, and any
	 * select list is expanded to the required number of placeholders. Select
	 * lists may contain an array of objects, and in that case the placeholders
	 * will be grouped and enclosed with parentheses. This allows for the use of
	 * "expression lists" in the SQL statement like:
	 *
	 * <pre class="code">
	 * select id, name, state from table where (name, age) in (('John', 35), ('Ann', 50))
	 * </pre>
	 *
	 * <p>The parameter values passed in are used to determine the number of
	 * placeholders to be used for a select list. Select lists should be limited
	 * to 100 or fewer elements. A larger number of elements is not guaranteed to be
	 * supported by the database and is strictly vendor-dependent.
	 * @param sql sql the original SQL statement
	 * @param bindMarkersFactory the bind marker factory
	 * @param paramSource the source for named parameters
	 * @return the expanded sql that accepts bind parameters and allows for execution
	 * without further translation wrapped as {@link PreparedOperation}.
	 */
	public PreparedOperation<String> expand(String sql, BindMarkersFactory bindMarkersFactory,
			BindParameterSource paramSource) {

		ParsedSql parsedSql = getParsedSql(sql);

		PreparedOperation<String> expanded = NamedParameterUtils.substituteNamedParameters(parsedSql, bindMarkersFactory,
				paramSource);

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Expanding SQL statement [%s] to [%s]", sql, expanded.toQuery()));
		}

		return expanded;
	}

	/**
	 * Parse the SQL statement and locate any placeholders or named parameters. Named parameters are returned as result of
	 * this method invocation.
	 *
	 * @return the parameter names.
	 */
	public List<String> getParameterNames(String sql) {
		return getParsedSql(sql).getParameterNames();
	}

}
