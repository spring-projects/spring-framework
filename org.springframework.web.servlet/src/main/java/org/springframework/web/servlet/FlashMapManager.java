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
 * underlying storage between two requests. This is typically used when
 * redirecting from one URL to another.
 * 
 * TODO ...
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
	 */
	public static final String PREVIOUS_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".PREVIOUS_FLASH_MAP";

	/**
	 * Perform flash storage tasks at the start of a new request:
	 * <ul>
	 * 	<li>Create a new FlashMap and make it available to the current request 
	 * 	under the request attribute {@link #CURRENT_FLASH_MAP_ATTRIBUTE}.
	 * 	<li>Locate the FlashMap saved on the previous request and expose its 
	 * 	contents as attributes in the current request.
	 * 	<li>Remove expired flash map instances.
	 * </ul>
	 * 
	 * @param request the current request
	 * 
	 * @return "true" if flash storage tasks were performed; "false" otherwise
	 * if the {@link #CURRENT_FLASH_MAP_ATTRIBUTE} request attribute exists.
	 */
	boolean requestStarted(HttpServletRequest request);

	/**
	 * Access the current FlashMap through the {@link #CURRENT_FLASH_MAP_ATTRIBUTE}
	 * request attribute and if not empty, save it in the underlying storage. This 
	 * method should be invoked after {@link #requestStarted} and if it returned "true". 
	 */
	void requestCompleted(HttpServletRequest request);

}
