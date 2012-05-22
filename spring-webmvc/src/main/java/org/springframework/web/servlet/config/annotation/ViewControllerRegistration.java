/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.util.Assert;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * Encapsulates information required to create a view controller.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @since 3.1
 */
public class ViewControllerRegistration {

	private final String urlPath;

	private String viewName;

	/**
	 * Creates a {@link ViewControllerRegistration} with the given URL path. When a request matches
	 * to the given URL path this view controller will process it.
	 */
	public ViewControllerRegistration(String urlPath) {
		Assert.notNull(urlPath, "A URL path is required to create a view controller.");
		this.urlPath = urlPath;
	}

	/**
	 * Sets the view name to use for this view controller. This field is optional. If not specified the
	 * view controller will return a {@code null} view name, which will be resolved through the configured
	 * {@link RequestToViewNameTranslator}. By default that means "/foo/bar" would resolve to "foo/bar".
	 */
	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

	/**
	 * Returns the URL path for the view controller.
	 */
	protected String getUrlPath() {
		return urlPath;
	}

	/**
	 * Returns the view controllers.
	 */
	protected Object getViewController() {
		ParameterizableViewController controller = new ParameterizableViewController();
		controller.setViewName(viewName);
		return controller;
	}

}
