/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
