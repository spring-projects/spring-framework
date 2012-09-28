/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.mock.servlet.samples.standalone.resultmatchers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.mock.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.mock.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.mock.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.mock.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.mock.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Examples of expectations on the view name selected by the controller.
 *
 * @author Rossen Stoyanchev
 */
public class ViewNameAssertionTests {

	private MockMvc mockMvc;

	@Before
	public void setup() {
		this.mockMvc = standaloneSetup(new SimpleController())
				.alwaysExpect(status().isOk())
				.build();
	}

	@Test
	public void testEqualTo() throws Exception {
		this.mockMvc.perform(get("/"))
			.andExpect(view().name("mySpecialView"))
			.andExpect(view().name(equalTo("mySpecialView")));
	}

	@Test
	public void testHamcrestMatcher() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(view().name(containsString("Special")));
	}


	@Controller
	private static class SimpleController {

		@RequestMapping("/")
		public String handle() {
			return "mySpecialView";
		}
	}
}
