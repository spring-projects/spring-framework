/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.config.annotation;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with a {@link ViewControllerRegistry}.
 *
 * @author Rossen Stoyanchev
 */
class ViewControllerRegistryTests {

	private ViewControllerRegistry registry;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@BeforeEach
	void setup() {
		this.registry = new ViewControllerRegistry(new StaticApplicationContext());
		this.request = new MockHttpServletRequest("GET", "/");
		this.response = new MockHttpServletResponse();
	}


	@Test
	void noViewControllers() {
		assertThat(this.registry.buildHandlerMapping()).isNull();
	}

	@Test
	void addViewController() {
		this.registry.addViewController("/path").setViewName("viewName");
		ParameterizableViewController controller = getController("/path");

		assertThat(controller.getViewName()).isEqualTo("viewName");
		assertThat(controller.getStatusCode()).isNull();
		assertThat(controller.isStatusOnly()).isFalse();
		assertThat(controller.getApplicationContext()).isNotNull();
	}

	@Test
	void addViewControllerWithDefaultViewName() {
		this.registry.addViewController("/path");
		ParameterizableViewController controller = getController("/path");

		assertThat(controller.getViewName()).isNull();
		assertThat(controller.getStatusCode()).isNull();
		assertThat(controller.isStatusOnly()).isFalse();
		assertThat(controller.getApplicationContext()).isNotNull();
	}

	@Test
	void addRedirectViewController() throws Exception {
		this.registry.addRedirectViewController("/path", "/redirectTo");
		RedirectView redirectView = getRedirectView("/path");
		this.request.setQueryString("a=b");
		this.request.setContextPath("/context");
		redirectView.render(Collections.emptyMap(), this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(302);
		assertThat(this.response.getRedirectedUrl()).isEqualTo("/context/redirectTo");
		assertThat(redirectView.getApplicationContext()).isNotNull();
	}

	@Test
	void addRedirectViewControllerWithCustomSettings() throws Exception {
		this.registry.addRedirectViewController("/path", "/redirectTo")
				.setContextRelative(false)
				.setKeepQueryParams(true)
				.setStatusCode(HttpStatus.PERMANENT_REDIRECT);

		RedirectView redirectView = getRedirectView("/path");
		this.request.setQueryString("a=b");
		this.request.setContextPath("/context");
		redirectView.render(Collections.emptyMap(), this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(308);
		assertThat(response.getRedirectedUrl()).isEqualTo("/redirectTo?a=b");
		assertThat(redirectView.getApplicationContext()).isNotNull();
	}

	@Test
	void addStatusController() {
		this.registry.addStatusController("/path", HttpStatus.NOT_FOUND);
		ParameterizableViewController controller = getController("/path");

		assertThat(controller.getViewName()).isNull();
		assertThat(controller.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(controller.isStatusOnly()).isTrue();
		assertThat(controller.getApplicationContext()).isNotNull();
	}

	@Test
	void order() {
		this.registry.addViewController("/path");
		SimpleUrlHandlerMapping handlerMapping = this.registry.buildHandlerMapping();
		assertThat(handlerMapping.getOrder()).isEqualTo(1);

		this.registry.setOrder(2);
		handlerMapping = this.registry.buildHandlerMapping();
		assertThat(handlerMapping.getOrder()).isEqualTo(2);
	}


	private ParameterizableViewController getController(String path) {
		Map<String, ?> urlMap = this.registry.buildHandlerMapping().getUrlMap();
		ParameterizableViewController controller = (ParameterizableViewController) urlMap.get(path);
		assertThat(controller).isNotNull();
		return controller;
	}

	private RedirectView getRedirectView(String path) {
		ParameterizableViewController controller = getController(path);
		assertThat(controller.getViewName()).isNull();
		assertThat(controller.getView()).isNotNull();
		assertThat(controller.getView().getClass()).isEqualTo(RedirectView.class);
		return (RedirectView) controller.getView();
	}

}
