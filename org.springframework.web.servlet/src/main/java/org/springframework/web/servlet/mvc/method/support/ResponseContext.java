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

import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Provides annotated controllers with convenience methods for setting up view
 * resolution. 
 * 
 * @author Keith Donald
 * @author Rossen Stoyanchev
 * 
 * @since 3.1
 */
public class ResponseContext {

	private final NativeWebRequest webRequest;

	private final ModelAndViewContainer mavContainer;
	
	public ResponseContext(NativeWebRequest webRequest, ModelAndViewContainer mavContainer) {
		this.webRequest = webRequest;
		this.mavContainer = mavContainer;
	}
	
	/**
	 * Set up view resolution based on the given view name and the implicit model
	 * of the current request. 
	 */
	public ViewResponse view(String viewName) {
		this.mavContainer.setViewName(viewName);
		return new ViewResponse(this.mavContainer);
	}
	
	/**
	 * Set up view resolution for a redirect. This method clears the implicit 
	 * model. Use convenience methods on the returned {@link RedirectResponse} 
	 * instance to add URI variables, query parameters, and flash attributes 
	 * as necessary.
	 * @param redirectUri a URI template either relative to the current URL or 
	 * absolute; do not prefix with "redirect:"
	 */
	public RedirectResponse redirect(String redirectUri) {
		if (!redirectUri.startsWith("redirect:")) {
			redirectUri = "redirect:" + redirectUri;
		}
		this.mavContainer.getModel().clear();
		this.mavContainer.setViewName(redirectUri);
		return new RedirectResponse(this.webRequest, this.mavContainer);
	}

}
