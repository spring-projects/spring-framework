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

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.springframework.web.portlet.ModelAndView;

/**
 * Base portlet Controller interface, representing a component that receives 
 * RenderRequest/RenderResponse and ActionRequest/ActionResponse like a 
 * <code>Portlet</code> but is able to participate in an MVC workflow.
 *
 * <p>Any implementation of the portlet Controller interface should be a
 * <i>reusable, threadsafe</i> class, capable of handling multiple
 * portlet requests throughout the lifecycle of an application. To be able to
 * configure Controller(s) in an easy way, Controllers are usually JavaBeans.</p>
 *
 * <p><b><a name="workflow">Workflow</a>:</b></p>
 *
 * <p>After the DispatcherPortlet has received a request and has done its work
 * to resolve locales, themes and suchlike, it tries to resolve a
 * Controller to handle that request, using a
 * {@link org.springframework.web.portlet.HandlerMapping HandlerMapping}.
 * When a Controller has been found, the
 * {@link #handleRenderRequest handleRenderRequest} or {@link #handleActionRequest handleActionRequest}
 * method will be invoked, which is responsible for handling the actual
 * request and - if applicable - returning an appropriate ModelAndView.
 * So actually, these method are the main entrypoint for the
 * {@link org.springframework.web.portlet.DispatcherPortlet DispatcherPortlet}
 * which delegates requests to controllers.</p>
 * 
 * <p>So basically any <i>direct</i> implementation of the Controller interface
 * just handles RenderRequests/ActionRequests and should return a ModelAndView, to be
 * further used by the DispatcherPortlet. Any additional functionality such as
 * optional validation, form handling, etc should be obtained through extending
 * one of the abstract controller classes mentioned above.</p>
 *
 * @author William G. Thompson, Jr.
 * @author John A. Lewis
 * @since 2.0
 * @see ResourceAwareController
 * @see EventAwareController
 * @see SimpleControllerHandlerAdapter
 * @see AbstractController
 * @see org.springframework.web.portlet.context.PortletContextAware
 */
public interface Controller {

	/**
	 * Process the action request. There is nothing to return.
	 * @param request current portlet action request
	 * @param response current portlet action response
	 * @throws Exception in case of errors
	 */
	void handleActionRequest(ActionRequest request, ActionResponse response) throws Exception;

	/**
	 * Process the render request and return a ModelAndView object which the DispatcherPortlet
	 * will render. A <code>null</code> return value is not an error: It indicates that this
	 * object completed request processing itself, thus there is no ModelAndView to render.
	 * @param request current portlet render request
	 * @param response current portlet render response
	 * @return a ModelAndView to render, or null if handled directly
	 * @throws Exception in case of errors
	 */
	ModelAndView handleRenderRequest(RenderRequest request, RenderResponse response) throws Exception;

}
