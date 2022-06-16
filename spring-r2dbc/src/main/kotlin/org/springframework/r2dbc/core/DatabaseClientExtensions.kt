/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.r2dbc.core

import io.r2dbc.spi.Parameters
import kotlinx.coroutines.reactor.awaitSingleOrNull

/**
 * Coroutines variant of [DatabaseClient.GenericExecuteSpec.then].
 *
 * @author Sebastien Deleuze
 */
@Suppress("DEPRECATION")
suspend fun DatabaseClient.GenericExecuteSpec.await() {
	then().awaitSingleOrNull()
}

/**
 * Extension for [DatabaseClient.GenericExecuteSpec.bind] providing a variant leveraging reified type parameters
 *
 * @author Mark Paluch
 * @author Ibanga Enoobong Ime
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
inline fun <reified T : Any> DatabaseClient.GenericExecuteSpec.bind(index: Int, value: T?) = bind(index, if (value != null) Parameters.`in`(value) else Parameters.`in`(T::class.java))

/**
 * Extension for [DatabaseClient.GenericExecuteSpec.bind] providing a variant leveraging reified type parameters
 *
 * @author Mark Paluch
 * @author Ibanga Enoobong Ime
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
inline fun <reified T : Any> DatabaseClient.GenericExecuteSpec.bind(name: String, value: T?) = bind(name, if (value != null) Parameters.`in`(value) else Parameters.`in`(T::class.java))
