/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint

import kotlin.reflect.KClass

/**
 * Extension for [JdkProxyHint.Builder.proxiedInterfaces] providing a [KClass] based variant.
 *
 * @author Sebastien Deleuze
 * @since 6.0.5
 */
fun JdkProxyHint.Builder.proxiedInterfaces(vararg proxiedInterfaces: KClass<*>) =
	proxiedInterfaces(*proxiedInterfaces.map { it.java }.toTypedArray())
