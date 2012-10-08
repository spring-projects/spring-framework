/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.test.web.mock.servlet.samples.context;

import static org.springframework.test.web.mock.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.mock.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.mock.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.mock.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.mock.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.mock.servlet.samples.context.SecurityRequestPostProcessors.user;
import static org.springframework.test.web.mock.servlet.samples.context.SecurityRequestPostProcessors.userDeatilsService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.mock.servlet.MockMvc;
import org.springframework.test.web.mock.servlet.MvcResult;
import org.springframework.test.web.mock.servlet.ResultMatcher;
import org.springframework.test.web.mock.servlet.request.RequestPostProcessor;
import org.springframework.test.web.mock.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Basic example that includes Spring Security configuration.
 *
 * <p>Note that currently there are no {@linkplain ResultMatcher ResultMatchers}
 * built specifically for asserting the Spring Security context. However, it's
 * quite easy to put them together as shown below, and Spring Security extensions
 * will become available in the near future.
 *
 * <p>This also demonstrates a custom {@link RequestPostProcessor} which authenticates
 * a user to a particular {@link HttpServletRequest}.
 *
 * @author Rob Winch
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see SecurityRequestPostProcessors
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration("src/test/resources/META-INF/web-resources")
@ContextConfiguration({ "security.xml", "servlet-context.xml" })
public class SpringSecurityTests {

	private static final String SEC_CONTEXT_ATTR = HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

	@Autowired
	private FilterChainProxy springSecurityFilterChain;

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;


	@Before
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac)//
		.addFilters(this.springSecurityFilterChain)//
		.build();
	}

	@Test
	public void requiresAuthentication() throws Exception {
		mockMvc.perform(get("/user")).//
		andExpect(redirectedUrl("http://localhost/spring_security_login"));
	}

	@Test
	public void accessGranted() throws Exception {
		this.mockMvc.perform(get("/").//
		with(userDeatilsService("user"))).//
		andExpect(status().isOk()).//
		andExpect(forwardedUrl("/WEB-INF/layouts/standardLayout.jsp"));
	}

	@Test
	public void accessDenied() throws Exception {
		this.mockMvc.perform(get("/")//
		.with(user("user").roles("DENIED")))//
		.andExpect(status().isForbidden());
	}

	@Test
	public void userAuthenticates() throws Exception {
		final String username = "user";
		mockMvc.perform(post("/j_spring_security_check").//
		param("j_username", username).//
		param("j_password", "password")).//
		andExpect(redirectedUrl("/")).//
		andExpect(new ResultMatcher() {

			public void match(MvcResult mvcResult) throws Exception {
				HttpSession session = mvcResult.getRequest().getSession();
				SecurityContext securityContext = (SecurityContext) session.getAttribute(SEC_CONTEXT_ATTR);
				Assert.assertEquals(securityContext.getAuthentication().getName(), username);
			}
		});
	}

	@Test
	public void userAuthenticateFails() throws Exception {
		final String username = "user";
		mockMvc.perform(post("/j_spring_security_check").//
		param("j_username", username).//
		param("j_password", "invalid")).//
		andExpect(redirectedUrl("/spring_security_login?login_error")).//
		andExpect(new ResultMatcher() {

			public void match(MvcResult mvcResult) throws Exception {
				HttpSession session = mvcResult.getRequest().getSession();
				SecurityContext securityContext = (SecurityContext) session.getAttribute(SEC_CONTEXT_ATTR);
				Assert.assertNull(securityContext);
			}
		});
	}

}
