/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.web.servlet

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_ATOM_XML
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_XML
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.test.web.Person
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.ModelAndView
import reactor.core.publisher.Mono
import java.security.Principal
import java.util.Locale

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
			status { isOk() }
			content { contentType(APPLICATION_JSON) }
			jsonPath("$.name") { value("Lee") }
			content { json("""{"someBoolean": false}""", false) }
		}.andDo {
			print()
		}
	}

	@Test
	fun `request without MockHttpServletRequestDsl`() {
		mockMvc.request(HttpMethod.GET, "/person/{name}", "Lee").andExpect {
			status { isOk() }
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
			status { isOk() }
		}.andExpect {
			match(matcher)
		}.andDo {
			handle(handler)
			handle {
				matcherInvoked = true
			}
		}
		assertThat(matcherInvoked).isTrue()
		assertThat(handlerInvoked).isTrue()
	}

	@Test
	fun `request with two custom matchers and matchAll`() {
		var matcher1Invoked = false
		var matcher2Invoked = false
		val matcher1 = ResultMatcher { matcher1Invoked = true; throw AssertionError("expected") }
		val matcher2 = ResultMatcher { matcher2Invoked = true }
		assertThatExceptionOfType(AssertionError::class.java).isThrownBy {
			mockMvc.request(HttpMethod.GET, "/person/{name}", "Lee")
					.andExpect {
						matchAll(matcher1, matcher2)
					}
		}
				.withMessage("expected")

		assertThat(matcher1Invoked).describedAs("matcher1").isTrue()
		assertThat(matcher2Invoked).describedAs("matcher2").isTrue()
	}

	@Test
	fun get() {
		mockMvc.get("/person/{name}", "Lee") {
				secure = true
				accept = APPLICATION_JSON
				headers {
					contentLanguage = Locale.FRANCE
				}
				principal = Principal { "foo" }
		}.andExpect {
			status { isOk() }
			content { contentType(APPLICATION_JSON) }
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
				isCreated()
			}
		}
	}

	@Test
	fun `negative assertion tests to verify the matchers throw errors when expected`() {
		val name = "Petr"
		mockMvc.get("/person/$name") {
			accept = APPLICATION_JSON
		}.andExpect {
			assertThatExceptionOfType(AssertionError::class.java).isThrownBy { content { contentType(APPLICATION_ATOM_XML) } }
			assertThatExceptionOfType(AssertionError::class.java).isThrownBy { content { string("Wrong") } }
			assertThatExceptionOfType(AssertionError::class.java).isThrownBy { jsonPath("name", CoreMatchers.`is`("Wrong")) }
			assertThatExceptionOfType(AssertionError::class.java).isThrownBy { content { json("""{"name":"wrong"}""") } }
			assertThatExceptionOfType(AssertionError::class.java).isThrownBy { jsonPath("name") { value("wrong") } }
			assertThatExceptionOfType(AssertionError::class.java).isThrownBy { cookie { value("name", "wrong") } }
			assertThatExceptionOfType(AssertionError::class.java).isThrownBy { flash { attribute("name", "wrong") } }
			assertThatExceptionOfType(AssertionError::class.java).isThrownBy { header { stringValues("name", "wrong") } }
			assertThatExceptionOfType(AssertionError::class.java).isThrownBy { model { attributeExists("name", "wrong") } }
			assertThatExceptionOfType(AssertionError::class.java).isThrownBy { redirectedUrl("wrong/Url") }
			assertThatExceptionOfType(AssertionError::class.java).isThrownBy { redirectedUrlPattern("wrong/Url") }
			assertThatExceptionOfType(AssertionError::class.java).isThrownBy { status { isAccepted() } }
			assertThatExceptionOfType(AssertionError::class.java).isThrownBy { view { name("wrongName") } }
			assertThatExceptionOfType(AssertionError::class.java).isThrownBy { jsonPath("name") { value("wrong") } }
		}
	}

	@Test
	fun `negative assertion tests for xpath`() {
		mockMvc.get("/person/Clint") {
			accept = APPLICATION_XML
		}.andExpect {
			status { isOk() }
			assertThatExceptionOfType(AssertionError::class.java).isThrownBy { xpath("//wrong") { nodeCount(1) } }
		}.andDo {
			print()
		}
	}

	@Test
	fun asyncDispatch() {
		mockMvc.get("/async").asyncDispatch().andExpect {
			status { isOk() }
		}
	}

	@Test
	fun modelAndView() {
		mockMvc.get("/").andExpect {
			model {
				assertThatExceptionOfType(AssertionError::class.java).isThrownBy { attribute("foo", "bar") }
				attribute("foo", "foo")
			}
		}
	}

	@Test
	fun `andExpectAll reports multiple assertion errors`() {
		assertThatCode {
			mockMvc.request(HttpMethod.GET, "/person/{name}", "Lee") {
				accept = APPLICATION_JSON
			}.andExpectAll {
				status { is4xxClientError() }
				content { contentType(TEXT_PLAIN) }
				jsonPath("$.name") { value("Lee") }
			}
		}
				.hasMessage("Multiple Exceptions (2):\n" +
						"Range for response status value 200 expected:<CLIENT_ERROR> but was:<SUCCESSFUL>\n" +
						"Content type expected:<text/plain> but was:<application/json>")
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

		@GetMapping("/async")
		fun getAsync(): Mono<Person> {
			return Mono.just(Person("foo"))
		}

		@GetMapping("/")
		fun index()  = ModelAndView("index", mapOf("foo" to "foo", "bar" to "bar"))
	}
}
