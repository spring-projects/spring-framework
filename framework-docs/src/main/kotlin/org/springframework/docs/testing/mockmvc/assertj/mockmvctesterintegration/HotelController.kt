/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.testing.mockmvc.assertj.mockmvctesterintegration

import org.assertj.core.api.Assertions.*
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 *
 * @author Stephane Nicoll
 */
class HotelController {

	private val mockMvc = MockMvcTester.of(HotelController())


	fun perform() {
		// tag::perform[]
		// Static import on MockMvcRequestBuilders.get
		assertThat(mockMvc.perform(get("/hotels/{id}",42)))
			.hasStatusOk()
		// end::perform[]
	}

	fun performWithCustomMatcher() {
		// tag::perform[]
		// Static import on MockMvcResultMatchers.status
		assertThat(mockMvc.get().uri("/hotels/{id}", 42))
			.matches(status().isOk())
		// end::perform[]
	}

	class HotelController
}