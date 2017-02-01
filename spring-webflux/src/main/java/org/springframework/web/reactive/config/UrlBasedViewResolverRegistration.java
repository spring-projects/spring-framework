/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.config;

import org.springframework.util.Assert;
import org.springframework.web.reactive.result.view.UrlBasedViewResolver;

/**
 * Assist with configuring properties of a {@link UrlBasedViewResolver}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class UrlBasedViewResolverRegistration {

	private final UrlBasedViewResolver viewResolver;


	public UrlBasedViewResolverRegistration(UrlBasedViewResolver viewResolver) {
		Assert.notNull(viewResolver, "ViewResolver must not be null");
		this.viewResolver = viewResolver;
	}


	/**
	 * Set the prefix that gets prepended to view names when building a URL.
	 * @see UrlBasedViewResolver#setPrefix
	 */
	public UrlBasedViewResolverRegistration prefix(String prefix) {
		this.viewResolver.setPrefix(prefix);
		return this;
	}

	/**
	 * Set the suffix that gets appended to view names when building a URL.
	 * @see UrlBasedViewResolver#setSuffix
	 */
	public UrlBasedViewResolverRegistration suffix(String suffix) {
		this.viewResolver.setSuffix(suffix);
		return this;
	}

	/**
	 * Set the view class that should be used to create views.
	 * @see UrlBasedViewResolver#setViewClass
	 */
	public UrlBasedViewResolverRegistration viewClass(Class<?> viewClass) {
		this.viewResolver.setViewClass(viewClass);
		return this;
	}

	/**
	 * Set the view names (or name patterns) that can be handled by this view
	 * resolver. View names can contain simple wildcards such that 'my*', '*Report'
	 * and '*Repo*' will all match the view name 'myReport'.
	 * @see UrlBasedViewResolver#setViewNames
	 */
	public UrlBasedViewResolverRegistration viewNames(String... viewNames) {
		this.viewResolver.setViewNames(viewNames);
		return this;
	}

	protected UrlBasedViewResolver getViewResolver() {
		return this.viewResolver;
	}

}
