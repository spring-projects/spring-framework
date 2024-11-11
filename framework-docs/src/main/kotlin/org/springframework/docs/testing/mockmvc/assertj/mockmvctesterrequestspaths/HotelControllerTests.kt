/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.docs.testing.mockmvc.assertj.mockmvctesterrequestspaths

import org.assertj.core.api.Assertions.assertThat
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder

class HotelControllerTests {

	private val mockMvc = MockMvcTester.of(HotelController())

	fun contextAndServletPaths() {
		// tag::context-servlet-paths[]
		assertThat(mockMvc.get().uri("/app/main/hotels/{id}", 42)
				.contextPath("/app").servletPath("/main"))
			. // ...
			// end::context-servlet-paths[]
			hasStatusOk()
	}

	fun configureMockMvcTesterWithDefaultSettings() {
		// tag::default-customizations[]
		val mockMvc =
			MockMvcTester.of(listOf(HotelController())) { builder: StandaloneMockMvcBuilder ->
				builder.defaultRequest<StandaloneMockMvcBuilder>(
					MockMvcRequestBuilders.get("/")
						.contextPath("/app").servletPath("/main")
						.accept(MediaType.APPLICATION_JSON)
				).build()
			}
		// end::default-customizations[]
		mockMvc.toString() // avoid  warning
	}


	class HotelController
}