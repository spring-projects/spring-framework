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
 * underlying storage until the next request. 
 * 
 * <p>The most common use case for using flash storage is a redirect. 
 * For example creating a resource in a POST request and then redirecting
 * to the page that shows the resource. Flash storage may be used to 
 * pass along a success message.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 * 
 * @see FlashMap
 */
public interface FlashMapManager {

	/**
	 * Request attribute holding the read-only Map with flash attributes saved 
	 * during the previous request.
	 * @see RequestContextUtils#getInputFlashMap(HttpServletRequest)
	 */
	public static final String INPUT_FLASH_MAP_ATTRIBUTE = FlashMapManager.class.getName() + ".INPUT_FLASH_MAP";

	/**
	 * Request attribute holding the {@link FlashMap} to add attributes to during 
	 * the current request.
	 * @see RequestContextUtils#getOutputFlashMap(HttpServletRequest)
	 */
	public static final String OUTPUT_FLASH_MAP_ATTRIBUTE = FlashMapManager.class.getName() + ".OUTPUT_FLASH_MAP";

	/**
	 * Perform flash storage tasks at the start of a new request:
	 * <ul>
	 * 	<li>Create a FlashMap and make it available under the request attribute 
	 * 	{@link #OUTPUT_FLASH_MAP_ATTRIBUTE}.
	 * 	<li>Locate the FlashMap saved during the previous request and make it
	 * 	available under the request attribute {@link #INPUT_FLASH_MAP_ATTRIBUTE}.
	 * 	<li>Remove expired FlashMap instances.
	 * </ul>
	 * <p>If the {@link #OUTPUT_FLASH_MAP_ATTRIBUTE} request attribute exists
	 * return "false" immediately.
	 * 
	 * @param request the current request
	 * @return "true" if flash storage tasks were performed; "false" otherwise.
	 */
	boolean requestStarted(HttpServletRequest request);

	/**
	 * Access the FlashMap with attributes added during the current request and
	 * if it is not empty, save it in the underlying storage. 
	 * <p>If the call to {@link #requestStarted} returned "false", this 
	 * method is not invoked.
	 */
	void requestCompleted(HttpServletRequest request);

}
