/*
 * Copyright 2002-2017 the original author or authors.
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

import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.HttpPutFormContentFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Test for issues related to form content.
 *
 * @author Rossen Stoyanchev
 */
public class FormContentTests {

	@Test // SPR-15753
	public void formContentIsNotDuplicated() throws Exception {

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new Spr15753Controller())
				.addFilter(new HttpPutFormContentFilter())
				.build();

		mockMvc.perform(put("/").content("d1=a&d2=s").contentType(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(content().string("d1:a, d2:s."));
	}


	@RestController
	private static class Spr15753Controller {

		@PutMapping
		public String test(Data d) {
			return String.format("d1:%s, d2:%s.", d.getD1(), d.getD2());
		}
	}

	@SuppressWarnings("unused")
	private static class Data {

		private String d1;

		private String d2;

		public Data() {
		}

		public String getD1() {
			return d1;
		}

		public void setD1(String d1) {
			this.d1 = d1;
		}

		public String getD2() {
			return d2;
		}

		public void setD2(String d2) {
			this.d2 = d2;
		}
	}

}
