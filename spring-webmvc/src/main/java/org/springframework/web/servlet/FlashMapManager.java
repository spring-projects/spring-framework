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
import javax.servlet.http.HttpServletResponse;

/**
 * A strategy interface for storing, retrieving, and managing {@code FlashMap}
 * instances. See {@link FlashMap} for a general overview of flash attributes.
 * 
 * <p>A FlashMapManager is invoked at the beginning and at the end of requests.
 * For each request it retrieves an "input" FlashMap with attributes passed 
 * from a previous request (if any) and creates an "output" FlashMap with 
 * attributes to pass to a subsequent request. "Input" and "output" FlashMap 
 * instances are exposed as request attributes and are accessible via methods
 * in {@code org.springframework.web.servlet.support.RequestContextUtils}.
 * 
 * <p>Annotated controllers will usually not use this FlashMap directly.
 * See {@code org.springframework.web.servlet.mvc.support.RedirectAttributes}.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 * 
 * @see FlashMap
 */
public interface FlashMapManager {

	/**
	 * Name of request attribute that holds a read-only 
	 * {@code Map<String, Object>} with "input" flash attributes if any.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getInputFlashMap(HttpServletRequest)
	 */
	String INPUT_FLASH_MAP_ATTRIBUTE = FlashMapManager.class.getName() + ".INPUT_FLASH_MAP";

	/**
	 * Name of request attribute that holds the "output" {@link FlashMap} with
	 * attributes to save for a subsequent request.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getOutputFlashMap(HttpServletRequest)
	 */
	String OUTPUT_FLASH_MAP_ATTRIBUTE = FlashMapManager.class.getName() + ".OUTPUT_FLASH_MAP";

	/**
	 * Perform the following tasks unless the {@link #OUTPUT_FLASH_MAP_ATTRIBUTE} 
	 * request attribute exists:
	 * <ol>
	 * 	<li>Find the "input" FlashMap, expose it under the request attribute 
	 * {@link #INPUT_FLASH_MAP_ATTRIBUTE}, and remove it from underlying storage.
	 * 	<li>Create the "output" FlashMap and expose it under the request 
	 * attribute {@link #OUTPUT_FLASH_MAP_ATTRIBUTE}.
	 * 	<li>Clean expired FlashMap instances.
	 * </ol>
	 * @param request the current request
	 * @param response the current response
	 */
	void requestStarted(HttpServletRequest request, HttpServletResponse response);

	/**
	 * Start the expiration period of the "output" FlashMap save it in the
	 * underlying storage.
	 * <p>The "output" FlashMap should not be saved if it is empty or if it was
	 * not created by the current FlashMapManager instance.
	 * @param request the current request
	 * @param response the current response
	 */
	void requestCompleted(HttpServletRequest request, HttpServletResponse response);

}
