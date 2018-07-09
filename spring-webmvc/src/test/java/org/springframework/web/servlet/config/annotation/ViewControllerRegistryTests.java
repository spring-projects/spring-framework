/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.RedirectView;

import static org.junit.Assert.*;

/**
 * Test fixture with a {@link ViewControllerRegistry}.
 *
 * @author Rossen Stoyanchev
 */
public class ViewControllerRegistryTests {

	private ViewControllerRegistry registry;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@Before
	public void setup() {
		this.registry = new ViewControllerRegistry(new StaticApplicationContext());
		this.request = new MockHttpServletRequest("GET", "/");
		this.response = new MockHttpServletResponse();
	}


	@Test
	public void noViewControllers() {
		assertNull(this.registry.buildHandlerMapping());
	}

	@Test
	public void addViewController() {
		this.registry.addViewController("/path").setViewName("viewName");
		ParameterizableViewController controller = getController("/path");

		assertEquals("viewName", controller.getViewName());
		assertNull(controller.getStatusCode());
		assertFalse(controller.isStatusOnly());
		assertNotNull(controller.getApplicationContext());
	}

	@Test
	public void addViewControllerWithDefaultViewName() {
		this.registry.addViewController("/path");
		ParameterizableViewController controller = getController("/path");

		assertNull(controller.getViewName());
		assertNull(controller.getStatusCode());
		assertFalse(controller.isStatusOnly());
		assertNotNull(controller.getApplicationContext());
	}

	@Test
	public void addRedirectViewController() throws Exception {
		this.registry.addRedirectViewController("/path", "/redirectTo");
		RedirectView redirectView = getRedirectView("/path");
		this.request.setQueryString("a=b");
		this.request.setContextPath("/context");
		redirectView.render(Collections.emptyMap(), this.request, this.response);

		assertEquals(302, this.response.getStatus());
		assertEquals("/context/redirectTo", this.response.getRedirectedUrl());
		assertNotNull(redirectView.getApplicationContext());
	}

	@Test
	public void addRedirectViewControllerWithCustomSettings() throws Exception {
		this.registry.addRedirectViewController("/path", "/redirectTo")
				.setContextRelative(false)
				.setKeepQueryParams(true)
				.setStatusCode(HttpStatus.PERMANENT_REDIRECT);

		RedirectView redirectView = getRedirectView("/path");
		this.request.setQueryString("a=b");
		this.request.setContextPath("/context");
		redirectView.render(Collections.emptyMap(), this.request, this.response);

		assertEquals(308, this.response.getStatus());
		assertEquals("/redirectTo?a=b", response.getRedirectedUrl());
		assertNotNull(redirectView.getApplicationContext());
	}

	@Test
	public void addStatusController() {
		this.registry.addStatusController("/path", HttpStatus.NOT_FOUND);
		ParameterizableViewController controller = getController("/path");

		assertNull(controller.getViewName());
		assertEquals(HttpStatus.NOT_FOUND, controller.getStatusCode());
		assertTrue(controller.isStatusOnly());
		assertNotNull(controller.getApplicationContext());
	}

	@Test
	public void order() {
		this.registry.addViewController("/path");
		SimpleUrlHandlerMapping handlerMapping = this.registry.buildHandlerMapping();
		assertEquals(1, handlerMapping.getOrder());

		this.registry.setOrder(2);
		handlerMapping = this.registry.buildHandlerMapping();
		assertEquals(2, handlerMapping.getOrder());
	}


	private ParameterizableViewController getController(String path) {
		Map<String, ?> urlMap = this.registry.buildHandlerMapping().getUrlMap();
		ParameterizableViewController controller = (ParameterizableViewController) urlMap.get(path);
		assertNotNull(controller);
		return controller;
	}

	private RedirectView getRedirectView(String path) {
		ParameterizableViewController controller = getController(path);
		assertNull(controller.getViewName());
		assertNotNull(controller.getView());
		assertEquals(RedirectView.class, controller.getView().getClass());
		return (RedirectView) controller.getView();
	}

}
