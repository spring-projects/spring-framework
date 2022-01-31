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

package org.springframework.jdbc.core.metadata;

import org.springframework.lang.Nullable;

import java.sql.DatabaseMetaData;

/**
 * Holder of Orcle meta-data for a specific parameter that is used for call processing.
 *
 * @author Loïc Lefèvre
 * @since 5.3.16
 * @see GenericCallMetaDataProvider
 */
public class OracleCallParameterMetaData extends CallParameterMetaData {
	// denotes the procedure or function overload number this parameter belongs to
	protected final int overload;

	/**
	 * Constructor taking all the properties including the function marker and the overload number.
	 * @since 5.3.16
	 */
	public OracleCallParameterMetaData(boolean function, @Nullable String columnName, int columnType,
								 int sqlType, @Nullable String typeName, boolean nullable, int overload) {

		super(function, columnName, columnType, sqlType, typeName, nullable);
		this.overload = overload;
	}

	/**
	 * Determine whether the declared parameter qualifies as a 'return' parameter
	 * for our purposes: type {@link DatabaseMetaData#procedureColumnReturn} or
	 * {@link DatabaseMetaData#procedureColumnResult}, or in case of a function,
	 * {@link DatabaseMetaData#functionReturn}.
	 * @since 5.3.16
	 */
	@Override
	public boolean isReturnParameter() {
		return (this.function ? (this.parameterType == DatabaseMetaData.functionReturn || this.parameterType == DatabaseMetaData.functionColumnResult) :
				(this.parameterType == DatabaseMetaData.procedureColumnReturn ||
						this.parameterType == DatabaseMetaData.procedureColumnResult));
	}
}
