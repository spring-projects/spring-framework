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

import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

/**
 * Encapsulates information required to create a
 * {@link org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver} and a
 * {@link org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer} beans.
 * Default configuration is "" prefix, ".ftl" suffix and "/WEB-INF/" templateLoaderPath.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class FreeMarkerRegistration extends ViewResolutionRegistration<FreeMarkerViewResolver> {


	public FreeMarkerRegistration(ViewResolutionRegistry registry) {
		super(registry, new FreeMarkerViewResolver());
		this.prefix("");
		this.suffix(".ftl");
	}

	/**
	 * Set the prefix that gets prepended to view names when building a URL.
	 *
	 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver#setPrefix(String)
	 */
	public FreeMarkerRegistration prefix(String prefix) {
		this.viewResolver.setPrefix(prefix);
		return this;
	}

	/**
	 * Set the suffix that gets appended to view names when building a URL.
	 *
	 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver#setSuffix(String)
	 */
	public FreeMarkerRegistration suffix(String suffix) {
		this.viewResolver.setSuffix(suffix);
		return this;
	}

	/**
	 * Enable or disable caching.
	 *
	 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver#setCache(boolean)
	 */
	public FreeMarkerRegistration cache(boolean cache) {
		this.viewResolver.setCache(cache);
		return this;
	}

}
