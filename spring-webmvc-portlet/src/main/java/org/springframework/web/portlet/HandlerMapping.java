/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.web.portlet;

import javax.portlet.PortletRequest;

/**
 * Interface to be implemented by objects that define a mapping between
 * requests and handler objects.
 *
 * <p>This class can be implemented by application developers, although this is not
 * necessary, as {@link org.springframework.web.portlet.handler.PortletModeHandlerMapping},
 * {@link org.springframework.web.portlet.handler.ParameterHandlerMapping} and
 * {@link org.springframework.web.portlet.handler.PortletModeParameterHandlerMapping}
 * are included in the framework. The first is the default if no HandlerMapping
 * bean is registered in the portlet application context.
 *
 * <p>HandlerMapping implementations can support mapped interceptors but do not
 * have to. A handler will always be wrapped in a {@link HandlerExecutionChain}
 * instance, optionally accompanied by some {@link HandlerInterceptor} instances.
 * The DispatcherPortlet will first call each HandlerInterceptor's
 * <code>preHandle</code> method in the given order, finally invoking the handler
 * itself if all <code>preHandle</code> methods have returned <code>true</code>.
 *
 * <p>The ability to parameterize this mapping is a powerful and unusual
 * capability of this Portlet MVC framework. For example, it is possible to
 * write a custom mapping based on session state, cookie state or many other
 * variables. No other MVC framework seems to be equally flexible.
 *
 * <p>Note: Implementations can implement the {@link org.springframework.core.Ordered}
 * interface to be able to specify a sorting order and thus a priority for getting
 * applied by DispatcherPortlet. Non-Ordered instances get treated as lowest priority.
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @see org.springframework.core.Ordered
 * @see org.springframework.web.portlet.handler.AbstractHandlerMapping
 * @see org.springframework.web.portlet.handler.PortletModeHandlerMapping
 * @see org.springframework.web.portlet.handler.ParameterHandlerMapping
 * @see org.springframework.web.portlet.handler.PortletModeParameterHandlerMapping
 */
public interface HandlerMapping {

	/**
	 * Return a handler and any interceptors for this request. The choice may be made
	 * on portlet mode, session state, or any factor the implementing class chooses.
	 * <p>The returned HandlerExecutionChain contains a handler Object, rather than
	 * even a tag interface, so that handlers are not constrained in any way.
	 * For example, a HandlerAdapter could be written to allow another framework's
	 * handler objects to be used.
	 * <p>Returns <code>null</code> if no match was found. This is not an error.
	 * The DispatcherPortlet will query all registered HandlerMapping beans to find
	 * a match, and only decide there is an error if none can find a handler.
	 * @param request current portlet request
	 * @return a HandlerExecutionChain instance containing handler object and
	 * any interceptors, or null if no mapping found
	 * @throws Exception if there is an internal error
	 */
	HandlerExecutionChain getHandler(PortletRequest request) throws Exception;

}
