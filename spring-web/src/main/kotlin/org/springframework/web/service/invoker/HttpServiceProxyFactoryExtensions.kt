/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.invoker

/**
 * Extension for [HttpServiceProxyFactory.createClient] providing a `createClient<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 6.0.5
 */
inline fun <reified T : Any> HttpServiceProxyFactory.createClient(): T =
	createClient(T::class.java)
