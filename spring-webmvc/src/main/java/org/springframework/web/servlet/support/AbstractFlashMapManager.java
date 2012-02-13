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

package org.springframework.web.servlet.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.util.UrlPathHelper;

/**
 * A base class for {@link FlashMapManager} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 3.1.1
 */
public abstract class AbstractFlashMapManager implements FlashMapManager {

	protected final Log logger = LogFactory.getLog(getClass());

	private int flashMapTimeout = 180;

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	private static final Object writeLock = new Object();

	/**
	 * Set the amount of time in seconds after a {@link FlashMap} is saved
	 * (at request completion) and before it expires.
	 * <p>The default value is 180 seconds.
	 */
	public void setFlashMapTimeout(int flashMapTimeout) {
		this.flashMapTimeout = flashMapTimeout;
	}

	/**
	 * Return the amount of time in seconds before a FlashMap expires.
	 */
	public int getFlashMapTimeout() {
		return this.flashMapTimeout;
	}

	/**
	 * Set the UrlPathHelper to use to match FlashMap instances to requests.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * Return the UrlPathHelper implementation to use.
	 */
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	public final FlashMap retrieveAndUpdate(HttpServletRequest request, HttpServletResponse response) {
		List<FlashMap> allMaps = retrieveFlashMaps(request);
		if (CollectionUtils.isEmpty(allMaps)) {
			return null;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Retrieved FlashMap(s): " + allMaps);
		}
		List<FlashMap> mapsToRemove = getExpiredFlashMaps(allMaps);
		FlashMap match = getMatchingFlashMap(allMaps, request);
		if (match != null) {
			mapsToRemove.add(match);
		}
		if (!mapsToRemove.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Removing FlashMap(s): " + allMaps);
			}
			synchronized (writeLock) {
				allMaps = retrieveFlashMaps(request);
				allMaps.removeAll(mapsToRemove);
				updateFlashMaps(allMaps, request, response);
			}
		}
		return match;
	}

	/**
	 * Retrieve saved FlashMap instances from underlying storage.
	 * @param request the current request
	 * @return a List with FlashMap instances or {@code null}
	 */
	protected abstract List<FlashMap> retrieveFlashMaps(HttpServletRequest request);

	/**
	 * Return a list of expired FlashMap instances contained in the given list.
	 */
	private List<FlashMap> getExpiredFlashMaps(List<FlashMap> allMaps) {
		List<FlashMap> result = new ArrayList<FlashMap>();
		for (FlashMap map : allMaps) {
			if (map.isExpired()) {
				result.add(map);
			}
		}
		return result;
	}

	/**
	 * Return a FlashMap contained in the given list that matches the request.
	 * @return a matching FlashMap or {@code null}
	 */
	private FlashMap getMatchingFlashMap(List<FlashMap> allMaps, HttpServletRequest request) {
		List<FlashMap> result = new ArrayList<FlashMap>();
		for (FlashMap flashMap : allMaps) {
			if (isFlashMapForRequest(flashMap, request)) {
				result.add(flashMap);
			}
		}
		if (!result.isEmpty()) {
			Collections.sort(result);
			if (logger.isDebugEnabled()) {
				logger.debug("Found matching FlashMap(s): " + result);
			}
			return result.get(0);
		}
		return null;
	}

	/**
	 * Whether the given FlashMap matches the current request.
	 * The default implementation uses the target request path and query
	 * parameters saved in the FlashMap.
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

	public final void saveOutputFlashMap(FlashMap flashMap, HttpServletRequest request, HttpServletResponse response) {
		if (CollectionUtils.isEmpty(flashMap)) {
			return;
		}
		String path = decodeAndNormalizePath(flashMap.getTargetRequestPath(), request);
		flashMap.setTargetRequestPath(path);
		flashMap.startExpirationPeriod(this.flashMapTimeout);
		if (logger.isDebugEnabled()) {
			logger.debug("Saving FlashMap=" + flashMap);
		}
		synchronized (writeLock) {
			List<FlashMap> allMaps = retrieveFlashMaps(request);
			allMaps = (allMaps == null) ? new CopyOnWriteArrayList<FlashMap>() : allMaps;
			allMaps.add(flashMap);
			updateFlashMaps(allMaps, request, response);
		}
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

	/**
	 * Update the FlashMap instances in some underlying storage.
	 * @param flashMaps a non-empty list of FlashMap instances to save
	 * @param request the current request
	 * @param response the current response
	 */
	protected abstract void updateFlashMaps(List<FlashMap> flashMaps, HttpServletRequest request,
			HttpServletResponse response);

}
