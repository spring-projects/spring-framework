/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.namedparam

/**
 * Extension for [MapSqlParameterSource.addValue] providing Array like setter.
 *
 * ```kotlin
 * source["age"] = 3
 * ```
 * @author Mario Arias
 * @since 5.0
 *
 */
operator fun MapSqlParameterSource.set(paramName: String, value: Any?) {
	this.addValue(paramName, value)
}

/**
 * Extension for [MapSqlParameterSource.addValue] providing Array like setter.
 *
 * ```kotlin
 * source["age", JDBCType.INTEGER.vendorTypeNumber] = 3
 * ```
 * @author Mario Arias
 * @since 5.0
 *
 */
operator fun MapSqlParameterSource.set(paramName: String, sqlType: Int, value: Any?) {
	this.addValue(paramName, value, sqlType)
}

/**
 * Extension for [MapSqlParameterSource.addValue] providing Array like setter
 *
 * ```kotlin
 * source["age", JDBCType.INTEGER.vendorTypeNumber, "INT"] = 3
 * ```
 * @author Mario Arias
 * @since 5.0
 *
 */
operator fun MapSqlParameterSource.set(paramName: String, sqlType: Int, typeName: String, value: Any?) {
	this.addValue(paramName, value, sqlType, typeName)
}
