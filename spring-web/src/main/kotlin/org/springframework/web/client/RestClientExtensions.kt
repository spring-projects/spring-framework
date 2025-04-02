/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.client

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.ResponseEntity

/**
 * Extension for [RestClient.RequestBodySpec.body] providing a `bodyWithType<Foo>(...)` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
inline fun <reified T : Any> RestClient.RequestBodySpec.bodyWithType(body: T): RestClient.RequestBodySpec =
	body(body, object : ParameterizedTypeReference<T>() {})


/**
 * Extension for [RestClient.ResponseSpec.body] providing a `body<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
inline fun <reified T : Any> RestClient.ResponseSpec.body(): T? =
	body(object : ParameterizedTypeReference<T>() {})

/**
 * Extension for [RestClient.ResponseSpec.body] providing a `requiredBody<Foo>()` variant with a non-nullable
 * return value.
 * @throws NoSuchElementException if there is no response body
 * @since 6.2
 */
inline fun <reified T : Any> RestClient.ResponseSpec.requiredBody(): T =
    body(object : ParameterizedTypeReference<T>() {}) ?: throw NoSuchElementException("Response body is required")

/**
 * Extension for [RestClient.ResponseSpec.toEntity] providing a `toEntity<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
inline fun <reified T : Any> RestClient.ResponseSpec.toEntity(): ResponseEntity<T> =
	toEntity(object : ParameterizedTypeReference<T>() {})