/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core

import kotlinx.coroutines.reactor.awaitSingle

/**
 * Coroutines variant of [UpdatedRowsFetchSpec.rowsUpdated].
 *
 * @author Fred Montariol
 */
suspend fun UpdatedRowsFetchSpec.awaitRowsUpdated(): Long =
		rowsUpdated().awaitSingle()
