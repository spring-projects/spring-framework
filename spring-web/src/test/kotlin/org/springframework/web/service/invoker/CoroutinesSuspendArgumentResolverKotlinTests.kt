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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.service.annotation.GetExchange

class CoroutinesSuspendArgumentResolverKotlinTests {

	private val client = TestHttpClientAdapter()

	private val service = HttpServiceProxyFactory.builder(client).build().createClient(Service::class.java)

	@Test
	fun continuation() {
		runBlocking {
			service.execute()
			assertThat(client.requestValues.continuation).isNotNull
		}
	}

	private interface Service {

		@GetExchange
		suspend fun execute()
	}

}