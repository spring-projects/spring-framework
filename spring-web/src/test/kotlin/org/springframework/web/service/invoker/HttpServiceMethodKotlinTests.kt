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
package org.springframework.web.service.invoker

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.service.annotation.GetExchange

import org.assertj.core.api.Assertions.assertThat

class HttpServiceMethodKotlinTests {

	private val clientAdapter = TestHttpClientAdapter()

	private val proxyFactory = HttpServiceProxyFactory.builder(clientAdapter).build()

	@Test
	fun suspendService() {
		val service = proxyFactory.createClient(SuspendService::class.java)

		runBlocking {

			service.execute()

			val headers = service.headers()
			assertThat(headers).isNotNull

			val body  = service.body()
			assertThat(body).isEqualTo("requestToBody")

			val voidEntity = service.voidEntity()
			assertThat(voidEntity.body).isNull()

			val entity = service.entity()
			assertThat(entity.body).isEqualTo("requestToEntity")
		}
	}


	private interface SuspendService {

		@GetExchange
		suspend fun execute()

		@GetExchange
		suspend fun headers(): HttpHeaders

		@GetExchange
		suspend fun body(): String?

		@GetExchange
		suspend fun voidEntity(): ResponseEntity<Void>

		@GetExchange
		suspend fun entity(): ResponseEntity<String>

	}

}