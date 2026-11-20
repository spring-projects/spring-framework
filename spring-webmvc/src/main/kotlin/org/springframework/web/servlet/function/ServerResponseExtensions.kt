/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.function

import org.springframework.core.ParameterizedTypeReference

/**
 * Extension for [ServerResponse.BodyBuilder.body] providing a variant
 * leveraging Kotlin reified type parameters.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
inline fun <reified T: Any> ServerResponse.BodyBuilder.bodyWithType(body: T) =
		body(body, object : ParameterizedTypeReference<T>() {})
