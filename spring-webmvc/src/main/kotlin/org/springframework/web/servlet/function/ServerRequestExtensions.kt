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

package org.springframework.web.servlet.function

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import java.net.InetSocketAddress
import java.security.Principal

/**
 * Nullable variant of [ServerRequest.remoteAddress]
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun ServerRequest.remoteAddressOrNull(): InetSocketAddress? = remoteAddress().orElse(null)

/**
 * Extension for [ServerRequest.body] providing a `body<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
inline fun <reified T : Any> ServerRequest.body(): T = body(object : ParameterizedTypeReference<T>() {})

/**
 * Nullable variant of [ServerRequest.attribute]
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun ServerRequest.attributeOrNull(name: String): Any? = attribute(name).orElse(null)

/**
 * Nullable variant of [ServerRequest.param]
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun ServerRequest.paramOrNull(name: String): String? = param(name).orElse(null)

/**
 * Nullable variant of [ServerRequest.param]
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun ServerRequest.principalOrNull(): Principal? = principal().orElse(null)

/**
 * Nullable variant of [ServerRequest.Headers.contentLength]
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun ServerRequest.Headers.contentLengthOrNull(): Long? = contentLength().let { if (it.isPresent) it.asLong else null }

/**
 * Nullable variant of [ServerRequest.Headers.contentType]
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun ServerRequest.Headers.contentTypeOrNull(): MediaType? = contentType().orElse(null)
