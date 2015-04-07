/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.view.tiles2;

import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.impl.BasicTilesContainer;
import org.apache.tiles.servlet.context.ServletTilesRequestContext;
import org.apache.tiles.servlet.context.ServletUtil;
import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 */
public class TilesConfigurerTests {

	@Test
	@SuppressWarnings("deprecation")
	public void simpleBootstrap() {
		MockServletContext sc = new MockServletContext();
		TilesConfigurer tc = new TilesConfigurer();
		tc.setDefinitions("/org/springframework/web/servlet/view/tiles2/tiles-definitions.xml");
		tc.setCheckRefresh(true);
		tc.setServletContext(sc);
		tc.afterPropertiesSet();

		BasicTilesContainer container = (BasicTilesContainer) ServletUtil.getContainer(sc);
		TilesRequestContext requestContext = new ServletTilesRequestContext(
				container.getApplicationContext(), new MockHttpServletRequest(), new MockHttpServletResponse());
		assertNotNull(container.getDefinitionsFactory().getDefinition("test", requestContext));

		tc.destroy();
	}

}
