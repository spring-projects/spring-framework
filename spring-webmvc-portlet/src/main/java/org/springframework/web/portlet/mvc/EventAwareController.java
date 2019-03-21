/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.portlet.mvc;

import javax.portlet.EventRequest;
import javax.portlet.EventResponse;

/**
 * Extension of the Portlet {@link Controller} interface that allows
 * for handling Portlet 2.0 event requests as well. Can also be
 * implemented by {@link AbstractController} subclasses.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see javax.portlet.EventPortlet
 * @see Controller
 * @see ResourceAwareController
 */
public interface EventAwareController {

	/**
	 * Process the event request. There is nothing to return.
	 * @param request current portlet event request
	 * @param response current portlet event response
	 * @throws Exception in case of errors
	 */
	void handleEventRequest(EventRequest request, EventResponse response) throws Exception;

}
