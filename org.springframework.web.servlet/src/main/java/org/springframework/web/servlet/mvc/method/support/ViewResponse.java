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

package org.springframework.web.servlet.mvc.method.support;

import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Provides annotated controller methods with convenience methods for setting
 * up a response with a view name that does not have the "redirect:" prefix .
 * 
 * <p>An instance of this class is obtained via {@link ResponseContext#view}.
 *
 * @author Keith Donald
 * @author Rossen Stoyanchev
 * 
 * @since 3.1
 */
public class ViewResponse {

	private final ModelAndViewContainer mavContainer;
	
	ViewResponse(ModelAndViewContainer mavContainer) {
		this.mavContainer = mavContainer;
	}

	public ViewResponse attribute(String attributeName, Object attributeValue) {
		this.mavContainer.addAttribute(attributeName, attributeValue);
		return this;
	}

	public ViewResponse attribute(Object attributeValue) {
		this.mavContainer.addAttribute(attributeValue);
		return this;
	}

}
