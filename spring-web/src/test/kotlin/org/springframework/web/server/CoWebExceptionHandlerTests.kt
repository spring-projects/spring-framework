/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.server

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest
import org.springframework.web.testfixture.server.MockServerWebExchange
import reactor.test.StepVerifier

class CoWebExceptionHandlerTest {

	@Test
	fun handle() {
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://example.com"))
		val ex = RuntimeException()

		val handler = MyCoWebExceptionHandler()
		val result = handler.handle(exchange, ex)

		StepVerifier.create(result).verifyComplete()

		assertThat(exchange.attributes["foo"]).isEqualTo("bar")
	}
}

private class MyCoWebExceptionHandler : CoWebExceptionHandler() {

	override suspend fun coHandle(exchange: ServerWebExchange, ex: Throwable) {
		exchange.attributes["foo"] = "bar"
	}
}
