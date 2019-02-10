/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.function.server

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.reactivestreams.Publisher
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType.*

/**
 * Mock object based tests for [ServerResponse] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
class ServerResponseExtensionsTests {

	val bodyBuilder = mockk<ServerResponse.BodyBuilder>(relaxed = true)


	@Test
	fun `BodyBuilder#body with Publisher and reified type parameters`() {
		val body = mockk<Publisher<List<Foo>>>()
		bodyBuilder.body(body)
		verify { bodyBuilder.body(body, object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `BodyBuilder#bodyToServerSentEvents with Publisher and reified type parameters`() {
		val body = mockk<Publisher<List<Foo>>>()
		bodyBuilder.bodyToServerSentEvents(body)
		verify { bodyBuilder.contentType(TEXT_EVENT_STREAM) }
	}

	@Test
	fun `BodyBuilder#json`() {
		bodyBuilder.json()
		verify { bodyBuilder.contentType(APPLICATION_JSON_UTF8) }
	}

	@Test
	fun `BodyBuilder#xml`() {
		bodyBuilder.xml()
		verify { bodyBuilder.contentType(APPLICATION_XML) }
	}

	@Test
	fun `BodyBuilder#html`() {
		bodyBuilder.html()
		verify { bodyBuilder.contentType(TEXT_HTML) }
	}

	class Foo
}
