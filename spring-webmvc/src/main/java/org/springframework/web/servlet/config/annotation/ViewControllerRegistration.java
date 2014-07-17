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

import org.springframework.util.Assert;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * Assist with the registration of a single view controller.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @since 3.1
 */
public class ViewControllerRegistration {

	private final String urlPath;

	private String viewName;


	/**
	 * Creates a registration for the given URL path (or path pattern).
	 */
	public ViewControllerRegistration(String urlPath) {
		Assert.notNull(urlPath, "'urlPath' is required.");
		this.urlPath = urlPath;
	}


	/**
	 * Set the view name to return.
	 *
	 * <p>If not specified, the view controller returns {@code null} as the view
	 * name in which case the configured {@link RequestToViewNameTranslator}
	 * selects the view. In effect {@code DefaultRequestToViewNameTranslator}
	 * translates "/foo/bar" to "foo/bar".
	 *
	 * @see org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator
	 */
	public void setViewName(String viewName) {
		this.viewName = viewName;
	}


	protected String getUrlPath() {
		return this.urlPath;
	}

	protected Object getViewController() {
		ParameterizableViewController controller = new ParameterizableViewController();
		controller.setViewName(this.viewName);
		return controller;
	}

}
