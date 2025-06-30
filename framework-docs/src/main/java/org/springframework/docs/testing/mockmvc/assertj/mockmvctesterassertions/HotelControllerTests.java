/*
 * Copyright 2002-present the original author or authors.
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
