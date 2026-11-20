/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.http.codec.multipart.Part
import org.springframework.util.MultiValueMap
import java.security.Principal

/**
 * Coroutines variant of [ServerWebExchange.getFormData].
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
suspend fun ServerWebExchange.awaitFormData(): MultiValueMap<String, String> =
		this.formData.awaitSingle()

/**
 * Coroutines variant of [ServerWebExchange.getMultipartData].
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
suspend fun ServerWebExchange.awaitMultipartData(): MultiValueMap<String, Part> =
		this.multipartData.awaitSingle()

/**
 * Coroutines variant of [ServerWebExchange.getPrincipal].
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
suspend fun <T : Principal> ServerWebExchange.awaitPrincipal(): T =
		this.getPrincipal<T>().awaitSingle()

/**
 * Coroutines variant of [ServerWebExchange.getSession].
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
suspend fun ServerWebExchange.awaitSession(): WebSession =
		this.session.awaitSingle()

/**
 * Coroutines variant of [ServerWebExchange.Builder.principal].
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
fun ServerWebExchange.Builder.principal(supplier: suspend () -> Principal): ServerWebExchange.Builder
		= principal(mono(Dispatchers.Unconfined) { supplier.invoke() })
