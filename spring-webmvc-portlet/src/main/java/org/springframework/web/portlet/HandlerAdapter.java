/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.portlet;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

/**
 * Portlet MVC framework SPI interface, allowing parameterization of core MVC workflow.
 *
 * <p>Interface that must be implemented for each handler type to handle a request.
 * This interface is used to allow the DispatcherPortlet to be indefinitely
 * extensible. The DispatcherPortlet accesses all installed handlers through this
 * interface, meaning that it does not contain code specific to any handler type.
 *
 * <p>Note that a handler can be of type Object. This is to enable handlers from
 * other frameworks to be integrated with this framework without custom coding.
 *
 * <p>This interface is not intended for application developers. It is available
 * to handlers who want to develop their own web workflow.
 *
 * <p>Note: Implementations can implement the Ordered interface to be able to
 * specify a sorting order and thus a priority for getting applied by
 * DispatcherPortlet. Non-Ordered instances get treated as lowest priority.
 *
 * @author John A. Lewis
 * @since 2.0
 * @see org.springframework.web.portlet.mvc.SimpleControllerHandlerAdapter
 */
public interface HandlerAdapter {

	/**
	 * Given a handler instance, return whether or not this HandlerAdapter can
	 * support it. Typical HandlerAdapters will base the decision on the handler
	 * type. HandlerAdapters will usually only support one handler type each.
	 * <p>A typical implementation:
	 * <p>{@code
	 * return (handler instanceof MyHandler);
	 * }
	 * @param handler handler object to check
	 * @return whether or not this object can use the given handler
	 */
	boolean supports(Object handler);

	/**
	 * Use the given handler to handle this action request.
	 * The workflow that is required may vary widely.
	 * @param request current action request
	 * @param response current action response
	 * @param handler handler to use. This object must have previously been passed
	 * to the {@code supports} method of this interface, which must have
	 * returned true.
	 * @throws Exception in case of errors
	 * @see javax.portlet.Portlet#processAction
	 */
	void handleAction(ActionRequest request, ActionResponse response, Object handler) throws Exception;

	/**
	 * Use the given handler to handle this render request.
	 * The workflow that is required may vary widely.
	 * @param request current render request
	 * @param response current render response
	 * @param handler handler to use. This object must have previously been passed
	 * to the {@code supports} method of this interface, which must have
	 * returned {@code true}.
	 * @throws Exception in case of errors
	 * @return ModelAndView object with the name of the view and the required
	 * model data, or {@code null} if the request has been handled directly
	 * @see javax.portlet.Portlet#render
	 */
	ModelAndView handleRender(RenderRequest request, RenderResponse response, Object handler) throws Exception;

	/**
	 * Use the given handler to handle this resource request.
	 * The workflow that is required may vary widely.
	 * @param request current render request
	 * @param response current render response
	 * @param handler handler to use. This object must have previously been passed
	 * to the {@code supports} method of this interface, which must have
	 * returned {@code true}.
	 * @throws Exception in case of errors
	 * @return ModelAndView object with the name of the view and the required
	 * model data, or {@code null} if the request has been handled directly
	 * @see javax.portlet.ResourceServingPortlet#serveResource
	 */
	ModelAndView handleResource(ResourceRequest request, ResourceResponse response, Object handler) throws Exception;

	/**
	 * Use the given handler to handle this event request.
	 * The workflow that is required may vary widely.
	 * @param request current action request
	 * @param response current action response
	 * @param handler handler to use. This object must have previously been passed
	 * to the {@code supports} method of this interface, which must have
	 * returned true.
	 * @throws Exception in case of errors
	 * @see javax.portlet.EventPortlet#processEvent
	 */
	void handleEvent(EventRequest request, EventResponse response, Object handler) throws Exception;

}
