/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
inline fun <reified T : Any> DatabaseClient.GenericExecuteSpec.bind(index: Int, value: T?) = bind(index, if (value != null) Parameters.`in`(value) else Parameters.`in`(T::class.java))

/**
 * Extension for [DatabaseClient.GenericExecuteSpec.bind] providing a variant leveraging reified type parameters
 *
 * @author Mark Paluch
 * @author Ibanga Enoobong Ime
 */
inline fun <reified T : Any> DatabaseClient.GenericExecuteSpec.bind(name: String, value: T?) = bind(name, if (value != null) Parameters.`in`(value) else Parameters.`in`(T::class.java))
