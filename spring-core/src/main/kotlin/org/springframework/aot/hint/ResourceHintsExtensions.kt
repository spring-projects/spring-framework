/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint

/**
 * Extension for [ResourceHints.registerType] providing a `registerType<Foo>()`
 * variant.
 *
 * @author Sebastien Deleuze
 * @since 6.0.5
 */
inline fun <reified T> ResourceHints.registerType() =
	registerType(T::class.java)
