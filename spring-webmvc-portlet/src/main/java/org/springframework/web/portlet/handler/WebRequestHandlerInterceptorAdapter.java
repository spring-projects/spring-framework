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

package org.springframework.web.portlet.handler;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.springframework.util.Assert;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.portlet.HandlerInterceptor;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.context.PortletWebRequest;

/**
 * Adapter that implements the Portlet HandlerInterceptor interface
 * and wraps an underlying WebRequestInterceptor.
 *
 * <p><b>NOTE:</b> The WebRequestInterceptor is by default only applied to the Portlet
 * <b>render</b> phase, which is dealing with preparing and rendering a Portlet view.
 * The Portlet action phase will only be intercepted with WebRequestInterceptor calls
 * if the {@code renderPhaseOnly} flag is explicitly set to {@code false}.
 * In general, it is recommended to use the Portlet-specific HandlerInterceptor
 * mechanism for differentiating between action and render interception.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.web.context.request.WebRequestInterceptor
 * @see org.springframework.web.portlet.HandlerInterceptor
 */
public class WebRequestHandlerInterceptorAdapter implements HandlerInterceptor {

	private final WebRequestInterceptor requestInterceptor;

	private final boolean renderPhaseOnly;


	/**
	 * Create a new WebRequestHandlerInterceptorAdapter for the given WebRequestInterceptor,
	 * applying to the render phase only.
	 * @param requestInterceptor the WebRequestInterceptor to wrap
	 */
	public WebRequestHandlerInterceptorAdapter(WebRequestInterceptor requestInterceptor) {
		this(requestInterceptor, true);
	}

	/**
	 * Create a new WebRequestHandlerInterceptorAdapter for the given WebRequestInterceptor.
	 * @param requestInterceptor the WebRequestInterceptor to wrap
	 * @param renderPhaseOnly whether to apply to the render phase only ({@code true})
	 * or to the action phase as well ({@code false})
	 */
	public WebRequestHandlerInterceptorAdapter(WebRequestInterceptor requestInterceptor, boolean renderPhaseOnly) {
		Assert.notNull(requestInterceptor, "WebRequestInterceptor must not be null");
		this.requestInterceptor = requestInterceptor;
		this.renderPhaseOnly = renderPhaseOnly;
	}


	public boolean preHandleAction(ActionRequest request, ActionResponse response, Object handler) throws Exception {
		if (!this.renderPhaseOnly) {
			this.requestInterceptor.preHandle(new PortletWebRequest(request));
		}
		return true;
	}

	public void afterActionCompletion(
			ActionRequest request, ActionResponse response, Object handler, Exception ex) throws Exception {

		if (!this.renderPhaseOnly) {
			this.requestInterceptor.afterCompletion(new PortletWebRequest(request), ex);
		}
	}

	public boolean preHandleRender(RenderRequest request, RenderResponse response, Object handler) throws Exception {
		this.requestInterceptor.preHandle(new PortletWebRequest(request));
		return true;
	}

	public void postHandleRender(
			RenderRequest request, RenderResponse response, Object handler, ModelAndView modelAndView) throws Exception {

		this.requestInterceptor.postHandle(new PortletWebRequest(request),
				(modelAndView != null && !modelAndView.wasCleared() ? modelAndView.getModelMap() : null));
	}

	public void afterRenderCompletion(
			RenderRequest request, RenderResponse response, Object handler, Exception ex) throws Exception {

		this.requestInterceptor.afterCompletion(new PortletWebRequest(request), ex);
	}

	public boolean preHandleResource(ResourceRequest request, ResourceResponse response, Object handler)
			throws Exception {

		this.requestInterceptor.preHandle(new PortletWebRequest(request));
		return true;
	}

	public void postHandleResource(ResourceRequest request, ResourceResponse response, Object handler, ModelAndView modelAndView)
			throws Exception {

		this.requestInterceptor.postHandle(new PortletWebRequest(request),
				(modelAndView != null ? modelAndView.getModelMap() : null));
	}

	public void afterResourceCompletion(ResourceRequest request, ResourceResponse response, Object handler,
			Exception ex) throws Exception {

		this.requestInterceptor.afterCompletion(new PortletWebRequest(request), ex);
	}

	public boolean preHandleEvent(EventRequest request, EventResponse response, Object handler) throws Exception {
		this.requestInterceptor.preHandle(new PortletWebRequest(request));
		return true;
	}

	public void afterEventCompletion(EventRequest request, EventResponse response, Object handler, Exception ex)
			throws Exception {

		this.requestInterceptor.afterCompletion(new PortletWebRequest(request), ex);
	}

}
