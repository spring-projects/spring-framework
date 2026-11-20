/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.server

import kotlinx.coroutines.reactor.awaitSingle

/**
 * Coroutines variant of [RenderingResponse.Builder.build].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend fun RenderingResponse.Builder.buildAndAwait(): RenderingResponse = build().awaitSingle()