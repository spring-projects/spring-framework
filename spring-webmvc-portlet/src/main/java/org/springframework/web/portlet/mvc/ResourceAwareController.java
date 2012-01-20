/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.portlet.mvc;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.springframework.web.portlet.ModelAndView;

/**
 * Extension of the Portlet {@link Controller} interface that allows
 * for handling Portlet 2.0 resource requests as well. Can also be
 * implemented by {@link AbstractController} subclasses.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see javax.portlet.ResourceServingPortlet
 * @see Controller
 * @see EventAwareController
 */
public interface ResourceAwareController {

	/**
	 * Process the resource request and return a ModelAndView object which the DispatcherPortlet
	 * will render. A <code>null</code> return value is not an error: It indicates that this
	 * object completed request processing itself, thus there is no ModelAndView to render.
	 * @param request current portlet resource request
	 * @param response current portlet resource response
	 * @return a ModelAndView to render, or null if handled directly
	 * @throws Exception in case of errors
	 */
	ModelAndView handleResourceRequest(ResourceRequest request, ResourceResponse response) throws Exception;

}
