/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Enumeration;
import java.util.Map;

import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.Test;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A malformed request parameter is a client error, including in the observation.
 *
 * @author Lordwill Kandiro
 */
class InvalidRequestParameterObservationTests extends AbstractServletHandlerMethodTests {

	@Test
	void recordedStatusMatchesTheParameterParsingFailure() throws Exception {
		initDispatcherServlet(EchoController.class, true);
		TestObservationRegistry registry = TestObservationRegistry.create();
		ServerHttpObservationFilter filter = new ServerHttpObservationFilter(registry);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/echo") {

			@Override
			public String getParameter(String name) {
				throw failure();
			}

			@Override
			public String[] getParameterValues(String name) {
				throw failure();
			}

			@Override
			public Enumeration<String> getParameterNames() {
				throw failure();
			}

			@Override
			public Map<String, String[]> getParameterMap() {
				throw failure();
			}

			private IllegalStateException failure() {
				return new IllegalStateException("Invalid character found in the request target");
			}
		};
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, (req, res) -> getServlet().service(req, res));

		assertThat(response.getStatus()).isEqualTo(400);
		TestObservationRegistryAssert.assertThat(registry)
				.hasObservationWithNameEqualTo("http.server.requests")
				.that()
				.hasLowCardinalityKeyValue("status", "400")
				.hasLowCardinalityKeyValue("outcome", "CLIENT_ERROR")
				.hasBeenStopped();
	}


	@Controller
	private static class EchoController {

		@GetMapping("/echo")
		@ResponseBody
		String echo(@RequestParam String email) {
			return email;
		}
	}

}
