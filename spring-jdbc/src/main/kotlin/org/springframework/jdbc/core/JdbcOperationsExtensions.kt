/*
 * Copyright 2002-2017 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
fun <T : Any> JdbcOperations.queryForObject(sql: String, vararg args: Any, function: (ResultSet, Int) -> T): T? =
		queryForObject(sql, RowMapper { resultSet, i -> function(resultSet, i) }, *args)

/**
 * Extension for [JdbcOperations.queryForObject] providing a
 * `queryForObject<Foo>("...", arrayOf(arg1, argN), intArray(type1, typeN))` variant.
 *
 * @author Mario Arias
 * @since 5.0
 */
inline fun <reified T : Any> JdbcOperations.queryForObject(sql: String, args: Array<out Any>, argTypes: IntArray): T? =
		queryForObject(sql, args, argTypes, T::class.java)

/**
 * Extension for [JdbcOperations.queryForObject] providing a
 * `queryForObject<Foo>("...", arrayOf(arg1, argN))` variant.
 *
 * @author Mario Arias
 * @since 5.0
 */
inline fun <reified T : Any> JdbcOperations.queryForObject(sql: String, args: Array<out Any>): T? =
		queryForObject(sql, args, T::class.java)

/**
 * Extension for [JdbcOperations.queryForList] providing a `queryForList<Foo>("...")` variant.
 *
 * @author Mario Arias
 * @since 5.0
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
inline fun <reified T : Any> JdbcOperations.queryForList(sql: String): List<T> =
		queryForList(sql, T::class.java)

/**
 * Extension for [JdbcOperations.queryForList] providing a
 * `queryForList<Foo>("...", arrayOf(arg1, argN), intArray(type1, typeN))` variant.
 *
 * @author Mario Arias
 * @since 5.0
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
inline fun <reified T : Any> JdbcOperations.queryForList(sql: String, args: Array<out Any>,
		argTypes: IntArray): List<T> =
		queryForList(sql, args, argTypes, T::class.java)

/**
 * Extension for [JdbcOperations.queryForList] providing a
 * `queryForList<Foo>("...", arrayOf(arg1, argN))` variant.
 *
 * @author Mario Arias
 * @since 5.0
 */
inline fun <reified T : Any> JdbcOperations.queryForList(sql: String, args: Array<out Any>): List<T> =
		queryForList(sql, args, T::class.java)

/**
 * Extension for [JdbcOperations.query] providing a ResultSetExtractor-like function
 * variant: `query<Foo>("...", arg1, argN){ rs -> }`.
 *
 * @author Mario Arias
 * @since 5.0
 */
inline fun <reified T : Any> JdbcOperations.query(sql: String, vararg args: Any,
		crossinline function: (ResultSet) -> T): T? =
		query(sql, ResultSetExtractor { function(it) }, *args)

/**
 * Extension for [JdbcOperations.query] providing a RowCallbackHandler-like function
 * variant: `query("...", arg1, argN){ rs -> }`.
 *
 * @author Mario Arias
 * @since 5.0
 */
fun JdbcOperations.query(sql: String, vararg args: Any, function: (ResultSet) -> Unit): Unit =
		query(sql, RowCallbackHandler { function(it) }, *args)

/**
 * Extensions for [JdbcOperations.query] providing a RowMapper-like function variant:
 * `query("...", arg1, argN){ rs, i -> }`.
 *
 * @author Mario Arias
 * @since 5.0
 */
fun <T : Any> JdbcOperations.query(sql: String, vararg args: Any, function: (ResultSet, Int) -> T): List<T> =
		query(sql, RowMapper { rs, i -> function(rs, i) }, *args)
