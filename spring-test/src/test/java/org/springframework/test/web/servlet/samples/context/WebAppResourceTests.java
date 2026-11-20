/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.samples.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests dependent on access to resources under the web application root directory.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration("src/test/resources/META-INF/web-resources")
@ContextHierarchy({
	@ContextConfiguration("root-context.xml"),
	@ContextConfiguration("servlet-context.xml")
})
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
class WebAppResourceTests {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@BeforeEach
	void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).alwaysExpect(status().isOk()).build();
	}

	// Resources served via <mvc:resources/>

	@Test
	void resourceRequest() throws Exception {
		this.mockMvc.perform(get("/resources/Spring.js"))
			.andExpect(content().contentType("text/javascript"))
			.andExpect(content().string(containsString("Spring={};")));
	}

	// Forwarded to the "default" servlet via <mvc:default-servlet-handler/>

	@Test
	void resourcesViaDefaultServlet() throws Exception {
		this.mockMvc.perform(get("/unknown/resource"))
			.andExpect(handler().handlerType(DefaultServletHttpRequestHandler.class))
			.andExpect(forwardedUrl("default"));
	}

}
