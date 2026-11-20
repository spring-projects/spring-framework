/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
