/*
 * Copyright 2002-2013 the original author or authors.
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

import org.apache.tiles.Definition;
import org.apache.tiles.access.TilesAccess;
import org.apache.tiles.impl.BasicTilesContainer;
import org.apache.tiles.request.ApplicationContext;
import org.apache.tiles.request.Request;
import org.apache.tiles.request.servlet.ServletRequest;
import org.apache.tiles.request.servlet.ServletUtil;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link TilesConfigurer}.
 *
 * @author Nicolas Le Bas
 */
public class TilesConfigurerTests {

	@Test
	public void simpleBootstrap() {
		MockServletContext servletContext = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(servletContext);
		wac.refresh();

		TilesConfigurer tc = new TilesConfigurer();
		tc.setDefinitions("/org/springframework/web/servlet/view/tiles3/tiles-definitions.xml");
		tc.setCheckRefresh(true);
		tc.setServletContext(servletContext);
		tc.setApplicationContext(wac);
		tc.afterPropertiesSet();

		ApplicationContext tilesContext = ServletUtil.getApplicationContext(servletContext);

		BasicTilesContainer container = (BasicTilesContainer) TilesAccess.getContainer(tilesContext, wac.getId());
		Request requestContext = new ServletRequest(container.getApplicationContext(),
				new MockHttpServletRequest(), new MockHttpServletResponse());
        Definition definition = container.getDefinitionsFactory().getDefinition("test", requestContext);
        assertNotNull(definition);
        assertEquals("/WEB-INF/tiles/test.jsp", definition.getTemplateAttribute().getValue());

        tc.destroy();
	}

    @Test
    public void moreThanOneContextBootstrap() {
        MockServletContext servletContext = new MockServletContext();

        StaticWebApplicationContext wac1 = new StaticWebApplicationContext();
        wac1.setDisplayName("wac1");
        wac1.setId("wac1");
        wac1.setServletContext(servletContext);
        wac1.refresh();

        TilesConfigurer tc1 = new TilesConfigurer();
        tc1.setDefinitions("/org/springframework/web/servlet/view/tiles3/tiles-definitions.xml");
        tc1.setCheckRefresh(true);
        tc1.setServletContext(servletContext);
        tc1.setApplicationContext(wac1);
        tc1.afterPropertiesSet();


        StaticWebApplicationContext wac2 = new StaticWebApplicationContext();
        wac2.setDisplayName("wac2");
        wac2.setId("wac2");
        wac2.setServletContext(servletContext);
        wac2.refresh();

        TilesConfigurer tc2 = new TilesConfigurer();
        tc2.setDefinitions("/org/springframework/web/servlet/view/tiles3/tiles-definitions-2nd.xml");
        tc2.setCheckRefresh(true);
        tc2.setServletContext(servletContext);
        tc2.setApplicationContext(wac2);
        tc2.afterPropertiesSet();

        ApplicationContext tilesContext = ServletUtil.getApplicationContext(servletContext);
        assertNotNull(tilesContext.getApplicationScope().get(wac1.getId()));
        assertNotNull(tilesContext.getApplicationScope().get(wac2.getId()));
        assertNull(TilesAccess.getContainer(tilesContext));

        try {
            BasicTilesContainer container = (BasicTilesContainer) TilesAccess.getContainer(tilesContext, wac1.getId());
            Request requestContext = new ServletRequest(container.getApplicationContext(),
                    new MockHttpServletRequest(), new MockHttpServletResponse());
            Definition definition = container.getDefinitionsFactory().getDefinition("test", requestContext);
            assertNotNull(definition);
            assertEquals("/WEB-INF/tiles/test.jsp", definition.getTemplateAttribute().getValue());
        } finally {

        }

        try {
            BasicTilesContainer container = (BasicTilesContainer) TilesAccess.getContainer(tilesContext, wac2.getId());
            Request requestContext = new ServletRequest(container.getApplicationContext(),
                    new MockHttpServletRequest(), new MockHttpServletResponse());
            Definition definition = container.getDefinitionsFactory().getDefinition("test2", requestContext);
            assertNotNull(definition);
            assertEquals("/WEB-INF/tiles/test2.jsp", definition.getTemplateAttribute().getValue());
        } finally {

        }


        tc1.destroy();
    }


	@Configuration
	public static class AppConfig {
	}

}
