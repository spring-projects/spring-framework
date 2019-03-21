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

package org.springframework.web.portlet.handler;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.springframework.web.portlet.HandlerInterceptor;
import org.springframework.web.portlet.ModelAndView;

/**
 * Abstract adapter class for the {@link HandlerInterceptor} interface,
 * for simplified implementation of pre-only/post-only interceptors.
 *
 * @author Juergen Hoeller
 * @author John A. Lewis
 * @since 2.0
 */
public abstract class HandlerInterceptorAdapter implements HandlerInterceptor {

	/**
	 * This implementation delegates to {@link #preHandle}.
	 */
	@Override
	public boolean preHandleAction(ActionRequest request, ActionResponse response, Object handler)
			throws Exception {

		return preHandle(request, response, handler);
	}

	/**
	 * This implementation delegates to {@link #afterCompletion}.
	 */
	@Override
	public void afterActionCompletion(
			ActionRequest request, ActionResponse response, Object handler, Exception ex)
			throws Exception {

		afterCompletion(request, response, handler, ex);
	}


	/**
	 * This implementation delegates to {@link #preHandle}.
	 */
	@Override
	public boolean preHandleRender(RenderRequest request, RenderResponse response, Object handler)
			throws Exception {

		return preHandle(request, response, handler);
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public void postHandleRender(
			RenderRequest request, RenderResponse response, Object handler, ModelAndView modelAndView)
			throws Exception {
	}

	/**
	 * This implementation delegates to {@link #afterCompletion}.
	 */
	@Override
	public void afterRenderCompletion(
			RenderRequest request, RenderResponse response, Object handler, Exception ex)
			throws Exception {

		afterCompletion(request, response, handler, ex);
	}


	/**
	 * This implementation delegates to {@link #preHandle}.
	 */
	@Override
	public boolean preHandleResource(ResourceRequest request, ResourceResponse response, Object handler)
			throws Exception {

		return preHandle(request, response, handler);
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public void postHandleResource(
			ResourceRequest request, ResourceResponse response, Object handler, ModelAndView modelAndView)
			throws Exception {
	}

	/**
	 * This implementation delegates to {@link #afterCompletion}.
	 */
	@Override
	public void afterResourceCompletion(
			ResourceRequest request, ResourceResponse response, Object handler, Exception ex)
			throws Exception {

		afterCompletion(request, response, handler, ex);
	}


	/**
	 * This implementation delegates to {@link #preHandle}.
	 */
	@Override
	public boolean preHandleEvent(EventRequest request, EventResponse response, Object handler)
			throws Exception {

		return preHandle(request, response, handler);
	}

	/**
	 * This implementation delegates to {@link #afterCompletion}.
	 */
	@Override
	public void afterEventCompletion(
			EventRequest request, EventResponse response, Object handler, Exception ex)
			throws Exception {

		afterCompletion(request, response, handler, ex);
	}


	/**
	 * Default callback that all "pre*" methods delegate to.
	 * <p>This implementation always returns {@code true}.
	 */
	protected boolean preHandle(PortletRequest request, PortletResponse response, Object handler)
			throws Exception {

		return true;
	}

	/**
	 * Default callback that all "after*" methods delegate to.
	 * <p>This implementation is empty.
	 */
	protected void afterCompletion(
			PortletRequest request, PortletResponse response, Object handler, Exception ex)
			throws Exception {

	}

}
