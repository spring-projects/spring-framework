/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class SharedHttpSessionTests {

	@Test
	void httpSession() throws Exception {
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
	void noHttpSession() throws Exception {
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
