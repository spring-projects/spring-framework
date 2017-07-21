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

package org.springframework.web.reactive.function

import org.reactivestreams.Publisher
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.http.server.reactive.ServerHttpResponse

/**
 * Function for providing a `bodyFromPublisher(publisher)` alternative to `BodyInserters.fromPublisher(publisher, Foo::class.java)`.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Publisher<S>, reified S : Any> bodyFromPublisher(publisher: T): BodyInserter<T, ReactiveHttpOutputMessage> =
		BodyInserters.fromPublisher(publisher, S::class.java)

/**
 * Function for providing a `bodyFromServerSentEvents(publisher)` alternative to `BodyInserters.fromServerSentEvents(publisher, Foo::class.java)`.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Publisher<S>, reified S : Any> bodyFromServerSentEvents(publisher: T): BodyInserter<T, ServerHttpResponse> =
		BodyInserters.fromServerSentEvents(publisher, S::class.java)
