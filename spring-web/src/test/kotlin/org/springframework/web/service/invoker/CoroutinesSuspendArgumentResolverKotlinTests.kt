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