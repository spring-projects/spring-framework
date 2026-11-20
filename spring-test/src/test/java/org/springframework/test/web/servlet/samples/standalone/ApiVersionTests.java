/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.samples.standalone;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.Person;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.accept.DefaultApiVersionStrategy;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.ApiVersionInserter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Tests demonstrating the use of API version.
 * @author Rossen Stoyanchev
 */
class ApiVersionTests {

	@Test
	void queryParameter() throws Exception {

		String header = "API-Version";

		DefaultApiVersionStrategy versionStrategy = new DefaultApiVersionStrategy(
				List.of(request -> request.getHeader(header)), new SemanticApiVersionParser(),
				true, null, true, null, null);

		MockMvc mockMvc = standaloneSetup(new PersonController())
				.setApiVersionStrategy(versionStrategy)
				.apiVersionInserter(ApiVersionInserter.useHeader(header))
				.build();

		mockMvc.perform(get("/search?name=George").accept(MediaType.APPLICATION_JSON).apiVersion(1.1))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$.name").value("George"));
	}


	@Controller
	private static class PersonController {

		@RequestMapping(path="/search", version = "1.1")
		@ResponseBody
		public Person get(@RequestParam String name) {
			return new Person(name);
		}
	}

}
