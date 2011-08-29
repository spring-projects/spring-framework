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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * A strategy interface for storing and retrieving {@code FlashMap} instances.
 * See {@link FlashMap} for a general overview of using flash attributes.
 * 
 * <p>A FlashMapManager is invoked at the beginning and at the end of a request.
 * For each request, it exposes an "input" FlashMap with attributes passed from
 * a previous request (if any) and an "output" FlashMap with attributes to pass 
 * to a subsequent request. Both FlashMap instances are exposed via request
 * attributes and can be accessed through methods in
 * {@code org.springframework.web.servlet.support.RequestContextUtils}.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 * 
 * @see FlashMap
 */
public interface FlashMapManager {

	/**
	 * Name of request attribute that holds a read-only {@link Map} with 
	 * "input" flash attributes from a previous request (if any).
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getInputFlashMap(HttpServletRequest)
	 */
	public static final String INPUT_FLASH_MAP_ATTRIBUTE = FlashMapManager.class.getName() + ".INPUT_FLASH_MAP";

	/**
	 * Name of request attribute that holds the "output" {@link FlashMap} with
	 * attributes to pass to a subsequent request.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getOutputFlashMap(HttpServletRequest)
	 */
	public static final String OUTPUT_FLASH_MAP_ATTRIBUTE = FlashMapManager.class.getName() + ".OUTPUT_FLASH_MAP";

	/**
	 * Performs the following tasks unless the {@link #OUTPUT_FLASH_MAP_ATTRIBUTE} 
	 * request attribute exists:
	 * <ol>
	 * 	<li>Find the "input" FlashMap from a previous request (if any), expose it 
	 * under the request attribute {@link #INPUT_FLASH_MAP_ATTRIBUTE}, and 
	 * remove it from underlying storage.
	 * 	<li>Create the "output" FlashMap where the current request can save 
	 * flash attributes and expose it under the request attribute 
	 * {@link #OUTPUT_FLASH_MAP_ATTRIBUTE}.
	 * 	<li>Remove expired FlashMap instances.
	 * </ol>
	 * 
	 * @param request the current request
	 */
	void requestStarted(HttpServletRequest request);

	/**
	 * Save the "output" FlashMap in underlying storage, start its expiration 
	 * period, and decode/normalize its target request path. 
	 * 
	 * <p>The "output" FlashMap is not saved if it is empty or if it was not 
	 * created by this FlashMapManager.
	 */
	void requestCompleted(HttpServletRequest request);

}
