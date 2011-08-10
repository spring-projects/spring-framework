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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * A strategy interface for maintaining {@link FlashMap} instances in some 
 * underlying storage until the next request. The most common use case is
 * a redirect. For example redirecting from a POST that creates a resource 
 * to the page that shows the created resource and passing along a 
 * success message that needs to be shown once only.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 * 
 * @see FlashMap
 */
public interface FlashMapManager {
	
	/**
	 * Request attribute to hold the current request FlashMap. 
	 * @see RequestContextUtils#getFlashMap
	 */
	public static final String CURRENT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".CURRENT_FLASH_MAP";
	
	/**
	 * Request attribute to hold the FlashMap from the previous request. 
	 * Access to the previous FlashMap should generally not be needed 
	 * since its content is exposed as attributes of the current 
	 * request. However, it may be useful to expose previous request
	 * flash attributes in other ways such as in the model of annotated 
	 * controllers.
	 */
	public static final String PREVIOUS_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".PREVIOUS_FLASH_MAP";

	/**
	 * Perform flash storage tasks at the start of a new request:
	 * <ul>
	 * 	<li>Create a new FlashMap and make it available to the current request 
	 * 	under the request attribute {@link #CURRENT_FLASH_MAP_ATTRIBUTE}.
	 * 	<li>Locate the FlashMap saved on the previous request and expose its 
	 * 	contents as attributes in the current request, also exposing the 
	 *  previous FlashMap under {@link #PREVIOUS_FLASH_MAP_ATTRIBUTE}.
	 * 	<li>Check for and remove expired FlashMap instances.
	 * </ul>
	 * 
	 * <p>If the {@link #CURRENT_FLASH_MAP_ATTRIBUTE} request attribute exists
	 * in the current request, this method should return "false" immediately.
	 * 
	 * @param request the current request
	 * 
	 * @return "true" if flash storage tasks were performed; "false" otherwise.
	 */
	boolean requestStarted(HttpServletRequest request);

	/**
	 * Access the current FlashMap through the request attribute
	 * {@link #CURRENT_FLASH_MAP_ATTRIBUTE} and if it is not empty, save it 
	 * in the underlying storage. 
	 * 
	 * <p>If the call to {@link #requestStarted} returned "false", this 
	 * method is not invoked.
	 */
	void requestCompleted(HttpServletRequest request);

}
