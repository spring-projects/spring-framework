/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.web.servlet.view.tiles3;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.tiles.request.ApplicationContext;
import org.apache.tiles.request.Request;
import org.apache.tiles.request.render.Renderer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Test fixture for {@link TilesView}.
 *
 * @author mick semb wever
 */
public class TilesViewTests {

	private static final String VIEW_PATH = "template.test";

	private TilesView view;

	private Renderer renderer;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@Before
	public void setUp() throws Exception {
		MockServletContext servletContext = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(servletContext);
		wac.refresh();

		request = new MockHttpServletRequest();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		response = new MockHttpServletResponse();

		renderer = createMock(Renderer.class);

		view = new TilesView();
		view.setServletContext(servletContext);
		view.setRenderer(renderer);
		view.setUrl(VIEW_PATH);
		view.afterPropertiesSet();
	}

	@Test
	public void testRender() throws Exception {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("modelAttribute", "modelValue");

		ApplicationContext tilesContext = createMock(ApplicationContext.class);

		renderer.render(eq(VIEW_PATH), and(isA(Request.class), isA(Request.class)));
		replay(tilesContext, renderer);

		view.render(model, request, response);

		assertEquals("modelValue", request.getAttribute("modelAttribute"));
		verify(tilesContext, renderer);
	}

}
