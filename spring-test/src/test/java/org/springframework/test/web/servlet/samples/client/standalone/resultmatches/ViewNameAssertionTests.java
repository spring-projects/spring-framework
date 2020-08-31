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
public class ViewNameAssertionTests {

	private final WebTestClient client =
			MockMvcWebTestClient.bindToController(new SimpleController())
					.alwaysExpect(status().isOk())
					.build();


	@Test
	public void testEqualTo() throws Exception {
		MockMvcWebTestClient.resultActionsFor(performRequest())
			.andExpect(view().name("mySpecialView"))
			.andExpect(view().name(equalTo("mySpecialView")));
	}

	@Test
	public void testHamcrestMatcher() throws Exception {
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
