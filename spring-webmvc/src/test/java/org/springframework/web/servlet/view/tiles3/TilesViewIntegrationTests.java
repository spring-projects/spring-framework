/*
 * Copyright 2002-2016 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.tiles.definition.NoSuchDefinitionException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Test fixture for {@link TilesView}.
 *
 * @author mick semb wever
 * @author Sebastien Deleuze
 */
public class TilesViewIntegrationTests {

	private static final String VIEW_PATH = "test";

    private static final String VIEW_PATH_2 = "test2";

	private TilesView view1;

    private TilesView view2;

	private MockHttpServletRequest request1;

	private MockHttpServletResponse response1;

    private MockHttpServletRequest request2;

    private MockHttpServletResponse response2;


	@Before
	public void setUp() throws Exception {
		final MockServletContext servletContext = new MockServletContext();

		final StaticWebApplicationContext wac1 = new StaticWebApplicationContext();
		wac1.setDisplayName("wac1");
		wac1.setId("wac1");
		wac1.setServletContext(servletContext);
		wac1.refresh();

		request1 = new MockHttpServletRequest();
		request1.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac1);

		response1 = new MockHttpServletResponse();

        final TilesConfigurer tc1 = new TilesConfigurer();
        tc1.setDefinitions("/org/springframework/web/servlet/view/tiles3/tiles-definitions.xml");
        tc1.setCheckRefresh(true);
        tc1.setServletContext(servletContext);
        tc1.setApplicationContext(wac1);
        tc1.afterPropertiesSet();

        final StaticWebApplicationContext wac2 = new StaticWebApplicationContext();
        wac2.setDisplayName("wac2");
        wac2.setId("wac2");
        wac2.setServletContext(servletContext);
        wac2.refresh();

        request2 = new MockHttpServletRequest();
        request2.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac2);

        response2 = new MockHttpServletResponse();

        final TilesConfigurer tc2 = new TilesConfigurer();
        tc2.setDefinitions("/org/springframework/web/servlet/view/tiles3/tiles-definitions-2nd.xml");
        tc2.setCheckRefresh(true);
        tc2.setServletContext(servletContext);
        tc2.setApplicationContext(wac2);
        tc2.afterPropertiesSet();

        view1 = new TilesView();
		view1.setServletContext(servletContext);
		view1.setApplicationContext(wac1);
		view1.setUrl(VIEW_PATH);
		view1.afterPropertiesSet();

        view2 = new TilesView();
        view2.setServletContext(servletContext);
        view2.setApplicationContext(wac2);
        view2.setUrl(VIEW_PATH_2);
        view2.afterPropertiesSet();
	}

	@Test
	public void render() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("modelAttribute", "modelValue");
		view1.render(model, request1, response1);
		assertEquals("modelValue", request1.getAttribute("modelAttribute"));
        view2.render(model, request2, response2);
        assertEquals("modelValue", request2.getAttribute("modelAttribute"));
	}

    @Test(expected = NoSuchDefinitionException.class)
	public void renderPath1FromWac2() throws Exception {
        Map<String, Object> model = new HashMap<>();
        view2.setUrl(VIEW_PATH);
        view2.render(model, request2, response2);
    }

    @Test(expected = NoSuchDefinitionException.class)
    public void renderPath2FromWac1() throws Exception {
        Map<String, Object> model = new HashMap<>();
        view1.setUrl(VIEW_PATH_2);
        view1.render(model, request1, response1);
    }
}
