/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.samples.client.standalone.resultmatches;

import org.junit.jupiter.api.Test;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.resultmatchers.UrlAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
class ViewNameAssertionTests {

	private final WebTestClient client =
			MockMvcWebTestClient.bindToController(new SimpleController())
					.alwaysExpect(status().isOk())
					.build();


	@Test
	void hamcrestEqualTo() throws Exception {
		MockMvcWebTestClient.resultActionsFor(performRequest())
			.andExpect(view().name("mySpecialView"))
			.andExpect(view().name(equalTo("mySpecialView")));
	}

	@Test
	void hamcrestMatcher() throws Exception {
		MockMvcWebTestClient.resultActionsFor(performRequest())
				.andExpect(view().name(containsString("Special")));
	}

	private EntityExchangeResult<Void> performRequest() {
		return client.get().uri("/").exchange().expectBody().isEmpty();
	}


	@Controller
	private static class SimpleController {

		@RequestMapping("/")
		public String handle() {
			return "mySpecialView";
		}
	}
}
