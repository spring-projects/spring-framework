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

import org.apache.tiles.request.render.Renderer;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * Convenience subclass of {@link UrlBasedViewResolver} that supports
 * {@link TilesView} (i.e. Tiles definitions) and custom subclasses of it.
 *
 * @author Nicolas Le Bas
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class TilesViewResolver extends UrlBasedViewResolver {

	private Renderer renderer;


	@Override
	@SuppressWarnings("rawtypes")
	protected Class getViewClass() {
		return TilesView.class;
	}

	/**
	 * Set the {@link Renderer} to use.
	 * If not set, by default {@link DefinitionRenderer} is used.
	 * @see TilesView#setRenderer(Renderer)
	 */
	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}

	@Override
	protected TilesView buildView(String viewName) throws Exception {
		TilesView view = (TilesView) super.buildView(viewName);
		view.setRenderer(this.renderer);
		return view;
	}

}
