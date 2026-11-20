/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.client

import org.springframework.core.ParameterizedTypeReference
import org.springframework.test.web.servlet.client.RestTestClient.BodySpec
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec

/**
 * Extension for [ResponseSpec.expectBody] providing an `expectBody<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @author Stephane Nicoll
 * @since 7.0
 */
inline fun <reified B : Any> ResponseSpec.expectBody(): BodySpec<B, *> =
		expectBody(object : ParameterizedTypeReference<B>() {})

/**
 * Extension for [ResponseSpec.returnResult] providing a `returnResult<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 */
inline fun <reified T : Any> ResponseSpec.returnResult(): EntityExchangeResult<T> =
		returnResult(object : ParameterizedTypeReference<T>() {})
