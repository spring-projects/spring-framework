/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint

import java.io.Serializable

/**
 * Extension for [SerializationHints.registerType] providing a `registerType<Foo>()`
 * variant.
 *
 * @author Sebastien Deleuze
 * @since 6.0.5
 */
@Suppress("removal", "DEPRECATION")
@Deprecated(level = DeprecationLevel.WARNING, message = "Use ReflectionHints")
inline fun <reified T : Serializable> SerializationHints.registerType(noinline serializationHint: (JavaSerializationHint.Builder) -> Unit = {}) =
	registerType(T::class.java, serializationHint::invoke)
