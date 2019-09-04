/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.testng.web;

import java.io.File;

import javax.servlet.ServletContext;

import org.testng.annotations.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.Assert.*;

/**
 * TestNG-based integration tests that verify support for loading a
 * {@link WebApplicationContext} when extending {@link AbstractTestNGSpringContextTests}.
 *
 * @author Sam Brannen
 * @since 3.2.7
 */
@ContextConfiguration
@WebAppConfiguration
public class TestNGSpringContextWebTests extends AbstractTestNGSpringContextTests implements ServletContextAware {

	@Configuration
	static class Config {

		@Bean
		String foo() {
			return "enigma";
		}
	}


	ServletContext servletContext;

	@Autowired
	WebApplicationContext wac;

	@Autowired
	MockServletContext mockServletContext;

	@Autowired
	MockHttpServletRequest request;

	@Autowired
	MockHttpServletResponse response;

	@Autowired
	MockHttpSession session;

	@Autowired
	ServletWebRequest webRequest;

	@Autowired
	String foo;


	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Test
	void basicWacFeatures() throws Exception {
		assertNotNull("ServletContext should be set in the WAC.", wac.getServletContext());

		assertNotNull("ServletContext should have been set via ServletContextAware.", servletContext);

		assertNotNull("ServletContext should have been autowired from the WAC.", mockServletContext);
		assertNotNull("MockHttpServletRequest should have been autowired from the WAC.", request);
		assertNotNull("MockHttpServletResponse should have been autowired from the WAC.", response);
		assertNotNull("MockHttpSession should have been autowired from the WAC.", session);
		assertNotNull("ServletWebRequest should have been autowired from the WAC.", webRequest);

		Object rootWac = mockServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		assertNotNull("Root WAC must be stored in the ServletContext as: "
				+ WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, rootWac);
		assertSame("test WAC and Root WAC in ServletContext must be the same object.", wac, rootWac);
		assertSame("ServletContext instances must be the same object.", mockServletContext, wac.getServletContext());
		assertSame("ServletContext in the WAC and in the mock request", mockServletContext, request.getServletContext());

		assertEquals("Getting real path for ServletContext resource.",
			new File("src/main/webapp/index.jsp").getCanonicalPath(), mockServletContext.getRealPath("index.jsp"));

	}

	@Test
	void fooEnigmaAutowired() {
		assertEquals("enigma", foo);
	}

}
