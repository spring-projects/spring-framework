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

package org.springframework.test.web.servlet.result

import io.mockk.mockk
import org.hamcrest.text.CharSequenceLength.*
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.*
import org.springframework.web.servlet.FlashMap
import org.springframework.web.servlet.ModelAndView
import java.nio.charset.StandardCharsets

/**
 * Tests for [ContentResultMatchersDsl].
 *
 * @author Dmitry Sulman
 */
class ContentResultMatchersDslTests {

	val mockMvc = mockk<MockMvc>()

	@Test
	fun `ContentResultMatchersDsl#string accepts Matcher parameterized with String supertype`() {
		getStubResultActionsDsl("some string")
			.andExpect { content { string(hasLength(11)) } }
	}

	private fun getStubResultActionsDsl(content: String): ResultActionsDsl {
		val resultActions = object : ResultActions {
			override fun andExpect(matcher: ResultMatcher): ResultActions {
				matcher.match(getStubMvcResult(content))
				return this
			}

			override fun andDo(handler: ResultHandler): ResultActions {
				throw UnsupportedOperationException()
			}

			override fun andReturn(): MvcResult {
				throw UnsupportedOperationException()
			}

		}
		return ResultActionsDsl(resultActions, mockMvc)
	}

	private fun getStubMvcResult(content: String): StubMvcResult {
		val response = MockHttpServletResponse()
		response.outputStream.write(content.toByteArray(StandardCharsets.UTF_8))
		return StubMvcResult(MockHttpServletRequest(), Any(), emptyArray(),  Exception(), ModelAndView(), FlashMap(), response)
	}
}
