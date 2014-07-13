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

package org.springframework.web.servlet.config.annotation;

import org.springframework.web.servlet.view.tiles3.TilesConfigurer;
import org.springframework.web.servlet.view.tiles3.TilesViewResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Encapsulates information required to create a
 * {@link org.springframework.web.servlet.view.tiles3.TilesViewResolver} and a
 * {@link org.springframework.web.servlet.view.tiles3.TilesConfigurer} beans.
 *
 * Default definition is "/WEB-INF/tiles.xml" and no Tiles definition check refresh.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class TilesRegistration extends ViewResolutionRegistration<TilesViewResolver> {

	private List<String> definitions;
	private Boolean checkRefresh;

	public TilesRegistration(ViewResolutionRegistry registry) {
		super(registry, new TilesViewResolver());
	}

	/**
	 * Set the prefix that gets prepended to view names when building a URL.
	 *
	 * @see TilesViewResolver#setPrefix(String)
	 */
	public TilesRegistration prefix(String prefix) {
		this.viewResolver.setPrefix(prefix);
		return this;
	}

	/**
	 * Set the suffix that gets appended to view names when building a URL.
	 *
	 * @see TilesViewResolver#setSuffix(String)
	 */
	public TilesRegistration suffix(String suffix) {
		this.viewResolver.setSuffix(suffix);
		return this;
	}

	/**
	 * Set the Tiles definitions, i.e. a single value or a list of files containing the definitions.
	 *
	 * @see TilesConfigurer#setDefinitions(String...)
	 */
	public TilesRegistration definition(String... definitions) {
		if(this.definitions == null) {
			this.definitions = new ArrayList<String>();
		}
		this.definitions.addAll(Arrays.asList(definitions));
		return this;
	}

	/**
	 * Set whether to check Tiles definition files for a refresh at runtime.
	 *
 	 * @see TilesConfigurer#setCheckRefresh(boolean)
	 */
	public TilesRegistration checkRefresh(boolean checkRefresh) {
		this.checkRefresh = checkRefresh;
		return this;
	}

	protected TilesConfigurer getTilesConfigurer() {
		TilesConfigurer tilesConfigurer = new TilesConfigurer();
		if(this.definitions != null && !this.definitions.isEmpty()) {
			tilesConfigurer.setDefinitions(this.definitions.toArray(new String[0]));
		}
		if(this.checkRefresh != null) {
			tilesConfigurer.setCheckRefresh(this.checkRefresh);
		}
		return tilesConfigurer;
	}

}
