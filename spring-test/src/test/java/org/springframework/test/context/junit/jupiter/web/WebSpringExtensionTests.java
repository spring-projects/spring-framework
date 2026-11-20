/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Integration tests which demonstrate use of the Spring MVC Test Framework and
 * the Spring TestContext Framework with JUnit Jupiter and the
 * {@link SpringExtension} (via a custom
 * {@link SpringJUnitWebConfig @SpringJUnitWebConfig} composed annotation).
 *
 * <p>Note how the {@link #springMvcTest(WebApplicationContext)} test method
 * has the {@link WebApplicationContext} injected as a method parameter.
 * This allows the {@link MockMvc} instance to be configured local to the
 * test method without any fields in the test class.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see SpringExtension
 * @see SpringJUnitWebConfig
 * @see org.springframework.test.context.junit.jupiter.web.MultipleWebRequestsSpringExtensionTests
 * @see org.springframework.test.context.junit.jupiter.SpringExtensionTests
 * @see org.springframework.test.context.junit.jupiter.ComposedSpringExtensionTests
 */
@SpringJUnitWebConfig(classes = WebConfig.class, loader = AnnotationConfigWebContextLoader.class)
@DisplayName("Web SpringExtension Tests")
class WebSpringExtensionTests {

	@Test
	void springMvcTest(WebApplicationContext wac) throws Exception {
		webAppContextSetup(wac).build()
			.perform(get("/person/42").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name", is("Dilbert")));
	}

}
