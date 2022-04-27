/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.http.codec.multipart.Part
import org.springframework.util.MultiValueMap
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebSession
import java.security.Principal

/**
 * Coroutines variant of [ServerWebExchange.getFormData].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend fun ServerWebExchange.awaitFormData(): MultiValueMap<String, String> =
		this.formData.awaitSingle()

/**
 * Coroutines variant of [ServerWebExchange.getMultipartData].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend fun ServerWebExchange.awaitMultipartData(): MultiValueMap<String, Part> =
		this.multipartData.awaitSingle()

/**
 * Coroutines variant of [ServerWebExchange.getPrincipal].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend fun <T : Principal> ServerWebExchange.awaitPrincipal(): T =
		this.getPrincipal<T>().awaitSingle()

/**
 * Coroutines variant of [ServerWebExchange.getSession].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend fun ServerWebExchange.awaitSession(): WebSession =
		this.session.awaitSingle()

/**
 * Coroutines variant of [ServerWebExchange.Builder.principal].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun ServerWebExchange.Builder.principal(supplier: suspend () -> Principal): ServerWebExchange.Builder
        = principal(mono(Dispatchers.Unconfined) { supplier.invoke() })
