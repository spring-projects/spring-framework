/*
 * Copyright 2002-2019 the original author or authors.
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
package org.springframework.web.servlet.view.tiles3;

import java.util.HashMap;
import java.util.Map;

import org.apache.tiles.request.AbstractRequest;
import org.apache.tiles.request.Request;
import org.apache.tiles.request.render.Renderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test fixture for {@link TilesView}.
 *
 * @author mick semb wever
 * @author Sebastien Deleuze
 */
public class TilesViewTests {

	private static final String VIEW_PATH = "template.test";

	private TilesView view;

	private Renderer renderer;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@BeforeEach
	public void setUp() throws Exception {
		MockServletContext servletContext = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(servletContext);
		wac.refresh();

		request = new MockHttpServletRequest();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		response = new MockHttpServletResponse();

		renderer = mock(Renderer.class);

		view = new TilesView();
		view.setServletContext(servletContext);
		view.setRenderer(renderer);
		view.setUrl(VIEW_PATH);
		view.afterPropertiesSet();
	}

	@Test
	public void render() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("modelAttribute", "modelValue");
		view.render(model, request, response);
		assertThat(request.getAttribute("modelAttribute")).isEqualTo("modelValue");
		verify(renderer).render(eq(VIEW_PATH), isA(Request.class));
	}

	@Test
	public void alwaysIncludeDefaults() throws Exception {
		view.render(new HashMap<>(), request, response);
		assertThat(request.getAttribute(AbstractRequest.FORCE_INCLUDE_ATTRIBUTE_NAME)).isNull();
	}

	@Test
	public void alwaysIncludeEnabled() throws Exception {
		view.setAlwaysInclude(true);
		view.render(new HashMap<>(), request, response);
		assertThat((boolean) (Boolean) request.getAttribute(AbstractRequest.FORCE_INCLUDE_ATTRIBUTE_NAME)).isTrue();
	}

}
