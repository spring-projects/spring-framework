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

import java.util.Map;

import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * Assist with configuring a {@link org.springframework.web.servlet.view.UrlBasedViewResolver}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class UrlBasedViewResolverRegistration {

	protected final UrlBasedViewResolver viewResolver;


	public UrlBasedViewResolverRegistration(UrlBasedViewResolver viewResolver) {
		this.viewResolver = viewResolver;
	}


	protected UrlBasedViewResolver getViewResolver() {
		return this.viewResolver;
	}

	/**
	 * Set the prefix that gets prepended to view names when building a URL.
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setPrefix
	 */
	public UrlBasedViewResolverRegistration prefix(String prefix) {
		this.viewResolver.setPrefix(prefix);
		return this;
	}

	/**
	 * Set the suffix that gets appended to view names when building a URL.
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setSuffix
	 */
	public UrlBasedViewResolverRegistration suffix(String suffix) {
		this.viewResolver.setSuffix(suffix);
		return this;
	}

	/**
	 * Set the view class that should be used to create views.
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setViewClass
	 */
	public UrlBasedViewResolverRegistration viewClass(Class<?> viewClass) {
		this.viewResolver.setViewClass(viewClass);
		return this;
	}

	/**
	 * Set the view names (or name patterns) that can be handled by this view
	 * resolver. View names can contain simple wildcards such that 'my*', '*Report'
	 * and '*Repo*' will all match the view name 'myReport'.
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setViewNames
	 */
	public UrlBasedViewResolverRegistration viewNames(String... viewNames) {
		this.viewResolver.setViewNames(viewNames);
		return this;
	}

	/**
	 * Set static attributes to be added to the model of every request for all
	 * views resolved by this view resolver. This allows for setting any kind of
	 * attribute values, for example bean references.
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setAttributesMap
	 */
	public UrlBasedViewResolverRegistration attributes(Map<String, ?> attributes) {
		this.viewResolver.setAttributesMap(attributes);
		return this;
	}

	/**
	 * Specify the maximum number of entries for the view cache.
	 * Default is 1024.
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setCache(boolean)
	 */
	public UrlBasedViewResolverRegistration cacheLimit(int cacheLimit) {
		this.viewResolver.setCacheLimit(cacheLimit);
		return this;
	}

	/**
	 * Enable or disable caching.
	 * <p>This is equivalent to setting the {@link #cacheLimit "cacheLimit"}
	 * property to the default limit (1024) or to 0, respectively.
	 * <p>Default is "true": caching is enabled.
	 * Disable this only for debugging and development.
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setCache(boolean)
	 */
	public UrlBasedViewResolverRegistration cache(boolean cache) {
		this.viewResolver.setCache(cache);
		return this;
	}

}


