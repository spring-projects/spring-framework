package org.springframework.test.web.reactive.server

import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

class WebTestClientKotlinTests {

	@Test
	fun expectBodyListKotlinSerialization() {
		val client = WebTestClient.bindToController(TestController::class.java)
			.configureClient()
			.codecs {
				it.registerDefaults(false)
				it.customCodecs().register(KotlinSerializationJsonDecoder())
			}.build()

		client.get().uri("/test")
			.accept(MediaType.APPLICATION_JSON)
			.exchangeSuccessfully()
			.expectBodyList<Response>()
			.hasSize(2)
			.contains(Response("Hello"), Response("World"))
	}

	@Serializable
	data class Response(val message: String)

	@RestController
	class TestController {
		@GetMapping("test")
		fun test(): List<Response> = listOf(Response("Hello"), Response("World"))
	}
}