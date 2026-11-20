/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.samples.standalone.resultmatchers;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerMapping;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Examples of expectations on created request attributes.
 *
 * @author Rossen Stoyanchev
 */
class RequestAttributeAssertionTests {

	private final MockMvc mockMvc = standaloneSetup(new SimpleController()).build();


	@Test
	void requestAttributeEqualTo() throws Exception {
		this.mockMvc.perform(get("/main/1").servletPath("/main"))
			.andExpect(request().attribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/{id}"))
			.andExpect(request().attribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/1"));
	}

	@Test
	void requestAttributeMatcher() throws Exception {
		String producibleMediaTypes = HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;

		this.mockMvc.perform(get("/1"))
			.andExpect(request().attribute(producibleMediaTypes, hasItem(MediaType.APPLICATION_JSON)))
			.andExpect(request().attribute(producibleMediaTypes, not(hasItem(MediaType.APPLICATION_XML))));

		this.mockMvc.perform(get("/main/1").servletPath("/main"))
			.andExpect(request().attribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, equalTo("/{id}")))
			.andExpect(request().attribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, equalTo("/1")));
	}


	@Controller
	private static class SimpleController {

		@RequestMapping(path="/{id}", produces="application/json")
		String show() {
			return "view";
		}
	}

}
