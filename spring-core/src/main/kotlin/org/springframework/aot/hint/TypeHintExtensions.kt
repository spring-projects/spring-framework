/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint

/**
 * Extension for [TypeHint.Builder.onReachableType] providing a `onReachableType<Foo>()`
 * variant.
 *
 * @author Sebastien Deleuze
 * @since 6.0.5
 */
inline fun <reified T>  TypeHint.Builder.onReachableType() =
	onReachableType(T::class.java)
