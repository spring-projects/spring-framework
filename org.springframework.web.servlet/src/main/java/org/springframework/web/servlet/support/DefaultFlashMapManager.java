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
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.util.UrlPathHelper;

/**
 * A {@link FlashMapManager} that stores FlashMap instances in the HTTP session.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class DefaultFlashMapManager implements FlashMapManager {

	private static final String FLASH_MAPS_SESSION_ATTRIBUTE = DefaultFlashMapManager.class + ".FLASH_MAPS";

	private static final Log logger = LogFactory.getLog(DefaultFlashMapManager.class);
	
	private int flashTimeout = 180;

	private final UrlPathHelper urlPathHelper = new UrlPathHelper();

	/**
	 * The amount of time in seconds after a FlashMap is saved (after request
	 * completion) before it is considered expired. The default value is 180.
	 */
	public void setFlashMapTimeout(int flashTimeout) {
		this.flashTimeout = flashTimeout;
	}

	/**
	 * {@inheritDoc}
	 * <p>This method never causes the HTTP session to be created.
	 */
	public void requestStarted(HttpServletRequest request) {
		if (request.getAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE) != null) {
			return;
		}
		
		Map<String, ?> inputFlashMap = lookupFlashMap(request);
		if (inputFlashMap != null) {
			request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, inputFlashMap);
		}

		FlashMap outputFlashMap = new FlashMap(this.hashCode());
		request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, outputFlashMap);

		removeExpiredFlashMaps(request);
	}

	/**
	 * Look up the "input" FlashMap by matching the target request path and 
	 * the target request parameters configured in each available FlashMap
	 * to the current request.
	 */
	private Map<String, ?> lookupFlashMap(HttpServletRequest request) {
		List<FlashMap> allFlashMaps = retrieveFlashMaps(request, false);
		if (CollectionUtils.isEmpty(allFlashMaps)) {
			return null;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Retrieved flash maps: " + allFlashMaps);
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
				logger.debug("Matching flash maps: " + result);
			}
			FlashMap match = result.remove(0);
			allFlashMaps.remove(match);
			return Collections.unmodifiableMap(match);
		}
		return null;
	}

	/**
	 * Compares the target request path and the target request parameters in the
	 * given FlashMap and returns "true" if they match. If the FlashMap does not
	 * have target request information, it matches any request.
	 */
	protected boolean isFlashMapForRequest(FlashMap flashMap, HttpServletRequest request) {
		if (flashMap.getTargetRequestPath() != null) {
			String requestUri = this.urlPathHelper.getRequestUri(request);
			if (!requestUri.equals(flashMap.getTargetRequestPath())
					&& !requestUri.equals(flashMap.getTargetRequestPath() + "/")) {
				return false;
			}
		}
		if (flashMap.getTargetRequestParams() != null) {
			for (Map.Entry<String, String> entry : flashMap.getTargetRequestParams().entrySet()) {
				if (!entry.getValue().equals(request.getParameter(entry.getKey()))) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Retrieve all available FlashMap instances from the HTTP session. 
	 * @param request the current request
	 * @param allowCreate whether to create and save the FlashMap in the session
	 * @return a Map with all FlashMap instances; or {@code null}
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
	 * Iterate available FlashMap instances and remove the ones that have expired.
	 */
	private void removeExpiredFlashMaps(HttpServletRequest request) {
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
		allMaps.removeAll(expiredMaps);
	}
	
	public void requestCompleted(HttpServletRequest request) {
		FlashMap flashMap = (FlashMap) request.getAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE);
		if (flashMap == null) {
			throw new IllegalStateException(
					"Did not find a FlashMap exposed as the request attribute " + OUTPUT_FLASH_MAP_ATTRIBUTE);
		}
		if (!flashMap.isEmpty() && flashMap.isCreatedBy(this.hashCode())) {
			if (logger.isDebugEnabled()) {
				logger.debug("Saving FlashMap=" + flashMap);
			}
			decodeAndNormalizeTargetPath(flashMap, request);
			flashMap.startExpirationPeriod(this.flashTimeout);
			retrieveFlashMaps(request, true).add(flashMap);
		}
	}

	/**
	 * Ensure the target request path in the given FlashMap is decoded and also 
	 * normalized (if it is relative) against the current request URL. 
	 */
	private void decodeAndNormalizeTargetPath(FlashMap flashMap, HttpServletRequest request) {
		String path = flashMap.getTargetRequestPath();
		if (path != null) {
			path = urlPathHelper.decodeRequestString(request, path);
			if (path.charAt(0) != '/') {
				String requestUri = this.urlPathHelper.getRequestUri(request);
				path = requestUri.substring(0, requestUri.lastIndexOf('/') + 1) + path;
				path = StringUtils.cleanPath(path);
			}
			flashMap.setTargetRequestPath(path);
		}
	}

}
