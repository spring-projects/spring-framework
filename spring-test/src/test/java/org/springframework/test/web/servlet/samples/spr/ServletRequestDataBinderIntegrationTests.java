/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.web.servlet.samples.spr;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringJUnitWebConfig(ServletRequestDataBinderIntegrationTests.SpringWebKeyValueController.class)
class ServletRequestDataBinderIntegrationTests {

	@Test // gh-34043
	void postMap(WebApplicationContext wac) throws Exception {
		MockMvc mockMvc = webAppContextSetup(wac).build();
		mockMvc.perform(post("/map")
						.param("someMap[a]", "valueA")
						.param("someMap[b]", "valueB"))
				.andExpect(status().isOk())
				.andExpect(content().string("valueB"));
	}

	@Test
	void postArray(WebApplicationContext wac) throws Exception {
		MockMvc mockMvc = webAppContextSetup(wac).build();
		mockMvc.perform(post("/array")
						.param("someArray[0]", "valueA")
						.param("someArray[1]", "valueB"))
				.andExpect(status().isOk())
				.andExpect(content().string("valueB"));
	}

	@Test // gh-34121
	void postArrayWithEmptyIndex(WebApplicationContext wac) throws Exception {
		MockMvc mockMvc = webAppContextSetup(wac).build();
		mockMvc.perform(post("/array")
						.param("someArray[]", "valueA")
						.param("someArray[]", "valueB"))
				.andExpect(status().isOk())
				.andExpect(content().string("valueB"));
	}

	@Test
	void postArrayWithoutIndex(WebApplicationContext wac) throws Exception {
		MockMvc mockMvc = webAppContextSetup(wac).build();
		mockMvc.perform(post("/array")
						.param("someArray", "valueA")
						.param("someArray", "valueB"))
				.andExpect(status().isOk())
				.andExpect(content().string("valueB"));
	}

	@Test
	void postList(WebApplicationContext wac) throws Exception {
		MockMvc mockMvc = webAppContextSetup(wac).build();
		mockMvc.perform(post("/list")
						.param("someList[0]", "valueA")
						.param("someList[1]", "valueB"))
				.andExpect(status().isOk())
				.andExpect(content().string("valueB"));
	}

	@Test // gh-34121
	void postListWithEmptyIndex(WebApplicationContext wac) throws Exception {
		MockMvc mockMvc = webAppContextSetup(wac).build();
		mockMvc.perform(post("/list")
						.param("someList[]", "valueA")
						.param("someList[]", "valueB"))
				.andExpect(status().isOk())
				.andExpect(content().string("valueB"));
	}

	@Test
	void postListWithoutIndex(WebApplicationContext wac) throws Exception {
		MockMvc mockMvc = webAppContextSetup(wac).build();
		mockMvc.perform(post("/list")
						.param("someList", "valueA")
						.param("someList", "valueB"))
				.andExpect(status().isOk())
				.andExpect(content().string("valueB"));
	}

	record PayloadWithMap(Map<String, String> someMap) {}

	record PayloadWithArray(String[] someArray) {}

	record PayloadWithList(List<String> someList) {}

	@RestController
	@SuppressWarnings("unused")
	static class SpringWebKeyValueController {

		@PostMapping("/map")
		String postMap(@ModelAttribute("payload") PayloadWithMap payload) {
			return payload.someMap.get("b");
		}

		@PostMapping("/array")
		String postArray(@ModelAttribute("payload") PayloadWithArray payload) {
			return payload.someArray[1];
		}

		@PostMapping("/list")
		String postList(@ModelAttribute("payload") PayloadWithList payload) {
			return payload.someList.get(1);
		}
	}

}
