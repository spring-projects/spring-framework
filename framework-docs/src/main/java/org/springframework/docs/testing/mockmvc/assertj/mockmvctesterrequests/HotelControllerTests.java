/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.testing.mockmvc.assertj.mockmvctesterrequests;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public class HotelControllerTests {

	private final MockMvcTester mockMvc = MockMvcTester.of(new HotelController());


	void createHotel() {
		// tag::post[]
		assertThat(mockMvc.post().uri("/hotels/{id}", 42).accept(MediaType.APPLICATION_JSON))
				. // ...
				// end::post[]
				hasStatusOk();
	}

	void createHotelMultipleAssertions() {
		// tag::post-exchange[]
		MvcTestResult result = mockMvc.post().uri("/hotels/{id}", 42)
				.accept(MediaType.APPLICATION_JSON).exchange();
		assertThat(result). // ...
				// end::post-exchange[]
				hasStatusOk();
	}

	void queryParameters() {
		// tag::query-parameters[]
		assertThat(mockMvc.get().uri("/hotels?thing={thing}", "somewhere"))
				. // ...
				// end::query-parameters[]
				hasStatusOk();
	}

	void parameters() {
		// tag::parameters[]
		assertThat(mockMvc.get().uri("/hotels").param("thing", "somewhere"))
				. // ...
				// end::parameters[]
				hasStatusOk();
	}

	static class HotelController {}
}
