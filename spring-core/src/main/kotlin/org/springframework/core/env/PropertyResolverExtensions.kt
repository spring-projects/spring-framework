/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.core.env

/**
 * Extension for [PropertyResolver.getProperty] providing Array like getter returning a
 * nullable [String].
 *
 * ```kotlin
 * val name = env["name"] ?: "Seb"
 * ```
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
operator fun PropertyResolver.get(key: String) : String? = getProperty(key)


/**
 * Extension for [PropertyResolver.getProperty] providing a `getProperty<Foo>(...)`
 * variant returning a nullable `Foo`.
 *
 * @author Sebastien Deleuze
 * @since 5.1
 */
inline fun <reified T : Any> PropertyResolver.getProperty(key: String) : T? =
		getProperty(key, T::class.java)

/**
 * Extension for [PropertyResolver.getProperty] providing a `getProperty<Foo>(...)`
 * variant returning a non-nullable `Foo` with a default value.
 *
 * @author John Burns
 * @since 6.1
 */
inline fun <reified T : Any> PropertyResolver.getProperty(key: String, default: T) : T =
	getProperty(key, T::class.java, default)

/**
 * Extension for [PropertyResolver.getRequiredProperty] providing a
 * `getRequiredProperty<Foo>(...)` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.1
 */
inline fun <reified T: Any> PropertyResolver.getRequiredProperty(key: String) : T =
		getRequiredProperty(key, T::class.java)
