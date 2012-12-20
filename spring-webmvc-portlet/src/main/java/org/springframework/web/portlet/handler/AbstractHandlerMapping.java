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

package org.springframework.web.portlet.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.portlet.PortletRequest;

import org.springframework.beans.BeansException;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.Ordered;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.portlet.HandlerExecutionChain;
import org.springframework.web.portlet.HandlerInterceptor;
import org.springframework.web.portlet.HandlerMapping;

/**
 * Abstract base class for {@link org.springframework.web.portlet.HandlerMapping}
 * implementations. Supports ordering, a default handler, and handler interceptors.
 *
 * @author Juergen Hoeller
 * @author John A. Lewis
 * @since 2.0
 * @see #getHandlerInternal
 * @see #setDefaultHandler
 * @see #setInterceptors
 * @see org.springframework.web.portlet.HandlerInterceptor
 */
public abstract class AbstractHandlerMapping extends ApplicationObjectSupport implements HandlerMapping, Ordered {

	private int order = Integer.MAX_VALUE;  // default: same as non-Ordered

	private Object defaultHandler;

	private final List<Object> interceptors = new ArrayList<Object>();

	private boolean applyWebRequestInterceptorsToRenderPhaseOnly = true;

	private HandlerInterceptor[] adaptedInterceptors;


	/**
	 * Specify the order value for this HandlerMapping bean.
	 * <p>Default value is <code>Integer.MAX_VALUE</code>, meaning that it's non-ordered.
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public final void setOrder(int order) {
	  this.order = order;
	}

	public final int getOrder() {
	  return this.order;
	}

	/**
	 * Set the default handler for this handler mapping.
	 * This handler will be returned if no specific mapping was found.
	 * <p>Default is <code>null</code>, indicating no default handler.
	 */
	public void setDefaultHandler(Object defaultHandler) {
		this.defaultHandler = defaultHandler;
	}

	/**
	 * Return the default handler for this handler mapping,
	 * or <code>null</code> if none.
	 */
	public Object getDefaultHandler() {
		return this.defaultHandler;
	}

	/**
	 * Set the interceptors to apply for all handlers mapped by this handler mapping.
	 * <p>Supported interceptor types are HandlerInterceptor and WebRequestInterceptor.
	 * Each given WebRequestInterceptor will be wrapped in a WebRequestHandlerInterceptorAdapter.
	 * @param interceptors array of handler interceptors, or <code>null</code> if none
	 * @see #adaptInterceptor
	 * @see org.springframework.web.portlet.HandlerInterceptor
	 * @see org.springframework.web.context.request.WebRequestInterceptor
	 */
	public void setInterceptors(Object[] interceptors) {
		this.interceptors.addAll(Arrays.asList(interceptors));
	}

	/**
	 * Specify whether to apply WebRequestInterceptors to the Portlet render phase
	 * only ("true", or whether to apply them to the Portlet action phase as well
	 * ("false").
	 * <p>Default is "true", since WebRequestInterceptors are usually built for
	 * MVC-style handler execution plus rendering process (which is, for example,
	 * the primary target scenario for "Open Session in View" interceptors,
	 * offering lazy loading of persistent objects during view rendering).
	 * Set this to "false" to have WebRequestInterceptors apply to the action
	 * phase as well (for example, in case of an "Open Session in View" interceptor,
	 * to allow for lazy loading outside of a transaction during the action phase).
	 * @see #setInterceptors
	 * @see org.springframework.web.context.request.WebRequestInterceptor
	 * @see WebRequestHandlerInterceptorAdapter#WebRequestHandlerInterceptorAdapter(WebRequestInterceptor, boolean)
	 * @see org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor
	 */
	public void setApplyWebRequestInterceptorsToRenderPhaseOnly(boolean applyWebRequestInterceptorsToRenderPhaseOnly) {
		this.applyWebRequestInterceptorsToRenderPhaseOnly = applyWebRequestInterceptorsToRenderPhaseOnly;
	}


	/**
	 * Initializes the interceptors.
	 * @see #extendInterceptors(java.util.List)
	 * @see #initInterceptors()
	 */
	@Override
	protected void initApplicationContext() throws BeansException {
		extendInterceptors(this.interceptors);
		initInterceptors();
	}

	/**
	 * Extension hook that subclasses can override to register additional interceptors,
	 * given the configured interceptors (see {@link #setInterceptors}).
	 * <p>Will be invoked before {@link #initInterceptors()} adapts the specified
	 * interceptors into {@link HandlerInterceptor} instances.
	 * <p>The default implementation is empty.
	 * @param interceptors the configured interceptor List (never <code>null</code>),
	 * allowing to add further interceptors before as well as after the existing
	 * interceptors
	 */
	protected void extendInterceptors(List<?> interceptors) {
	}

	/**
	 * Initialize the specified interceptors, adapting them where necessary.
	 * @see #setInterceptors
	 * @see #adaptInterceptor
	 */
	protected void initInterceptors() {
		if (!this.interceptors.isEmpty()) {
			this.adaptedInterceptors = new HandlerInterceptor[this.interceptors.size()];
			for (int i = 0; i < this.interceptors.size(); i++) {
				Object interceptor = this.interceptors.get(i);
				if (interceptor == null) {
					throw new IllegalArgumentException("Entry number " + i + " in interceptors array is null");
				}
				this.adaptedInterceptors[i] = adaptInterceptor(interceptor);
			}
		}
	}

	/**
	 * Adapt the given interceptor object to the HandlerInterceptor interface.
	 * <p>Supported interceptor types are HandlerInterceptor and WebRequestInterceptor.
	 * Each given WebRequestInterceptor will be wrapped in a WebRequestHandlerInterceptorAdapter.
	 * Can be overridden in subclasses.
	 * @param interceptor the specified interceptor object
	 * @return the interceptor wrapped as HandlerInterceptor
	 * @see #setApplyWebRequestInterceptorsToRenderPhaseOnly
	 * @see org.springframework.web.portlet.HandlerInterceptor
	 * @see org.springframework.web.context.request.WebRequestInterceptor
	 * @see WebRequestHandlerInterceptorAdapter
	 */
	protected HandlerInterceptor adaptInterceptor(Object interceptor) {
		if (interceptor instanceof HandlerInterceptor) {
			return (HandlerInterceptor) interceptor;
		}
		else if (interceptor instanceof WebRequestInterceptor) {
			return new WebRequestHandlerInterceptorAdapter(
					(WebRequestInterceptor) interceptor, this.applyWebRequestInterceptorsToRenderPhaseOnly);
		}
		else {
			throw new IllegalArgumentException("Interceptor type not supported: " + interceptor.getClass().getName());
		}
	}

	/**
	 * Return the adapted interceptors as HandlerInterceptor array.
	 * @return the array of HandlerInterceptors, or <code>null</code> if none
	 */
	protected final HandlerInterceptor[] getAdaptedInterceptors() {
		return this.adaptedInterceptors;
	}


	/**
	 * Look up a handler for the given request, falling back to the default
	 * handler if no specific one is found.
	 * @param request current portlet request
	 * @return the corresponding handler instance, or the default handler
	 * @see #getHandlerInternal
	 */
	public final HandlerExecutionChain getHandler(PortletRequest request) throws Exception {
		Object handler = getHandlerInternal(request);
		if (handler == null) {
			handler = getDefaultHandler();
		}
		if (handler == null) {
			return null;
		}
		// Bean name or resolved handler?
		if (handler instanceof String) {
			String handlerName = (String) handler;
			handler = getApplicationContext().getBean(handlerName);
		}
		return getHandlerExecutionChain(handler, request);
	}

	/**
	 * Look up a handler for the given request, returning <code>null</code> if no
	 * specific one is found. This method is called by {@link #getHandler};
	 * a <code>null</code> return value will lead to the default handler, if one is set.
	 * <p>Note: This method may also return a pre-built {@link HandlerExecutionChain},
	 * combining a handler object with dynamically determined interceptors.
	 * Statically specified interceptors will get merged into such an existing chain.
	 * @param request current portlet request
	 * @return the corresponding handler instance, or <code>null</code> if none found
	 * @throws Exception if there is an internal error
	 * @see #getHandler
	 */
	protected abstract Object getHandlerInternal(PortletRequest request) throws Exception;

	/**
	 * Build a HandlerExecutionChain for the given handler, including applicable interceptors.
	 * <p>The default implementation simply builds a standard HandlerExecutionChain with
	 * the given handler and this handler mapping's common interceptors. Subclasses may
	 * override this in order to extend/rearrange the list of interceptors.
	 * <p><b>NOTE:</b> The passed-in handler object may be a raw handler or a pre-built
	 * HandlerExecutionChain. This method should handle those two cases explicitly,
	 * either building a new HandlerExecutionChain or extending the existing chain.
	 * <p>For simply adding an interceptor, consider calling <code>super.getHandlerExecutionChain</code>
	 * and invoking {@link HandlerExecutionChain#addInterceptor} on the returned chain object.
	 * @param handler the resolved handler instance (never <code>null</code>)
	 * @param request current portlet request
	 * @return the HandlerExecutionChain (never <code>null</code>)
	 * @see #getAdaptedInterceptors()
	 */
	protected HandlerExecutionChain getHandlerExecutionChain(Object handler, PortletRequest request) {
		if (handler instanceof HandlerExecutionChain) {
			HandlerExecutionChain chain = (HandlerExecutionChain) handler;
			chain.addInterceptors(getAdaptedInterceptors());
			return chain;
		}
		else {
			return new HandlerExecutionChain(handler, getAdaptedInterceptors());
		}
	}

}
