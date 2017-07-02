/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jdbc.core;

import java.util.LinkedList;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Object to represent a SQL parameter definition.
 *
 * <p>Parameters may be anonymous, in which case "name" is {@code null}.
 * However, all parameters must define a SQL type according to {@link java.sql.Types}.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see java.sql.Types
 */
public class SqlParameter {

	/** The name of the parameter, if any */
	@Nullable
	private String name;

	/** SQL type constant from {@code java.sql.Types} */
	private final int sqlType;

	/** Used for types that are user-named like: STRUCT, DISTINCT, JAVA_OBJECT, named array types */
	@Nullable
	private String typeName;

	/** The scale to apply in case of a NUMERIC or DECIMAL type, if any */
	@Nullable
	private Integer scale;


	/**
	 * Create a new anonymous SqlParameter, supplying the SQL type.
	 * @param sqlType SQL type of the parameter according to {@code java.sql.Types}
	 */
	public SqlParameter(int sqlType) {
		this.sqlType = sqlType;
	}

	/**
	 * Create a new anonymous SqlParameter, supplying the SQL type.
	 * @param sqlType SQL type of the parameter according to {@code java.sql.Types}
	 * @param typeName the type name of the parameter (optional)
	 */
	public SqlParameter(int sqlType, @Nullable String typeName) {
		this.sqlType = sqlType;
		this.typeName = typeName;
	}

	/**
	 * Create a new anonymous SqlParameter, supplying the SQL type.
	 * @param sqlType SQL type of the parameter according to {@code java.sql.Types}
	 * @param scale the number of digits after the decimal point
	 * (for DECIMAL and NUMERIC types)
	 */
	public SqlParameter(int sqlType, int scale) {
		this.sqlType = sqlType;
		this.scale = scale;
	}

	/**
	 * Create a new SqlParameter, supplying name and SQL type.
	 * @param name name of the parameter, as used in input and output maps
	 * @param sqlType SQL type of the parameter according to {@code java.sql.Types}
	 */
	public SqlParameter(String name, int sqlType) {
		this.name = name;
		this.sqlType = sqlType;
	}

	/**
	 * Create a new SqlParameter, supplying name and SQL type.
	 * @param name name of the parameter, as used in input and output maps
	 * @param sqlType SQL type of the parameter according to {@code java.sql.Types}
	 * @param typeName the type name of the parameter (optional)
	 */
	public SqlParameter(String name, int sqlType, @Nullable String typeName) {
		this.name = name;
		this.sqlType = sqlType;
		this.typeName = typeName;
	}

	/**
	 * Create a new SqlParameter, supplying name and SQL type.
	 * @param name name of the parameter, as used in input and output maps
	 * @param sqlType SQL type of the parameter according to {@code java.sql.Types}
	 * @param scale the number of digits after the decimal point
	 * (for DECIMAL and NUMERIC types)
	 */
	public SqlParameter(String name, int sqlType, int scale) {
		this.name = name;
		this.sqlType = sqlType;
		this.scale = scale;
	}

	/**
	 * Copy constructor.
	 * @param otherParam the SqlParameter object to copy from
	 */
	public SqlParameter(SqlParameter otherParam) {
		Assert.notNull(otherParam, "SqlParameter object must not be null");
		this.name = otherParam.name;
		this.sqlType = otherParam.sqlType;
		this.typeName = otherParam.typeName;
		this.scale = otherParam.scale;
	}


	/**
	 * Return the name of the parameter, or {@code null} if anonymous.
	 */
	@Nullable
	public String getName() {
		return this.name;
	}

	/**
	 * Return the SQL type of the parameter.
	 */
	public int getSqlType() {
		return this.sqlType;
	}

	/**
	 * Return the type name of the parameter, if any.
	 */
	@Nullable
	public String getTypeName() {
		return this.typeName;
	}

	/**
	 * Return the scale of the parameter, if any.
	 */
	@Nullable
	public Integer getScale() {
		return this.scale;
	}


	/**
	 * Return whether this parameter holds input values that should be set
	 * before execution even if they are {@code null}.
	 * <p>This implementation always returns {@code true}.
	 */
	public boolean isInputValueProvided() {
		return true;
	}

	/**
	 * Return whether this parameter is an implicit return parameter used during the
	 * results preocessing of the CallableStatement.getMoreResults/getUpdateCount.
	 * <p>This implementation always returns {@code false}.
	 */
	public boolean isResultsParameter() {
		return false;
	}


	/**
	 * Convert a list of JDBC types, as defined in {@code java.sql.Types},
	 * to a List of SqlParameter objects as used in this package.
	 */
	public static List<SqlParameter> sqlTypesToAnonymousParameterList(@Nullable int... types) {
		List<SqlParameter> result = new LinkedList<>();
		if (types != null) {
			for (int type : types) {
				result.add(new SqlParameter(type));
			}
		}
		return result;
	}

}
