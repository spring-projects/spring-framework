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
import java.util.Map;
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

	/**
	 * {@inheritDoc}
	 * <p>Does not cause an HTTP session to be created.
	 */
	public final Map<String, ?> getFlashMapForRequest(HttpServletRequest request) {
		List<FlashMap> flashMaps = retrieveFlashMaps(request);
		if (CollectionUtils.isEmpty(flashMaps)) {
			return null;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Retrieved FlashMap(s): " + flashMaps);
		}
		List<FlashMap> result = new ArrayList<FlashMap>();
		for (FlashMap flashMap : flashMaps) {
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
			flashMaps.remove(match);
			return Collections.unmodifiableMap(match);
		}
		return null;
	}

	/**
	 * Retrieve saved FlashMap instances from underlying storage.
	 * @param request the current request
	 * @return a List with FlashMap instances or {@code null}
	 */
	protected abstract List<FlashMap> retrieveFlashMaps(HttpServletRequest request);

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
	 * {@inheritDoc}
	 * <p>The FlashMap, if not empty, is saved to the HTTP session.
	 */
	public final void save(FlashMap flashMap, HttpServletRequest request, HttpServletResponse response) {
		Assert.notNull(flashMap, "FlashMap must not be null");

		List<FlashMap> flashMaps = retrieveFlashMaps(request);
		if (flashMap.isEmpty() && (flashMaps == null)) {
			return;
		}
		synchronized (this) {
			boolean update = false;
			flashMaps = retrieveFlashMaps(request);
			if (!CollectionUtils.isEmpty(flashMaps)) {
				update = removeExpired(flashMaps);
			}
			if (!flashMap.isEmpty()) {
				String path = decodeAndNormalizePath(flashMap.getTargetRequestPath(), request);
				flashMap.setTargetRequestPath(path);
				flashMap.startExpirationPeriod(this.flashMapTimeout);
				if (logger.isDebugEnabled()) {
					logger.debug("Saving FlashMap=" + flashMap);
				}
				flashMaps = (flashMaps == null) ? new CopyOnWriteArrayList<FlashMap>() : flashMaps;
				flashMaps.add(flashMap);
				update = true;
			}
			if (update) {
				updateFlashMaps(flashMaps, request, response);
			}
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

	/**
	 * Remove expired FlashMap instances from the given List.
	 */
	protected boolean removeExpired(List<FlashMap> flashMaps) {
		List<FlashMap> expired = new ArrayList<FlashMap>();
		for (FlashMap flashMap : flashMaps) {
			if (flashMap.isExpired()) {
				if (logger.isTraceEnabled()) {
					logger.trace("Removing expired FlashMap: " + flashMap);
				}
				expired.add(flashMap);
			}
		}
		if (expired.isEmpty()) {
			return false;
		}
		else {
			return flashMaps.removeAll(expired);
		}
	}

}
