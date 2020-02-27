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

/**
 * Extension for [ServerResponse.BodyBuilder.body] providing a variant
 * leveraging Kotlin reified type parameters.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
inline fun <reified T: Any> ServerResponse.BodyBuilder.bodyWithType(body: T) =
		body(body, object : ParameterizedTypeReference<T>() {})
