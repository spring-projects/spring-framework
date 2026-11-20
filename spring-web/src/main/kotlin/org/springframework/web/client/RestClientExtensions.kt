/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.client

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.ResponseEntity
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.typeOf

/**
 * Extension for [RestClient.RequestBodySpec.body] providing a `bodyWithType<Foo>(...)` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * It also provides a [KType] hint for [org.springframework.http.converter.SmartHttpMessageConverter]s
 * supporting them.
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
inline fun <reified T : Any> RestClient.RequestBodySpec.bodyWithType(body: T): RestClient.RequestBodySpec =
	hint(KType::class.jvmName, typeOf<T>()).body(body, object : ParameterizedTypeReference<T>() {})


/**
 * Extension for [RestClient.ResponseSpec.body] providing a `body<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * It also provides a [KType] hint for [org.springframework.http.converter.SmartHttpMessageConverter]s
 * supporting them.
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
inline fun <reified T : Any> RestClient.ResponseSpec.body(): T? =
	hint(KType::class.jvmName, typeOf<T>()).body(object : ParameterizedTypeReference<T>() {})

/**
 * Extension for [RestClient.ResponseSpec.body] providing a `requiredBody<Foo>()` variant with a non-nullable
 * return value.
 *
 * It also provides a [KType] hint for [org.springframework.http.converter.SmartHttpMessageConverter]s
 * supporting them.
 *
 * @throws NoSuchElementException if there is no response body
 * @since 6.2
 */
inline fun <reified T : Any> RestClient.ResponseSpec.requiredBody(): T =
	hint(KType::class.jvmName, typeOf<T>()).body(object : ParameterizedTypeReference<T>() {}) ?:
	throw NoSuchElementException("Response body is required")

/**
 * Extension for [RestClient.ResponseSpec.toEntity] providing a `toEntity<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * It also provides a [KType] hint for [org.springframework.http.converter.SmartHttpMessageConverter]s
 * supporting them.
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
inline fun <reified T : Any> RestClient.ResponseSpec.toEntity(): ResponseEntity<T> =
	hint(KType::class.jvmName, typeOf<T>()).toEntity(object : ParameterizedTypeReference<T>() {})