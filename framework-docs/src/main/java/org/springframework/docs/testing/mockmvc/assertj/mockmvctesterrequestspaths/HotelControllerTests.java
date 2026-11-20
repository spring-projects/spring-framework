/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
