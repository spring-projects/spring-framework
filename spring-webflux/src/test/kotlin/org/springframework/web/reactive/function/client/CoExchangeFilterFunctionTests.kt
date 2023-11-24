/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.reactive.function.client

import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Test for [CoExchangeFilterFunction].
 *
 * @author Sebastien Deleuze
 */
class CoExchangeFilterFunctionTests {

	@Test
	fun exchange() {
		val response = mockk<ClientResponse>()
		val exchangeFunction = MyCoExchangeFunction(response)
		runBlocking {
			assertThat(exchangeFunction.exchange(mockk())).isEqualTo(response)
		}
	}
}

private class MyCoExchangeFunction(private val response: ClientResponse) : CoExchangeFunction {

	override suspend fun exchange(request: ClientRequest): ClientResponse {
		delay(1)
		return response
	}
}
