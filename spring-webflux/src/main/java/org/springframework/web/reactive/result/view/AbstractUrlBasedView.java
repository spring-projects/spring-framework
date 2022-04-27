/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.result.view;

import java.util.Locale;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * Abstract base class for URL-based views. Provides a consistent way of
 * holding the URL that a View wraps, in the form of a "url" bean property.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractUrlBasedView extends AbstractView implements InitializingBean {

	@Nullable
	private String url;


	/**
	 * Constructor for use as a bean.
	 */
	protected AbstractUrlBasedView() {
	}

	/**
	 * Create a new AbstractUrlBasedView with the given URL.
	 */
	protected AbstractUrlBasedView(String url) {
		this.url = url;
	}


	/**
	 * Set the URL of the resource that this view wraps.
	 * The URL must be appropriate for the concrete View implementation.
	 */
	public void setUrl(@Nullable String url) {
		this.url = url;
	}

	/**
	 * Return the URL of the resource that this view wraps.
	 */
	@Nullable
	public String getUrl() {
		return this.url;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (getUrl() == null) {
			throw new IllegalArgumentException("Property 'url' is required");
		}
	}

	/**
	 * Check whether the resource for the configured URL actually exists.
	 * @param locale the desired Locale that we're looking for
	 * @return {@code false} if the resource exists
	 * {@code false} if we know that it does not exist
	 * @throws Exception if the resource exists but is invalid (e.g. could not be parsed)
	 */
	public abstract boolean checkResourceExists(Locale locale) throws Exception;


	@Override
	public String toString() {
		return super.toString() + "; URL [" + getUrl() + "]";
	}

}
