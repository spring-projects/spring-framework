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
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.util.WebUtils;

/**
 * A {@link FlashMapManager} that saves and retrieves FlashMap instances in the 
 * HTTP session.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class DefaultFlashMapManager implements FlashMapManager {

	private static final String FLASH_MAPS_SESSION_ATTRIBUTE = DefaultFlashMapManager.class + ".FLASH_MAPS";

	private static final Log logger = LogFactory.getLog(DefaultFlashMapManager.class);
	
	private int flashTimeout = 180;

	/**
	 * The amount of time in seconds after a request has completed processing 
	 * and before a FlashMap is considered expired.
	 * The default value is 180.
	 */
	public void setFlashMapTimeout(int flashTimeout) {
		this.flashTimeout = flashTimeout;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>This method never creates an HTTP session. The current FlashMap is 
	 * exposed as a request attribute only and is not saved in the session 
	 * until {@link #requestCompleted}.
	 */
	public boolean requestStarted(HttpServletRequest request) {
		if (request.getAttribute(CURRENT_FLASH_MAP_ATTRIBUTE) != null) {
			return false;
		}

		FlashMap currentFlashMap = new FlashMap();
		request.setAttribute(CURRENT_FLASH_MAP_ATTRIBUTE, currentFlashMap);
		
		FlashMap previousFlashMap = lookupPreviousFlashMap(request);
		if (previousFlashMap != null) {
			WebUtils.exposeRequestAttributes(request, previousFlashMap);
			request.setAttribute(PREVIOUS_FLASH_MAP_ATTRIBUTE, previousFlashMap);
		}
		
		// Remove expired flash maps
		List<FlashMap> allMaps = retrieveFlashMaps(request, false);
		if (allMaps != null && !allMaps.isEmpty()) {
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
		
		return true;
	}

	/**
	 * Return the FlashMap from the previous request.
	 * 
	 * @return the FlashMap from the previous request; or {@code null} if none.
	 */
	private FlashMap lookupPreviousFlashMap(HttpServletRequest request) {
		List<FlashMap> allMaps = retrieveFlashMaps(request, false);
		if (CollectionUtils.isEmpty(allMaps)) {
			return null;
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("Looking up previous FlashMap among available FlashMaps: " + allMaps);
		}
		
		List<FlashMap> matches = new ArrayList<FlashMap>();
		for (FlashMap flashMap : allMaps) {
			if (flashMap.matches(request)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Matched " + flashMap);
				}
				matches.add(flashMap);
			}
		}
		
		if (!matches.isEmpty()) {
			Collections.sort(matches);
			return matches.remove(0);
		}
		
		return null;
	}

	/**
	 * Retrieve the list of all FlashMap instances from the HTTP session. 
	 * @param request the current request
	 * @param allowCreate whether to create and the FlashMap container if not found
	 * @return a Map with all stored FlashMap instances; or {@code null}
	 */
	@SuppressWarnings("unchecked")
	private List<FlashMap> retrieveFlashMaps(HttpServletRequest request, boolean allowCreate) {
		HttpSession session = request.getSession(allowCreate);
		if (session == null) {
			return null;
		} 
		
		List<FlashMap> allMaps = (List<FlashMap>) session.getAttribute(FLASH_MAPS_SESSION_ATTRIBUTE);
		if (allMaps == null && allowCreate) {
			synchronized (DefaultFlashMapManager.class) {
				allMaps = (List<FlashMap>) session.getAttribute(FLASH_MAPS_SESSION_ATTRIBUTE);
				if (allMaps == null) {
					allMaps = new CopyOnWriteArrayList<FlashMap>();
					session.setAttribute(FLASH_MAPS_SESSION_ATTRIBUTE, allMaps);
				}
			}
		}
		
		return allMaps;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>The HTTP session is not created if the current FlashMap instance is empty.
	 */
	public void requestCompleted(HttpServletRequest request) {
		FlashMap flashMap = (FlashMap) request.getAttribute(CURRENT_FLASH_MAP_ATTRIBUTE);
		if (flashMap == null) {
			throw new IllegalStateException(
					"Did not find a FlashMap exposed as the request attribute " + CURRENT_FLASH_MAP_ATTRIBUTE);
		}
		
		if (!flashMap.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Saving FlashMap=" + flashMap);
			}
			List<FlashMap> allFlashMaps = retrieveFlashMaps(request, true);
			flashMap.startExpirationPeriod(this.flashTimeout);
			allFlashMaps.add(flashMap);
		}
	}

}
