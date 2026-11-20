/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.testing.mockmvc.assertj.mockmvctesterassertions;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

public class HotelControllerTests {

	private final MockMvcTester mockMvc = MockMvcTester.of(new HotelController());


	void getHotel() {
		// tag::get[]
		assertThat(mockMvc.get().uri("/hotels/{id}", 42))
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson().isLenientlyEqualTo("sample/hotel-42.json");
		// end::get[]
	}


	void getHotelInvalid() {
		// tag::failure[]
		assertThat(mockMvc.get().uri("/hotels/{id}", -1))
				.hasFailed()
				.hasStatus(HttpStatus.BAD_REQUEST)
				.failure().hasMessageContaining("Identifier should be positive");
		// end::failure[]
	}

	static class HotelController {}
}
