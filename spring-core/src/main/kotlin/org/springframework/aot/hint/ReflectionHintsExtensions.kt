/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint

/**
 * Extension for [ReflectionHints.getTypeHint] providing a `getTypeHint<Foo>(...)`
 * variant.
 *
 * @author Sebastien Deleuze
 * @since 6.0.5
 */
inline fun <reified T> ReflectionHints.getTypeHint() =
	getTypeHint(T::class.java)

/**
 * Extension for [ReflectionHints.registerType] providing a `registerType<Foo> { ... }`
 * variant.
 *
 * @author Sebastien Deleuze
 * @since 6.0.5
 */
inline fun <reified T> ReflectionHints.registerType(noinline typeHint: (TypeHint.Builder) -> Unit) =
	registerType(T::class.java, typeHint::invoke)

/**
 * Extension for [ReflectionHints.registerType] providing a `registerType<Foo>(...)`
 * variant.
 *
 * @author Sebastien Deleuze
 * @since 6.0.5
 */
inline fun <reified T> ReflectionHints.registerType(vararg memberCategories: MemberCategory) =
	registerType(T::class.java, *memberCategories)
