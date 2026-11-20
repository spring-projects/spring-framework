/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.testing.mockmvc.assertj.mockmvctesterrequests

import org.assertj.core.api.Assertions.assertThat
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.assertj.MockMvcTester

class HotelControllerTests {

	private val mockMvc = MockMvcTester.of(HotelController())

	fun createHotel() {
		// tag::post[]
		assertThat(mockMvc.post().uri("/hotels/{id}", 42).accept(MediaType.APPLICATION_JSON))
			. // ...
			// end::post[]
			hasStatusOk()
	}

	fun createHotelMultipleAssertions() {
		// tag::post-exchange[]
		val result = mockMvc.post().uri("/hotels/{id}", 42)
			.accept(MediaType.APPLICATION_JSON).exchange()
		assertThat(result)
			. // ...
			// end::post-exchange[]
			hasStatusOk()
	}

	fun queryParameters() {
		// tag::query-parameters[]
		assertThat(mockMvc.get().uri("/hotels?thing={thing}", "somewhere"))
			. // ...
			//end::query-parameters[]
			hasStatusOk()
	}

	fun parameters() {
		// tag::parameters[]
		assertThat(mockMvc.get().uri("/hotels").param("thing", "somewhere"))
			. // ...
			// end::parameters[]
			hasStatusOk()
	}

	class HotelController
}