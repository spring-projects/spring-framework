/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.Locale;

import org.apache.tiles.request.Request;
import org.apache.tiles.request.render.Renderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test fixture for {@link TilesViewResolver}.
 *
 * @author mick semb wever
 */
public class TilesViewResolverTests {

	private TilesViewResolver viewResolver;

	private Renderer renderer;


	@BeforeEach
	public void setUp() {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());
		wac.refresh();

		this.renderer = mock(Renderer.class);

		this.viewResolver = new TilesViewResolver();
		this.viewResolver.setRenderer(this.renderer);
		this.viewResolver.setApplicationContext(wac);
	}

	@Test
	public void testResolve() throws Exception {
		given(this.renderer.isRenderable(eq("/template.test"), isA(Request.class))).willReturn(true);
		given(this.renderer.isRenderable(eq("/nonexistent.test"), isA(Request.class))).willReturn(false);

		boolean condition = this.viewResolver.resolveViewName("/template.test", Locale.ITALY) instanceof TilesView;
		assertThat(condition).isTrue();
		assertThat(this.viewResolver.resolveViewName("/nonexistent.test", Locale.ITALY)).isNull();

		verify(this.renderer).isRenderable(eq("/template.test"), isA(Request.class));
		verify(this.renderer).isRenderable(eq("/nonexistent.test"), isA(Request.class));
	}

}
