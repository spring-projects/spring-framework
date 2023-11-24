/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.cache


/**
 * Extension for [Cache.get] providing a `get<Foo>()` variant.
 *
 * @author Mikhael Sokolov
 * @since 6.0
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
inline fun <reified T : Any> Cache.get(key: Any): T? = get(key, T::class.java)

/**
 * Extension for [Cache.get] providing a `foo[key]` variant.
 *
 * @author Mikhael Sokolov
 * @since 6.0
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
operator fun Cache.get(key: Any): Cache.ValueWrapper? = get(key)

/**
 * Extension for [Cache.put] providing a `foo[key]` variant.
 *
 * @author Mikhael Sokolov
 * @since 6.0
 */
operator fun Cache.set(key: Any, value: Any?) = put(key, value)