/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core

import java.sql.ResultSet

/**
 * Extension for [JdbcOperations.queryForObject] providing a `queryForObject<Foo>("...")` variant.
 *
 * @author Mario Arias
 * @since 5.0
 */
inline fun <reified T : Any> JdbcOperations.queryForObject(sql: String): T? =
		queryForObject(sql, T::class.java)

/**
 * Extensions for [JdbcOperations.queryForObject] providing a RowMapper-like function
 * variant: `queryForObject("...", arg1, argN){ rs, i -> }`.
 *
 * @author Mario Arias
 * @since 5.0
 */
inline fun <reified T> JdbcOperations.queryForObject(sql: String, vararg args: Any?, crossinline function: (ResultSet, Int) -> T): T =
		queryForObject(sql, { resultSet, i -> function(resultSet, i) }, *args)

/**
 * Extension for [JdbcOperations.queryForObject] providing a
 * `queryForObject<Foo>("...", arrayOf(arg1, argN), intArray(type1, typeN))` variant.
 *
 * @author Mario Arias
 * @since 5.0
 */
inline fun <reified T : Any> JdbcOperations.queryForObject(sql: String, args: Array<out Any?>, argTypes: IntArray): T? =
		queryForObject(sql, args, argTypes, T::class.java)

/**
 * Extension for [JdbcOperations.queryForObject] providing a
 * `queryForObject<Foo>("...", arg1, argN)` variant.
 *
 * @author Sébastien Deleuze
 * @since 7.0
 */
inline fun <reified T : Any> JdbcOperations.queryForObject(sql: String, vararg args: Any?): T? =
		queryForObject(sql, T::class.java, *args)

/**
 * Extension for [JdbcOperations.queryForList] providing a `queryForList<Foo>("...")` variant.
 *
 * @author Mario Arias
 * @since 5.0
 */
inline fun <reified T : Any> JdbcOperations.queryForList(sql: String): List<T?> =
		queryForList(sql, T::class.java)

/**
 * Extension for [JdbcOperations.queryForList] providing a
 * `queryForList<Foo>("...", arrayOf(arg1, argN), intArray(type1, typeN))` variant.
 *
 * @author Mario Arias
 * @since 5.0
 */
inline fun <reified T : Any> JdbcOperations.queryForList(sql: String, args: Array<out Any?>,
												   argTypes: IntArray): List<T?> =
		queryForList(sql, args, argTypes, T::class.java)

/**
 * Extension for [JdbcOperations.queryForList] providing a
 * `queryForList<Foo>("...", arg1, argN)` variant.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 */
inline fun <reified T : Any> JdbcOperations.queryForList(sql: String, vararg args: Any?): List<T?> =
		queryForList(sql, T::class.java, *args)


/**
 * Extension for [JdbcOperations.query] providing a ResultSetExtractor-like function
 * variant: `query<Foo>("...", arg1, argN){ rs -> }`.
 *
 * @author Mario Arias
 * @since 5.0
 */
fun <T> JdbcOperations.query(sql: String, vararg args: Any?,
		function: (ResultSet) -> T): T =
		query(sql, ResultSetExtractor { function(it) }, *args)

/**
 * Extension for [JdbcOperations.query] providing a RowCallbackHandler-like function
 * variant: `query("...", arg1, argN){ rs -> }`.
 *
 * @author Mario Arias
 * @since 5.0
 */
fun JdbcOperations.query(sql: String, vararg args: Any?, function: (ResultSet) -> Unit): Unit =
		query(sql, { function(it) }, *args)

/**
 * Extensions for [JdbcOperations.query] providing a RowMapper-like function variant:
 * `query("...", arg1, argN){ rs, i -> }`.
 *
 * @author Mario Arias
 * @since 5.0
 */
fun <T> JdbcOperations.query(sql: String, vararg args: Any?, function: (ResultSet, Int) -> T): List<T> =
		query(sql, { rs, i -> function(rs, i) }, *args)
