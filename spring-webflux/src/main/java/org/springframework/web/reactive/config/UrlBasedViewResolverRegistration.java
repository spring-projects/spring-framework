/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
