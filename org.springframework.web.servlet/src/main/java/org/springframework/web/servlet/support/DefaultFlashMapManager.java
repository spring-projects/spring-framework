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

package org.springframework.web.servlet.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.util.UrlPathHelper;

/**
 * A default {@link FlashMapManager} implementation that stores {@link FlashMap}
 * instances in the HTTP session.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class DefaultFlashMapManager implements FlashMapManager {

	private static final String FLASH_MAPS_SESSION_ATTRIBUTE = DefaultFlashMapManager.class.getName() + ".FLASH_MAPS";

	private static final Log logger = LogFactory.getLog(DefaultFlashMapManager.class);
	
	private int flashTimeout = 180;

	private final UrlPathHelper urlPathHelper = new UrlPathHelper();

	/**
	 * Set the amount of time in seconds after a {@link FlashMap} is saved 
	 * (at request completion) and before it expires. 
	 * <p>The default value is 180 seconds.
	 */
	public void setFlashMapTimeout(int flashTimeout) {
		this.flashTimeout = flashTimeout;
	}

	/**
	 * {@inheritDoc}
	 * <p>An HTTP session is never created by this method.
	 */
	public final void requestStarted(HttpServletRequest request, HttpServletResponse response) {
		if (request.getAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE) != null) {
			return;
		}
		
		FlashMap inputFlashMap = lookupFlashMap(request);
		if (inputFlashMap != null) {
			request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Collections.unmodifiableMap(inputFlashMap));
		}

		FlashMap outputFlashMap = new FlashMap(this.hashCode());
		request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, outputFlashMap);

		removeExpiredFlashMaps(request);
	}

	/**
	 * Find the "input" FlashMap for the current request target by matching it
	 * to the target request information of all stored FlashMap instances.
	 * @return a FlashMap instance or {@code null}
  	 */
	private FlashMap lookupFlashMap(HttpServletRequest request) {
		List<FlashMap> allFlashMaps = retrieveFlashMaps(request, false);
		if (CollectionUtils.isEmpty(allFlashMaps)) {
			return null;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Retrieved FlashMap(s): " + allFlashMaps);
		}
		List<FlashMap> result = new ArrayList<FlashMap>();
		for (FlashMap flashMap : allFlashMaps) {
			if (isFlashMapForRequest(flashMap, request)) {
				result.add(flashMap);
			}
		}
		if (!result.isEmpty()) {
			Collections.sort(result);
			if (logger.isDebugEnabled()) {
				logger.debug("Found matching FlashMap(s): " + result);
			}
			FlashMap match = result.remove(0);
			allFlashMaps.remove(match);
			return match;
		}
		return null;
	}

	/**
	 * Whether the given FlashMap matches the current request.
	 * The default implementation uses the target request path and query params 
	 * saved in the FlashMap.
	 */
	protected boolean isFlashMapForRequest(FlashMap flashMap, HttpServletRequest request) {
		if (flashMap.getTargetRequestPath() != null) {
			String requestUri = this.urlPathHelper.getOriginatingRequestUri(request);
			if (!requestUri.equals(flashMap.getTargetRequestPath())
					&& !requestUri.equals(flashMap.getTargetRequestPath() + "/")) {
				return false;
			}
		}
		MultiValueMap<String, String> targetParams = flashMap.getTargetRequestParams();
		for (String paramName : targetParams.keySet()) {
			for (String targetValue : targetParams.get(paramName)) {
				if (!ObjectUtils.containsElement(request.getParameterValues(paramName), targetValue)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Retrieve all FlashMap instances from the current HTTP session.
	 * If {@code allowCreate} is "true" and no flash maps exist yet, a new list
	 * is created and stored as a session attribute.
	 * @param request the current request
	 * @param allowCreate whether to create the session if necessary
	 * @return a List to add FlashMap instances to or {@code null} 
	 * 	assuming {@code allowCreate} is "false".
	 */
	@SuppressWarnings("unchecked")
	protected List<FlashMap> retrieveFlashMaps(HttpServletRequest request, boolean allowCreate) {
		HttpSession session = request.getSession(allowCreate);
		if (session == null) {
			return null;
		}
		List<FlashMap> allFlashMaps = (List<FlashMap>) session.getAttribute(FLASH_MAPS_SESSION_ATTRIBUTE);
		if (allFlashMaps == null && allowCreate) {
			synchronized (this) {
				allFlashMaps = (List<FlashMap>) session.getAttribute(FLASH_MAPS_SESSION_ATTRIBUTE);
				if (allFlashMaps == null) {
					allFlashMaps = new CopyOnWriteArrayList<FlashMap>();
					session.setAttribute(FLASH_MAPS_SESSION_ATTRIBUTE, allFlashMaps);
				}
			}
		}
		return allFlashMaps;
	}
	
	/**
	 * Check and remove expired FlashMaps instances.
	 */
	protected void removeExpiredFlashMaps(HttpServletRequest request) {
		List<FlashMap> allMaps = retrieveFlashMaps(request, false);
		if (CollectionUtils.isEmpty(allMaps)) {
			return;
		}
		List<FlashMap> expiredMaps = new ArrayList<FlashMap>();
		for (FlashMap flashMap : allMaps) {
			if (flashMap.isExpired()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Removing expired FlashMap: " + flashMap);
				}
				expiredMaps.add(flashMap);
			}
		}
		if (!expiredMaps.isEmpty()) {
			allMaps.removeAll(expiredMaps);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>An HTTP session is never created if the "output" FlashMap is empty.
	 */
	public void requestCompleted(HttpServletRequest request, HttpServletResponse response) {
		FlashMap flashMap = (FlashMap) request.getAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE);
		if (flashMap == null) {
			throw new IllegalStateException("requestCompleted called but \"output\" FlashMap was never created");
		}
		if (!flashMap.isEmpty() && flashMap.isCreatedBy(this.hashCode())) {
			if (logger.isDebugEnabled()) {
				logger.debug("Saving FlashMap=" + flashMap);
			}
			onSaveFlashMap(flashMap, request, response);
			saveFlashMap(flashMap, request, response);
		}
	}
	
	/**
	 * Update a FlashMap before it is stored in the underlying storage.
	 * <p>The default implementation starts the expiration period and ensures the
	 * target request path is decoded and normalized if it is relative. 
	 * @param flashMap the flash map to be saved
	 * @param request the current request
	 * @param response the current response
	 */
	protected void onSaveFlashMap(FlashMap flashMap, HttpServletRequest request, HttpServletResponse response) {
		String targetPath = flashMap.getTargetRequestPath();
		flashMap.setTargetRequestPath(decodeAndNormalizePath(targetPath, request));
		flashMap.startExpirationPeriod(this.flashTimeout);
	}

	/**
	 * Save the FlashMap in the underlying storage.
	 * @param flashMap the FlashMap to save
	 * @param request the current request
	 * @param response the current response
	 */
	protected void saveFlashMap(FlashMap flashMap, HttpServletRequest request, HttpServletResponse response) {
		retrieveFlashMaps(request, true).add(flashMap);
	}
	
	private String decodeAndNormalizePath(String path, HttpServletRequest request) {
		if (path != null) {
			path = this.urlPathHelper.decodeRequestString(request, path);
			if (path.charAt(0) != '/') {
				String requestUri = this.urlPathHelper.getRequestUri(request);
				path = requestUri.substring(0, requestUri.lastIndexOf('/') + 1) + path;
				path = StringUtils.cleanPath(path);
			}
		}
		return path;
	}

}
