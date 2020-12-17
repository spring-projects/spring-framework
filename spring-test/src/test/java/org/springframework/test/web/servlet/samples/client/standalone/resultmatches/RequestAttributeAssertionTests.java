/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.web.servlet.samples.client.standalone.resultmatches;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.HandlerMapping;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

/**
 * MockMvcWebTestClient equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.resultmatchers.RequestAttributeAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestAttributeAssertionTests {

	private final WebTestClient mainServletClient =
			MockMvcWebTestClient.bindToController(new SimpleController())
					.defaultRequest(get("/").servletPath("/main"))
					.build();

	private final WebTestClient client =
			MockMvcWebTestClient.bindToController(new SimpleController()).build();


	@Test
	void requestAttributeEqualTo() throws Exception {
		performRequest(mainServletClient, "/main/1")
			.andExpect(request().attribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/{id}"))
			.andExpect(request().attribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/1"));
	}

	@Test
	void requestAttributeMatcher() throws Exception {
		String producibleMediaTypes = HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;

		performRequest(client, "/1")
			.andExpect(request().attribute(producibleMediaTypes, hasItem(MediaType.APPLICATION_JSON)))
			.andExpect(request().attribute(producibleMediaTypes, not(hasItem(MediaType.APPLICATION_XML))));

		performRequest(mainServletClient, "/main/1")
			.andExpect(request().attribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, equalTo("/{id}")))
			.andExpect(request().attribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, equalTo("/1")));
	}

	private ResultActions performRequest(WebTestClient client, String uri) {
		EntityExchangeResult<Void> result = client.get().uri(uri)
				.exchange()
				.expectStatus().isOk()
				.expectBody().isEmpty();

		return MockMvcWebTestClient.resultActionsFor(result);
	}


	@Controller
	private static class SimpleController {

		@GetMapping(path="/{id}", produces="application/json")
		String show() {
			return "view";
		}
	}

}
