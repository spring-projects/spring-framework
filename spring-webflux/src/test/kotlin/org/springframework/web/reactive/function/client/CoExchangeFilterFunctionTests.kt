/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.client

import io.mockk.mockk
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Test for [CoExchangeFilterFunction].
 *
 * @author Sebastien Deleuze
 */
class CoExchangeFilterFunctionTests {

	@Test
	suspend fun exchange() {
		val response = mockk<ClientResponse>()
		val exchangeFunction = MyCoExchangeFunction(response)
		assertThat(exchangeFunction.exchange(mockk())).isEqualTo(response)
	}
}

private class MyCoExchangeFunction(private val response: ClientResponse) : CoExchangeFunction {

	override suspend fun exchange(request: ClientRequest): ClientResponse {
		delay(1)
		return response
	}
}
