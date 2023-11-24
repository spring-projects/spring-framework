/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.web.servlet.setup;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.SharedHttpSessionConfigurer.sharedHttpSession;

/**
 * Tests for {@link SharedHttpSessionConfigurer}.
 *
 * @author Rossen Stoyanchev
 */
public class SharedHttpSessionTests {

	@Test
	public void httpSession() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
				.apply(sharedHttpSession())
				.build();

		String url = "/session";

		MvcResult result = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
		HttpSession session = result.getRequest().getSession(false);
		assertThat(session).isNotNull();
		assertThat(session.getAttribute("counter")).isEqualTo(1);

		result = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
		session = result.getRequest().getSession(false);
		assertThat(session).isNotNull();
		assertThat(session.getAttribute("counter")).isEqualTo(2);

		result = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
		session = result.getRequest().getSession(false);
		assertThat(session).isNotNull();
		assertThat(session.getAttribute("counter")).isEqualTo(3);
	}

	@Test
	public void noHttpSession() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
				.apply(sharedHttpSession())
				.build();

		String url = "/no-session";

		MvcResult result = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
		HttpSession session = result.getRequest().getSession(false);
		assertThat(session).isNull();

		result = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
		session = result.getRequest().getSession(false);
		assertThat(session).isNull();

		url = "/session";

		result = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
		session = result.getRequest().getSession(false);
		assertThat(session).isNotNull();
		assertThat(session.getAttribute("counter")).isEqualTo(1);
	}


	@Controller
	private static class TestController {

		@GetMapping("/session")
		public String handle(HttpSession session) {
			Integer counter = (Integer) session.getAttribute("counter");
			session.setAttribute("counter", (counter != null ? counter + 1 : 1));
			return "view";
		}

		@GetMapping("/no-session")
		public String handle() {
			return "view";
		}
	}

}
