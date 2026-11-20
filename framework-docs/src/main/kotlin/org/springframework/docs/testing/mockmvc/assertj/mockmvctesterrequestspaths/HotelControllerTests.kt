/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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