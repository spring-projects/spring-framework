/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.servlet.function

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference

/**
 * Tests for WebMvc.fn [ServerResponse] extensions.
 *
 * @author Sebastien Deleuze
 */
class ServerResponseExtensionsTests {

	@Test
	fun bodyWithType() {
		val builder = mockk<ServerResponse.BodyBuilder>()
		val response = mockk<ServerResponse>()
		val body = listOf("foo", "bar")
		val typeReference = object: ParameterizedTypeReference<List<String>>() {}
		every { builder.body(body, typeReference) } returns response
		assertThat(builder.bodyWithType<List<String>>(body)).isEqualTo(response)
		verify { builder.body(body, typeReference) }
	}
}
