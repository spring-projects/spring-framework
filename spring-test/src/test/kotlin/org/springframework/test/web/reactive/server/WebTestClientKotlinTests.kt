/*
 * Copyright 2002-present the original author or authors.
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
