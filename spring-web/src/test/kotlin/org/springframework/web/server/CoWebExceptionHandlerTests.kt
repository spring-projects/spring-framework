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
