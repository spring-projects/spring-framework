/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.web.servlet

import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.test.util.AssertionErrors
import org.springframework.test.web.Person
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

import org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup

/**
 * MockMvcDsl tests that verify builder, actions, and expect blocks
 *
 * @author Clint Checketts
 * @author Petr Balat
 */
class MockMvcDslTests {
	private lateinit var mockMvc: MockMvc

	@Before
	fun setup() {
		mockMvc = standaloneSetup(PersonController()).build()
	}

	@Test
	fun json() {
		mockMvc.perform(HttpMethod.GET,"/person/{name}", "Lee") {
			builder { accept(MediaType.APPLICATION_JSON) }
			print()
			expect {
				status { isOk }
				content { contentType("application/json;charset=UTF-8") }
				jsonPath("$.name") { value("Lee") }
				json("""{"someBoolean": false}""", strict = false)
			}
		}
	}

	@Test
	fun `negative assertion tests to verify the matchers throw errors when expected`() {
		val name = "Petr"
		mockMvc.perform(HttpMethod.GET,"/person/$name") {
			builder { accept(MediaType.APPLICATION_JSON) }
			print()
			expect {
				assertThrows<AssertionError> { content { contentType(MediaType.APPLICATION_ATOM_XML) } }
				assertThrows<AssertionError> { contentString("Wrong") }
				assertThrows<AssertionError> { jsonPath("name", CoreMatchers.`is`("Wrong")) }
				assertThrows<AssertionError> { json("""{"name":"wrong"}""") }
				assertThrows<AssertionError> { jsonPath("name") { value("wrong") } }
				assertThrows<AssertionError> { cookie { value("name", "wrong") } }
				assertThrows<AssertionError> { flash { attribute<String>("name", "wrong") } }
				assertThrows<AssertionError> { header { stringValues("name", "wrong") } }
				assertThrows<AssertionError> { model { attributeExists("name", "wrong") } }
				assertThrows<AssertionError> { model<String>("name") { Assert.assertThat(this, CoreMatchers.`is`("wrong")) } }
				assertThrows<AssertionError> { redirectedUrl("wrong/Url") }
				assertThrows<AssertionError> { redirectedUrlPattern("wrong/Url") }
				assertThrows<AssertionError> { redirectedUrlPattern("wrong/Url") }
				assertThrows<AssertionError> { status { isAccepted } }
				assertThrows<AssertionError> { viewName("wrongName") }
				assertThrows<AssertionError> { HttpStatus.ACCEPTED.isStatus() }
				assertThrows<AssertionError> { "$.name" jsonPathIs "wrong" }
				assertThrows<AssertionError> { "$.name" jsonPathMatcher CoreMatchers.`is`("wrong") }
				assertThrows<AssertionError> { "name" jsonPath { value("wrong") } }
				assertThrows<AssertionError> { jsonPath("name") { value("wrong") } }
				assertThrows<AssertionError> { +HandlerMethod("helloJsonWrong") }
			}
		}
	}

	@Test
	fun `negative assertion tests for xpath`() {
		mockMvc.perform(HttpMethod.GET,"/person/Clint") {
			builder { accept(MediaType.APPLICATION_XML) }

			andDo(MockMvcResultHandlers.print())
			andExpect(status().isOk)

			print()
			expect {
				assertThrows<AssertionError> { xpath("//wrong") { nodeCount(1) } }
			}
		}
	}

	@Test
	fun `demonstrate actions block for raw ResultAction calls as MvcResult is returned by default`() {
		val result = mockMvc.perform(HttpMethod.GET,"/person/{name}", "Lee") {
			builder { accept(MediaType.APPLICATION_JSON) }
			actions {
				andExpect(status().isOk)
			}
			expect{ status { isOk } }
		}

		assertNotNull(result)
	}


	@Controller
	private inner class PersonController {

		@RequestMapping("/person/{name}")
		@ResponseBody
		operator fun get(@PathVariable name: String): Person {
			return Person(name)
		}
	}

	/** Silly example matcher to demonstrate how to add custom matchers that aren't in the DSL using unary operator */
	class HandlerMethod(private val name: String) : ResultMatcher {

		override fun match(result: MvcResult) {
			val handler = result.handler
			if (handler is org.springframework.web.method.HandlerMethod) {
				AssertionErrors.assertEquals("Handler name", name, handler.method.name)
			}
		}
	}


}
