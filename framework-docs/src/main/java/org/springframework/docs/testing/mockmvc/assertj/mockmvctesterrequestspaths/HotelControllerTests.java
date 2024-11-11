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

package org.springframework.docs.testing.mockmvc.assertj.mockmvctesterrequestspaths;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * @author Stephane Nicoll
 */
public class HotelControllerTests {

	private final MockMvcTester mockMvc = MockMvcTester.of(new HotelController());

	void contextAndServletPaths() {
		// tag::context-servlet-paths[]
		assertThat(mockMvc.get().uri("/app/main/hotels/{id}", 42)
				.contextPath("/app").servletPath("/main"))
				. // ...
				// end::context-servlet-paths[]
				hasStatusOk();
	}

	void configureMockMvcTesterWithDefaultSettings() {
		// tag::default-customizations[]
		MockMvcTester mockMvc = MockMvcTester.of(List.of(new HotelController()),
				builder -> builder.defaultRequest(get("/")
						.contextPath("/app").servletPath("/main")
						.accept(MediaType.APPLICATION_JSON)).build());
		// end::default-customizations[]
	}


	static class HotelController {}
}
