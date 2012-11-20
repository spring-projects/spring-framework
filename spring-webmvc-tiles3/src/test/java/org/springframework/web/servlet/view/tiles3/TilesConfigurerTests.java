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

import static org.junit.Assert.assertNotNull;

import org.apache.tiles.access.TilesAccess;
import org.apache.tiles.impl.BasicTilesContainer;
import org.apache.tiles.request.ApplicationContext;
import org.apache.tiles.request.Request;
import org.apache.tiles.request.servlet.ServletRequest;
import org.apache.tiles.request.servlet.ServletUtil;
import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

/**
 * Test fixture for {@link TilesConfigurer}.
 *
 * @author Nicolas Le Bas
 */
public class TilesConfigurerTests {

	@Test
	public void simpleBootstrap() {
		MockServletContext servletContext = new MockServletContext();

		TilesConfigurer tc = new TilesConfigurer();
		tc.setDefinitions(new String[] { "/org/springframework/web/servlet/view/tiles3/tiles-definitions.xml" });
		tc.setCheckRefresh(true);
		tc.setServletContext(servletContext);
		tc.afterPropertiesSet();

		ApplicationContext tilesContext = ServletUtil.getApplicationContext(servletContext);

		BasicTilesContainer container = (BasicTilesContainer) TilesAccess.getContainer(tilesContext);
		Request requestContext = new ServletRequest(container.getApplicationContext(),
				new MockHttpServletRequest(), new MockHttpServletResponse());
		assertNotNull(container.getDefinitionsFactory().getDefinition("test", requestContext));

		tc.destroy();
	}

	@Configuration
	public static class AppConfig {
	}

}
