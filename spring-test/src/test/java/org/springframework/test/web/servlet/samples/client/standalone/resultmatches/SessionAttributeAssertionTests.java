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

import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.resultmatchers.SessionAttributeAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class SessionAttributeAssertionTests {

	private final WebTestClient client =
			MockMvcWebTestClient.bindToController(new SimpleController())
					.alwaysExpect(status().isOk())
					.build();


	@Test
	void sessionAttributeEqualTo() throws Exception {
		performRequest().andExpect(request().sessionAttribute("locale", Locale.UK));

		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> performRequest().andExpect(request().sessionAttribute("locale", Locale.US)))
			.withMessage("Session attribute 'locale' expected:<en_US> but was:<en_GB>");
	}

	@Test
	void sessionAttributeMatcher() throws Exception {
		performRequest()
			.andExpect(request().sessionAttribute("bogus", is(nullValue())))
			.andExpect(request().sessionAttribute("locale", is(notNullValue())))
			.andExpect(request().sessionAttribute("locale", equalTo(Locale.UK)));

		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> performRequest().andExpect(request().sessionAttribute("bogus", is(notNullValue()))))
			.withMessageContaining("null");
	}

	@Test
	void sessionAttributeDoesNotExist() throws Exception {
		performRequest().andExpect(request().sessionAttributeDoesNotExist("bogus", "enigma"));

		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> performRequest().andExpect(request().sessionAttributeDoesNotExist("locale")))
			.withMessage("Session attribute 'locale' exists");
	}

	private ResultActions performRequest() {
		EntityExchangeResult<Void> result = client.post().uri("/").exchange().expectBody().isEmpty();
		return MockMvcWebTestClient.resultActionsFor(result);
	}


	@Controller
	@SessionAttributes("locale")
	private static class SimpleController {

		@ModelAttribute
		void populate(Model model) {
			model.addAttribute("locale", Locale.UK);
		}

		@RequestMapping("/")
		String handle() {
			return "view";
		}
	}

}
