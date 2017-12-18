/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.core.namedparam;

import org.springframework.lang.Nullable;

/**
 * A simple empty implementation of the {@link SqlParameterSource} interface.
 *
 * @author Juergen Hoeller
 * @since 3.2.2
 */
public class EmptySqlParameterSource implements SqlParameterSource {

	/**
	 * A shared instance of {@link EmptySqlParameterSource}.
	 */
	public static final EmptySqlParameterSource INSTANCE = new EmptySqlParameterSource();


	@Override
	public boolean hasValue(String paramName) {
		return false;
	}

	@Override
	@Nullable
	public Object getValue(String paramName) throws IllegalArgumentException {
		throw new IllegalArgumentException("This SqlParameterSource is empty");
	}

	@Override
	public int getSqlType(String paramName) {
		return TYPE_UNKNOWN;
	}

	@Override
	@Nullable
	public String getTypeName(String paramName) {
		return null;
	}

}
