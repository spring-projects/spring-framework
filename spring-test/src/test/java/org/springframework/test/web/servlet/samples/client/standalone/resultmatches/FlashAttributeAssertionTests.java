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

import java.net.URL;

import org.junit.jupiter.api.Test;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.resultmatchers.FlashAttributeAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class FlashAttributeAssertionTests {

	private final WebTestClient client =
			MockMvcWebTestClient.bindToController(new PersonController())
					.alwaysExpect(status().isFound())
					.alwaysExpect(flash().attributeCount(3))
					.build();


	@Test
	void attributeCountWithWrongCount() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> performRequest().andExpect(flash().attributeCount(1)))
			.withMessage("FlashMap size expected:<1> but was:<3>");
	}

	@Test
	void attributeExists() throws Exception {
		performRequest().andExpect(flash().attributeExists("one", "two", "three"));
	}

	@Test
	void attributeEqualTo() throws Exception {
		performRequest()
				.andExpect(flash().attribute("one", "1"))
				.andExpect(flash().attribute("two", 2.222))
				.andExpect(flash().attribute("three", new URL("https://example.com")));
	}

	@Test
	void attributeMatchers() throws Exception {
		performRequest()
				.andExpect(flash().attribute("one", containsString("1")))
				.andExpect(flash().attribute("two", closeTo(2, 0.5)))
				.andExpect(flash().attribute("three", notNullValue()))
				.andExpect(flash().attribute("one", equalTo("1")))
				.andExpect(flash().attribute("two", equalTo(2.222)))
				.andExpect(flash().attribute("three", equalTo(new URL("https://example.com"))));
	}

	private ResultActions performRequest() {
		EntityExchangeResult<Void> result = client.post().uri("/persons").exchange().expectBody().isEmpty();
		return MockMvcWebTestClient.resultActionsFor(result);
	}


	@Controller
	private static class PersonController {

		@PostMapping("/persons")
		String save(RedirectAttributes redirectAttrs) throws Exception {
			redirectAttrs.addFlashAttribute("one", "1");
			redirectAttrs.addFlashAttribute("two", 2.222);
			redirectAttrs.addFlashAttribute("three", new URL("https://example.com"));
			return "redirect:/person/1";
		}
	}

}
