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

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.apache.tiles.request.Request;
import org.apache.tiles.request.render.Renderer;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

/**
 * Test fixture for {@link TilesViewResolver}.
 *
 * @author mick semb wever
 */
public class TilesViewResolverTests {

	private TilesViewResolver viewResolver;

	private Renderer renderer;


	@Before
	public void setUp() {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());
		wac.refresh();

		this.renderer = EasyMock.createMock(Renderer.class);

		this.viewResolver = new TilesViewResolver();
		this.viewResolver.setRenderer(this.renderer);
		this.viewResolver.setApplicationContext(wac);
	}

	@Test
	public void testResolve() throws Exception {
		expect(this.renderer.isRenderable(eq("/template.test"), isA(Request.class))).andReturn(true);
		expect(this.renderer.isRenderable(eq("/nonexistent.test"), isA(Request.class))).andReturn(false);
		replay(this.renderer);

		assertTrue(this.viewResolver.resolveViewName("/template.test", Locale.ITALY) instanceof TilesView);
		assertNull(this.viewResolver.resolveViewName("/nonexistent.test", Locale.ITALY));

		verify(this.renderer);
	}
}
