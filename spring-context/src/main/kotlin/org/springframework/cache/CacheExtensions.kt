/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache


/**
 * Extension for [Cache.get] providing a `get<Foo>()` variant.
 *
 * @author Mikhael Sokolov
 * @since 6.0
 */
inline fun <reified T : Any> Cache.get(key: Any): T? = get(key, T::class.java)

/**
 * Extension for [Cache.get] providing a `foo[key]` variant.
 *
 * @author Mikhael Sokolov
 * @since 6.0
 */
operator fun Cache.get(key: Any): Cache.ValueWrapper? = get(key)

/**
 * Extension for [Cache.put] providing a `foo[key]` variant.
 *
 * @author Mikhael Sokolov
 * @since 6.0
 */
operator fun Cache.set(key: Any, value: Any?) = put(key, value)