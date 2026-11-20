/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.samples.spr;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.FormContentFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Test for issues related to form content.
 *
 * @author Rossen Stoyanchev
 */
class FormContentTests {

	@Test // SPR-15753
	void formContentIsNotDuplicated() throws Exception {

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new Spr15753Controller())
				.addFilter(new FormContentFilter())
				.build();

		mockMvc.perform(put("/").content("d1=a&d2=s").contentType(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(content().string("d1:a, d2:s."));
	}


	@RestController
	private static class Spr15753Controller {

		@PutMapping("/")
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
