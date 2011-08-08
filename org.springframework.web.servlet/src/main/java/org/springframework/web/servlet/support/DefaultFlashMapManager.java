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

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;

/**
 * A default implementation that saves and retrieves FlashMap instances to and 
 * from the HTTP session.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class DefaultFlashMapManager implements FlashMapManager {

	static final String FLASH_MAPS_SESSION_ATTRIBUTE = DefaultFlashMapManager.class + ".FLASH_MAPS";

	private boolean useUniqueFlashKey = true;
	
	private String flashKeyParameterName = "_flashKey";
	
	private int flashTimeout = 180;
	
	private static final Random random = new Random();

	/**
	 * Whether each FlashMap instance should be stored with a unique key.
	 * The unique key needs to be passed as a parameter in the redirect URL 
	 * and then used to look up the FlashMap instance avoiding potential 
	 * issues with concurrent requests.
	 * <p>The default setting is "true".
	 * <p>When set to "false" only one FlashMap is maintained making it
	 * possible for a second concurrent request (e.g. via Ajax) to "consume" 
	 * the FlashMap inadvertently.
	 */
	public void setUseUniqueFlashKey(boolean useUniqueFlashKey) {
		this.useUniqueFlashKey = useUniqueFlashKey;
	}

	/**
	 * Customize the name of the request parameter to be appended to the 
	 * redirect URL when using a unique flash key.
	 * <p>The default value is "_flashKey".
	 */
	public void setFlashKeyParameterName(String parameterName) {
		this.flashKeyParameterName = parameterName;
	}

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

		FlashMap currentFlashMap = 
			this.useUniqueFlashKey ?
				new FlashMap(createFlashKey(request), this.flashKeyParameterName) : new FlashMap();
		request.setAttribute(CURRENT_FLASH_MAP_ATTRIBUTE, currentFlashMap);
		
		FlashMap previousFlashMap = lookupPreviousFlashMap(request);
		if (previousFlashMap != null) {
			for (String name : previousFlashMap.keySet()) {
				if (request.getAttribute(name) == null) {
					request.setAttribute(name, previousFlashMap.get(name));
				}
			}
			// For exposing flash attributes in other places (e.g. annotated controllers)
			request.setAttribute(PREVIOUS_FLASH_MAP_ATTRIBUTE, previousFlashMap);
		}
		
		// Check and remove expired instances
		Map<String, FlashMap> allFlashMaps = retrieveAllFlashMaps(request, false);
		if (allFlashMaps != null && !allFlashMaps.isEmpty()) {
			Iterator<FlashMap> iterator = allFlashMaps.values().iterator();
			while (iterator.hasNext()) {
				if (iterator.next().isExpired()) {
					iterator.remove();
				}
			}
		}
		
		return true;
	}

	/**
	 * Create a unique flash key. The default implementation uses {@link Random}.
	 * @return the unique key; never {@code null}.
	 */
	protected String createFlashKey(HttpServletRequest request) {
		return String.valueOf(random.nextInt());
	}
	
	/**
	 * Return the FlashMap from the previous request, if available. 
	 * If {@link #useUniqueFlashKey} is "true", a flash key parameter is 
	 * expected to be in the request. Otherwise there can be only one  
	 * FlashMap instance to return.
	 * 
	 * @return the FlashMap from the previous request; or {@code null} if none.
	 */
	private FlashMap lookupPreviousFlashMap(HttpServletRequest request) {
		Map<String, FlashMap> flashMaps = retrieveAllFlashMaps(request, false);
		if (flashMaps != null && !flashMaps.isEmpty()) {
			if (this.useUniqueFlashKey) {
				String key = request.getParameter(this.flashKeyParameterName);
				if (key != null) {
					return flashMaps.remove(key);
				}
			}
			else {
				String key = flashMaps.keySet().iterator().next();
				return flashMaps.remove(key);
			}
		}
		return null;
	}

	/**
	 * Retrieve all FlashMap instances from the HTTP session in a thread-safe way. 
	 * @param request the current request
	 * @param allowCreate whether to create and the FlashMap container if not found
	 * @return a Map with all stored FlashMap instances; or {@code null}
	 */
	@SuppressWarnings("unchecked")
	private Map<String, FlashMap> retrieveAllFlashMaps(HttpServletRequest request, boolean allowCreate) {
		HttpSession session = request.getSession(allowCreate);
		if (session == null) {
			return null;
		} 
		Map<String, FlashMap> result = (Map<String, FlashMap>) session.getAttribute(FLASH_MAPS_SESSION_ATTRIBUTE);
		if (result == null && allowCreate) {
			synchronized (DefaultFlashMapManager.class) {
				result = (Map<String, FlashMap>) session.getAttribute(FLASH_MAPS_SESSION_ATTRIBUTE);
				if (result == null) {
					result = new ConcurrentHashMap<String, FlashMap>(5);
					session.setAttribute(FLASH_MAPS_SESSION_ATTRIBUTE, result);
				}
			}
		}
		return result;
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
					"Did not find current FlashMap exposed as request attribute " + CURRENT_FLASH_MAP_ATTRIBUTE);
		}
		if (!flashMap.isEmpty()) {
			Map<String, FlashMap> allFlashMaps = retrieveAllFlashMaps(request, true);
			flashMap.startExpirationPeriod(this.flashTimeout);
			String key = this.useUniqueFlashKey ? flashMap.getKey() : "flashMap";
			allFlashMaps.put(key, flashMap);
		}
	}

}
