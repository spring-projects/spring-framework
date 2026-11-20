/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.testing.mockmvc.assertj.mockmvctesterintegration;

import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class HotelControllerTests {

	private final MockMvcTester mockMvc = MockMvcTester.of(new HotelController());


	void perform() {
		// tag::perform[]
		// Static import on MockMvcRequestBuilders.get
		assertThat(mockMvc.perform(get("/hotels/{id}", 42)))
				.hasStatusOk();
		// end::perform[]
	}

	void performWithCustomMatcher() {
		// tag::matches[]
		// Static import on MockMvcResultMatchers.status
		assertThat(mockMvc.get().uri("/hotels/{id}", 42))
				.matches(status().isOk());
		// end::matches[]
	}

	static class HotelController {}
}
