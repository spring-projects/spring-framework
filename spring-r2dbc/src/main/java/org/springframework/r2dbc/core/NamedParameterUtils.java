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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.binding.BindMarker;
import org.springframework.r2dbc.core.binding.BindMarkers;
import org.springframework.r2dbc.core.binding.BindMarkersFactory;
import org.springframework.r2dbc.core.binding.BindTarget;
import org.springframework.util.Assert;

/**
 * Helper methods for named parameter parsing.
 *
 * <p>Only intended for internal use within Spring's R2DBC
 * framework.
 *
 * <p>References to the same parameter name are substituted with
 * the same bind marker placeholder if a {@link BindMarkersFactory} uses
 * {@link BindMarkersFactory#identifiablePlaceholders() identifiable}
 * placeholders.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Mark Paluch
 * @since 5.3
 */
abstract class NamedParameterUtils {

	/**
	 * Set of characters that qualify as comment or quotes starting characters.
	 */
	private static final String[] START_SKIP = new String[] {"'", "\"", "--", "/*"};

	/**
	 * Set of characters that at are the corresponding comment or quotes ending characters.
	 */
	private static final String[] STOP_SKIP = new String[] {"'", "\"", "\n", "*/"};

	/**
	 * Set of characters that qualify as parameter separators,
	 * indicating that a parameter name in an SQL String has ended.
	 */
	private static final String PARAMETER_SEPARATORS = "\"':&,;()|=+-*%/\\<>^";

	/**
	 * An index with separator flags per character code.
	 * Technically only needed between 34 and 124 at this point.
	 */
	private static final boolean[] separatorIndex = new boolean[128];

	static {
		for (char c : PARAMETER_SEPARATORS.toCharArray()) {
			separatorIndex[c] = true;
		}
	}


	// -------------------------------------------------------------------------
	// Core methods used by NamedParameterSupport.
	// -------------------------------------------------------------------------

	/**
	 * Parse the SQL statement and locate any placeholders or named parameters.
	 * Named parameters are substituted for a R2DBC placeholder.
	 * @param sql the SQL statement
	 * @return the parsed statement, represented as {@link ParsedSql} instance
	 */
	public static ParsedSql parseSqlStatement(String sql) {
		Assert.notNull(sql, "SQL must not be null");

		Set<String> namedParameters = new HashSet<>();
		String sqlToUse = sql;
		List<ParameterHolder> parameterList = new ArrayList<>();

		char[] statement = sql.toCharArray();
		int namedParameterCount = 0;
		int unnamedParameterCount = 0;
		int totalParameterCount = 0;

		int escapes = 0;
		int i = 0;
		while (i < statement.length) {
			int skipToPosition = i;
			while (i < statement.length) {
				skipToPosition = skipCommentsAndQuotes(statement, i);
				if (i == skipToPosition) {
					break;
				}
				else {
					i = skipToPosition;
				}
			}
			if (i >= statement.length) {
				break;
			}
			char c = statement[i];
			if (c == ':' || c == '&') {
				int j = i + 1;
				if (c == ':' && j < statement.length && statement[j] == ':') {
					// Postgres-style "::" casting operator should be skipped
					i = i + 2;
					continue;
				}
				String parameter = null;
				if (c == ':' && j < statement.length && statement[j] == '{') {
					// :{x} style parameter
					while (statement[j] != '}') {
						j++;
						if (j >= statement.length) {
							throw new InvalidDataAccessApiUsageException(
									"Non-terminated named parameter declaration at position " + i +
									" in statement: " + sql);
						}
						if (statement[j] == ':' || statement[j] == '{') {
							throw new InvalidDataAccessApiUsageException(
									"Parameter name contains invalid character '" + statement[j] +
									"' at position " + i + " in statement: " + sql);
						}
					}
					if (j - i > 2) {
						parameter = sql.substring(i + 2, j);
						namedParameterCount = addNewNamedParameter(
								namedParameters, namedParameterCount, parameter);
						totalParameterCount = addNamedParameter(
								parameterList, totalParameterCount, escapes, i, j + 1, parameter);
					}
					j++;
				}
				else {
					while (j < statement.length && !isParameterSeparator(statement[j])) {
						j++;
					}
					if (j - i > 1) {
						parameter = sql.substring(i + 1, j);
						namedParameterCount = addNewNamedParameter(
								namedParameters, namedParameterCount, parameter);
						totalParameterCount = addNamedParameter(
								parameterList, totalParameterCount, escapes, i, j, parameter);
					}
				}
				i = j - 1;
			}
			else {
				if (c == '\\') {
					int j = i + 1;
					if (j < statement.length && statement[j] == ':') {
						// escaped ":" should be skipped
						sqlToUse = sqlToUse.substring(0, i - escapes)
								+ sqlToUse.substring(i - escapes + 1);
						escapes++;
						i = i + 2;
						continue;
					}
				}
			}
			i++;
		}
		ParsedSql parsedSql = new ParsedSql(sqlToUse);
		for (ParameterHolder ph : parameterList) {
			parsedSql.addNamedParameter(ph.getParameterName(), ph.getStartIndex(), ph.getEndIndex());
		}
		parsedSql.setNamedParameterCount(namedParameterCount);
		parsedSql.setUnnamedParameterCount(unnamedParameterCount);
		parsedSql.setTotalParameterCount(totalParameterCount);
		return parsedSql;
	}

	private static int addNamedParameter(List<ParameterHolder> parameterList,
			int totalParameterCount, int escapes, int i, int j, String parameter) {

		parameterList.add(new ParameterHolder(parameter, i - escapes, j - escapes));
		totalParameterCount++;
		return totalParameterCount;
	}

	private static int addNewNamedParameter(Set<String> namedParameters, int namedParameterCount, String parameter) {
		if (!namedParameters.contains(parameter)) {
			namedParameters.add(parameter);
			namedParameterCount++;
		}
		return namedParameterCount;
	}

	/**
	 * Skip over comments and quoted names present in an SQL statement.
	 * @param statement character array containing SQL statement
	 * @param position current position of statement
	 * @return next position to process after any comments or quotes are skipped
	 */
	private static int skipCommentsAndQuotes(char[] statement, int position) {
		for (int i = 0; i < START_SKIP.length; i++) {
			if (statement[position] == START_SKIP[i].charAt(0)) {
				boolean match = true;
				for (int j = 1; j < START_SKIP[i].length(); j++) {
					if (statement[position + j] != START_SKIP[i].charAt(j)) {
						match = false;
						break;
					}
				}
				if (match) {
					int offset = START_SKIP[i].length();
					for (int m = position + offset; m < statement.length; m++) {
						if (statement[m] == STOP_SKIP[i].charAt(0)) {
							boolean endMatch = true;
							int endPos = m;
							for (int n = 1; n < STOP_SKIP[i].length(); n++) {
								if (m + n >= statement.length) {
									// last comment not closed properly
									return statement.length;
								}
								if (statement[m + n] != STOP_SKIP[i].charAt(n)) {
									endMatch = false;
									break;
								}
								endPos = m + n;
							}
							if (endMatch) {
								// found character sequence ending comment or quote
								return endPos + 1;
							}
						}
					}
					// character sequence ending comment or quote not found
					return statement.length;
				}
			}
		}
		return position;
	}

	/**
	 * Parse the SQL statement and locate any placeholders or named parameters. Named
	 * parameters are substituted for a R2DBC placeholder, and any select list is expanded
	 * to the required number of placeholders. Select lists may contain an array of objects,
	 * and in that case the placeholders will be grouped and enclosed with parentheses.
	 * This allows for the use of "expression lists" in the SQL statement like:
	 * {@code select id, name, state from table where (name, age) in (('John', 35), ('Ann', 50))}
	 * <p>The parameter values passed in are used to determine the number of placeholders to
	 * be used for a select list. Select lists should be limited to 100 or fewer elements.
	 * A larger number of elements is not guaranteed to be supported by the database and
	 * is strictly vendor-dependent.
	 * @param parsedSql the parsed representation of the SQL statement
	 * @param bindMarkersFactory the bind marker factory.
	 * @param paramSource the source for named parameters
	 * @return the expanded query that accepts bind parameters and allows for execution
	 * without further translation
	 * @see #parseSqlStatement
	 */
	public static PreparedOperation<String> substituteNamedParameters(ParsedSql parsedSql,
			BindMarkersFactory bindMarkersFactory, BindParameterSource paramSource) {

		NamedParameters markerHolder = new NamedParameters(bindMarkersFactory);
		String originalSql = parsedSql.getOriginalSql();
		List<String> paramNames = parsedSql.getParameterNames();
		if (paramNames.isEmpty()) {
			return new ExpandedQuery(originalSql, markerHolder, paramSource);
		}

		StringBuilder actualSql = new StringBuilder(originalSql.length());
		int lastIndex = 0;
		for (int i = 0; i < paramNames.size(); i++) {
			String paramName = paramNames.get(i);
			int[] indexes = parsedSql.getParameterIndexes(i);
			int startIndex = indexes[0];
			int endIndex = indexes[1];
			actualSql.append(originalSql, lastIndex, startIndex);
			NamedParameters.NamedParameter marker = markerHolder.getOrCreate(paramName);
			if (paramSource.hasValue(paramName)) {
				Object value = paramSource.getValue(paramName);
				if (value instanceof Collection) {

					Iterator<?> entryIter = ((Collection<?>) value).iterator();
					int k = 0;
					int counter = 0;
					while (entryIter.hasNext()) {
						if (k > 0) {
							actualSql.append(", ");
						}
						k++;
						Object entryItem = entryIter.next();
						if (entryItem instanceof Object[]) {
							Object[] expressionList = (Object[]) entryItem;
							actualSql.append('(');
							for (int m = 0; m < expressionList.length; m++) {
								if (m > 0) {
									actualSql.append(", ");
								}
								actualSql.append(marker.getPlaceholder(counter));
								counter++;
							}
							actualSql.append(')');
						}
						else {
							actualSql.append(marker.getPlaceholder(counter));
							counter++;
						}
					}
				}
				else {
					actualSql.append(marker.getPlaceholder());
				}
			}
			else {
				actualSql.append(marker.getPlaceholder());
			}
			lastIndex = endIndex;
		}
		actualSql.append(originalSql, lastIndex, originalSql.length());

		return new ExpandedQuery(actualSql.toString(), markerHolder, paramSource);
	}

	/**
	 * Determine whether a parameter name ends at the current position,
	 * that is, whether the given character qualifies as a separator.
	 */
	private static boolean isParameterSeparator(char c) {
		return (c < 128 && separatorIndex[c]) || Character.isWhitespace(c);
	}


	// -------------------------------------------------------------------------
	// Convenience methods operating on a plain SQL String
	// -------------------------------------------------------------------------

	/**
	 * Parse the SQL statement and locate any placeholders or named parameters.
	 * Named parameters are substituted for a native placeholder and any
	 * select list is expanded to the required number of placeholders.
	 * @param sql the SQL statement
	 * @param bindMarkersFactory the bind marker factory
	 * @param paramSource the source for named parameters
	 * @return the expanded query that accepts bind parameters and allows for execution
	 * without further translation
	 */
	public static PreparedOperation<String> substituteNamedParameters(String sql,
			BindMarkersFactory bindMarkersFactory, BindParameterSource paramSource) {

		ParsedSql parsedSql = parseSqlStatement(sql);
		return substituteNamedParameters(parsedSql, bindMarkersFactory, paramSource);
	}


	private static class ParameterHolder {

		private final String parameterName;

		private final int startIndex;

		private final int endIndex;

		ParameterHolder(String parameterName, int startIndex, int endIndex) {
			this.parameterName = parameterName;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
		}

		String getParameterName() {
			return this.parameterName;
		}

		int getStartIndex() {
			return this.startIndex;
		}

		int getEndIndex() {
			return this.endIndex;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof ParameterHolder)) {
				return false;
			}
			ParameterHolder that = (ParameterHolder) o;
			return this.startIndex == that.startIndex && this.endIndex == that.endIndex
					&& Objects.equals(this.parameterName, that.parameterName);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.parameterName, this.startIndex, this.endIndex);
		}
	}


	/**
	 * Holder for bind markers progress.
	 */
	static class NamedParameters {

		private final BindMarkers bindMarkers;

		private final boolean identifiable;

		private final Map<String, List<NamedParameter>> references = new TreeMap<>();

		NamedParameters(BindMarkersFactory factory) {
			this.bindMarkers = factory.create();
			this.identifiable = factory.identifiablePlaceholders();
		}

		/**
		 * Get the {@link NamedParameter} identified by {@code namedParameter}.
		 * Parameter objects get created if they do not yet exist.
		 * @param namedParameter the parameter name
		 * @return the named parameter
		 */
		NamedParameter getOrCreate(String namedParameter) {
			List<NamedParameter> reference = this.references.computeIfAbsent(
					namedParameter, key -> new ArrayList<>());
			if (reference.isEmpty()) {
				NamedParameter param = new NamedParameter(namedParameter);
				reference.add(param);
				return param;
			}
			if (this.identifiable) {
				return reference.get(0);
			}
			NamedParameter param = new NamedParameter(namedParameter);
			reference.add(param);
			return param;
		}

		@Nullable
		List<NamedParameter> getMarker(String name) {
			return this.references.get(name);
		}


		class NamedParameter {

			private final String namedParameter;

			private final List<BindMarker> placeholders = new ArrayList<>();

			NamedParameter(String namedParameter) {
				this.namedParameter = namedParameter;
			}

			/**
			 * Create a placeholder to translate a single value into a bindable parameter.
			 * <p>Can be called multiple times to create placeholders for array/collections.
			 * @return the placeholder to be used in the SQL statement
			 */
			String addPlaceholder() {
				BindMarker bindMarker = NamedParameters.this.bindMarkers.next(this.namedParameter);
				this.placeholders.add(bindMarker);
				return bindMarker.getPlaceholder();
			}

			String getPlaceholder() {
				return getPlaceholder(0);
			}

			String getPlaceholder(int counter) {
				while (counter + 1 > this.placeholders.size()) {
					addPlaceholder();
				}
				return this.placeholders.get(counter).getPlaceholder();
			}
		}
	}


	/**
	 * Expanded query that allows binding of parameters using parameter names that were
	 * used to expand the query. Binding unrolls {@link Collection}s and nested arrays.
	 */
	private static class ExpandedQuery implements PreparedOperation<String> {

		private final String expandedSql;

		private final NamedParameters parameters;

		private final BindParameterSource parameterSource;

		ExpandedQuery(String expandedSql, NamedParameters parameters, BindParameterSource parameterSource) {
			this.expandedSql = expandedSql;
			this.parameters = parameters;
			this.parameterSource = parameterSource;
		}

		@SuppressWarnings("unchecked")
		public void bind(BindTarget target, String identifier, Object value) {
			List<BindMarker> bindMarkers = getBindMarkers(identifier);
			if (bindMarkers == null) {
				target.bind(identifier, value);
				return;
			}
			if (value instanceof Collection) {
				Collection<Object> collection = (Collection<Object>) value;
				Iterator<Object> iterator = collection.iterator();
				Iterator<BindMarker> markers = bindMarkers.iterator();
				while (iterator.hasNext()) {
					Object valueToBind = iterator.next();
					if (valueToBind instanceof Object[]) {
						Object[] objects = (Object[]) valueToBind;
						for (Object object : objects) {
							bind(target, markers, object);
						}
					}
					else {
						bind(target, markers, valueToBind);
					}
				}
			}
			else {
				for (BindMarker bindMarker : bindMarkers) {
					bindMarker.bind(target, value);
				}
			}
		}

		private void bind(BindTarget target, Iterator<BindMarker> markers, Object valueToBind) {
			Assert.isTrue(markers.hasNext(), () -> String.format(
					"No bind marker for value [%s] in SQL [%s]. Check that the query was expanded using the same arguments.",
					valueToBind, toQuery()));
			markers.next().bind(target, valueToBind);
		}

		public void bindNull(BindTarget target, String identifier, Class<?> valueType) {
			List<BindMarker> bindMarkers = getBindMarkers(identifier);
			if (bindMarkers == null) {
				target.bindNull(identifier, valueType);
				return;
			}
			for (BindMarker bindMarker : bindMarkers) {
				bindMarker.bindNull(target, valueType);
			}
		}

		@Nullable
		List<BindMarker> getBindMarkers(String identifier) {
			List<NamedParameters.NamedParameter> parameters = this.parameters.getMarker(identifier);
			if (parameters == null) {
				return null;
			}
			List<BindMarker> markers = new ArrayList<>();
			for (NamedParameters.NamedParameter parameter : parameters) {
				markers.addAll(parameter.placeholders);
			}
			return markers;
		}

		@Override
		public String getSource() {
			return this.expandedSql;
		}

		@Override
		public void bindTo(BindTarget target) {
			for (String namedParameter : this.parameterSource.getParameterNames()) {
				Object value = this.parameterSource.getValue(namedParameter);
				if (value == null) {
					bindNull(target, namedParameter, this.parameterSource.getType(namedParameter));
				}
				else {
					bind(target, namedParameter, value);
				}
			}
		}

		@Override
		public String toQuery() {
			return this.expandedSql;
		}
	}

}
