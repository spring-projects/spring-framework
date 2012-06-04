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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.servlet.FlashMap;

/**
 * Store and retrieve {@link FlashMap} instances to and from the HTTP session.
 *
 * @author Rossen Stoyanchev
 * @since 3.1.1
 */
public class SessionFlashMapManager extends AbstractFlashMapManager{

	private static final String FLASH_MAPS_SESSION_ATTRIBUTE = SessionFlashMapManager.class.getName() + ".FLASH_MAPS";

	/**
	 * Retrieve saved FlashMap instances from the HTTP Session.
	 * <p>Does not cause an HTTP session to be created but may update it if a
	 * FlashMap matching the current request is found or there are expired
	 * FlashMap to be removed.
	 */
	@SuppressWarnings("unchecked")
	protected List<FlashMap> retrieveFlashMaps(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		return (session != null) ? (List<FlashMap>) session.getAttribute(FLASH_MAPS_SESSION_ATTRIBUTE) : null;
	}

	/**
	 * Save the given FlashMap instance, if not empty, in the HTTP session.
	 */
	protected void updateFlashMaps(List<FlashMap> flashMaps, HttpServletRequest request, HttpServletResponse response) {
		request.getSession().setAttribute(FLASH_MAPS_SESSION_ATTRIBUTE, flashMaps);
	}

}
