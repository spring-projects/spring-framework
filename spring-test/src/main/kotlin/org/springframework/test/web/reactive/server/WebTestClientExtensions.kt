/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.reactive.server

import org.reactivestreams.Publisher
import org.springframework.test.web.reactive.server.WebTestClient.*

/**
 * Extension for [RequestBodySpec.body] providing a variant without explicit class
 * parameter thanks to Kotlin reified type parameters.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any, S : Publisher<T>> RequestBodySpec.body(publisher: S): RequestHeadersSpec<*>
		= body(publisher, T::class.java)

/**
 * Extension for [ResponseSpec.expectBody] providing a `expectBody<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
inline fun <reified B : Any> ResponseSpec.expectBody(): BodySpec<B, *> =
		expectBody(B::class.java)

/**
 * Extension for [ResponseSpec.expectBodyList] providing a `expectBodyList<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified E : Any> ResponseSpec.expectBodyList(): ListBodySpec<E> =
		expectBodyList(E::class.java)

/**
 * Extension for [ResponseSpec.returnResult] providing a `returnResult<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
inline fun <reified T : Any> ResponseSpec.returnResult(): FluxExchangeResult<T> =
		returnResult(T::class.java)
