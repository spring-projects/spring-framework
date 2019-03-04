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

package org.springframework.test.web.servlet

import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.*
import org.springframework.test.web.Person
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.*
import java.security.Principal
import java.util.*

/**
 * [MockMvc] DSL tests that verifies builder, actions and expect blocks.
 *
 * @author Clint Checketts
 * @author Petr Balat
 * @author Sebastien Deleuze
 */
class MockMvcExtensionsTests {

	private val mockMvc = MockMvcBuilders.standaloneSetup(PersonController()).build()

	@Test
	fun request() {
		mockMvc.request(HttpMethod.GET, "/person/{name}", "Lee") {
			secure = true
			accept = APPLICATION_JSON
			headers {
				contentLanguage = Locale.FRANCE
			}
			principal = Principal { "foo" }
		}.andExpect {
			status { isOk }
			content { contentType(APPLICATION_JSON_UTF8) }
			jsonPath("$.name") { value("Lee") }
			content { json("""{"someBoolean": false}""", false) }
		}.andDo {
			print()
		}
	}

	@Test
	fun `request without MockHttpServletRequestDsl`() {
		mockMvc.request(HttpMethod.GET, "/person/{name}", "Lee").andExpect {
			status { isOk }
		}.andDo {
			print()
		}
	}

	@Test
	fun `request with custom matcher and handler`() {
		var matcherInvoked = false
		var handlerInvoked = false
		val matcher = ResultMatcher { matcherInvoked = true }
		val handler = ResultHandler { handlerInvoked = true }
		mockMvc.request(HttpMethod.GET, "/person/{name}", "Lee").andExpect {
			status { isOk }
		}.andExpect {
			match(matcher)
		}.andDo {
			handle(handler)
		}
		Assert.assertTrue(matcherInvoked)
		Assert.assertTrue(handlerInvoked)
	}

	@Test
	fun get() {
		mockMvc.get("/person/{name}", "Lee") {
				secure = true
				accept = APPLICATION_JSON_UTF8
				headers {
					contentLanguage = Locale.FRANCE
				}
				principal = Principal { "foo" }
		}.andExpect {
			status { isOk }
			content { contentType(APPLICATION_JSON_UTF8) }
			jsonPath("$.name") { value("Lee") }
			content { json("""{"someBoolean": false}""", false) }
		}.andDo {
			print()
		}
	}

	@Test
	fun post() {
		mockMvc.post("/person") {
			content = """{ "name": "foo" }"""
			headers {
				accept = listOf(APPLICATION_JSON)
				contentType = APPLICATION_JSON
			}
		}.andExpect {
			status {
				isCreated
			}
		}
	}

	@Test
	fun `negative assertion tests to verify the matchers throw errors when expected`() {
		val name = "Petr"
		mockMvc.get("/person/$name") {
			accept = APPLICATION_JSON
		}.andExpect {
			assertThrows<AssertionError> { content { contentType(APPLICATION_ATOM_XML) } }
			assertThrows<AssertionError> { content { string("Wrong") } }
			assertThrows<AssertionError> { jsonPath("name", CoreMatchers.`is`("Wrong")) }
			assertThrows<AssertionError> { content { json("""{"name":"wrong"}""") } }
			assertThrows<AssertionError> { jsonPath("name") { value("wrong") } }
			assertThrows<AssertionError> { cookie { value("name", "wrong") } }
			assertThrows<AssertionError> { flash { attribute<String>("name", "wrong") } }
			assertThrows<AssertionError> { header { stringValues("name", "wrong") } }
			assertThrows<AssertionError> { model { attributeExists("name", "wrong") } }
			assertThrows<AssertionError> { redirectedUrl("wrong/Url") }
			assertThrows<AssertionError> { redirectedUrlPattern("wrong/Url") }
			assertThrows<AssertionError> { redirectedUrlPattern("wrong/Url") }
			assertThrows<AssertionError> { status { isAccepted } }
			assertThrows<AssertionError> { view { name("wrongName") } }
			assertThrows<AssertionError> { jsonPath("name") { value("wrong") } }
		}
	}

	@Test
	fun `negative assertion tests for xpath`() {
		mockMvc.get("/person/Clint") {
			accept = APPLICATION_XML
		}.andExpect {
			status { isOk }
			assertThrows<AssertionError> { xpath("//wrong") { nodeCount(1) } }
		}.andDo {
			print()
		}
	}


	@RestController
	private class PersonController {

		@GetMapping("/person/{name}")
		fun get(@PathVariable name: String): Person {
			return Person(name)
		}

		@Suppress("UNUSED_PARAMETER")
		@PostMapping("/person")
		@ResponseStatus(HttpStatus.CREATED)
		fun post(@RequestBody person: Person) {}
	}
}
