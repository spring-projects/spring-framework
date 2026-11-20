/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.observation;


import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServerRequestObservationContext}.
 */
class ServerRequestObservationContextTests {

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private MockHttpServletResponse response = new MockHttpServletResponse();

	private ServerRequestObservationContext context = new ServerRequestObservationContext(request, response);

	@Test
	void shouldGetMultipleHeaderValues() {
		request.addHeader("test", List.of("spring", "framework"));
		assertThat(context.getGetter().getAll(request, "test")).contains("spring", "framework");
	}

}
