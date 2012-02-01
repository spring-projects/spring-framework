/*
 * Copyright 2002-2012 the original author or authors.
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
import javax.servlet.http.HttpServletResponse;

/**
 * A strategy interface for retrieving and saving FlashMap instances.
 * See {@link FlashMap} for a general overview of flash attributes.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 * 
 * @see FlashMap
 */
public interface FlashMapManager {

	/**
	 * Get a Map with flash attributes saved by a previous request.
	 * See {@link FlashMap} for details on how FlashMap instances
	 * identifies the target requests they're saved for.
	 * If found, the Map is removed from the underlying storage.
	 * @param request the current request
	 * @return a read-only Map with flash attributes or {@code null}
	 */
	Map<String, ?> getFlashMapForRequest(HttpServletRequest request);

	/**
	 * Save the given FlashMap, in some underlying storage, mark the beginning
	 * of its expiration period, and remove other expired FlashMap instances.
	 * The method has no impact if the FlashMap is empty and there are no
	 * expired FlashMap instances to be removed.
	 * <p><strong>Note:</strong> Invoke this method prior to a redirect in order
	 * to allow saving the FlashMap in the HTTP session or perhaps in a response
	 * cookie before the response is committed.
	 * @param flashMap the FlashMap to save
	 * @param request the current request
	 * @param response the current response
	 */
	void save(FlashMap flashMap, HttpServletRequest request, HttpServletResponse response);

}
