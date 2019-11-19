/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.jdbc.core.namedparam;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * A composite implementation of the {@link SqlParameterSource} interface.
 *
 * @author Kazuki Shimizu
 * @since 5.2.2
 * @see BeanPropertySqlParameterSource
 * @see MapSqlParameterSource
 */
public final class CompositeSqlParameterSource implements SqlParameterSource {

	private final Map<String, SqlParameterSource> indexByParamName = new HashMap<>();

	private final SqlParameterSource[] sources;

	private CompositeSqlParameterSource(SqlParameterSource... sources) {
		this.sources = sources;
	}

	/**
	 * Return a composite {@link SqlParameterSource} with specified sources.
	 *
	 * @param sources composite target sources
	 * @return a composite {@link SqlParameterSource}
	 */
	public static SqlParameterSource of(SqlParameterSource... sources) {
		return new CompositeSqlParameterSource(sources);
	}

	@Override
	public boolean hasValue(String paramName) {
		return findSqlParameterSourceByParamName(paramName) != null;
	}

	@Override
	@Nullable
	public Object getValue(String paramName) {
		SqlParameterSource source = findSqlParameterSourceByParamName(paramName);
		return source == null ? null : source.getValue(paramName);
	}

	@Override
	@Nullable
	public String getTypeName(String paramName) {
		SqlParameterSource source = findSqlParameterSourceByParamName(paramName);
		return source == null ? null : source.getTypeName(paramName);
	}

	@Override
	public int getSqlType(String paramName) {
		SqlParameterSource source = findSqlParameterSourceByParamName(paramName);
		return source == null ? TYPE_UNKNOWN : source.getSqlType(paramName);
	}

	@Override
	@NonNull
	public String[] getParameterNames() {
		return Stream.of(this.sources)
				.map(SqlParameterSource::getParameterNames)
				.filter(Objects::nonNull)
				.flatMap(Stream::of)
				.distinct()
				.toArray(String[]::new);
	}

	@Nullable
	private SqlParameterSource findSqlParameterSourceByParamName(String paramName) {
		return this.indexByParamName.computeIfAbsent(paramName, x ->
				Stream.of(this.sources)
				.filter(s -> s.hasValue(paramName))
				.findFirst()
				.orElse(null));
	}

}
