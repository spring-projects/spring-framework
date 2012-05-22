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

package org.springframework.web.portlet;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.EventResponse;
import javax.portlet.EventRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.ResourceRequest;

/**
 * Workflow interface that allows for customized handler execution chains.
 * Applications can register any number of existing or custom interceptors
 * for certain groups of handlers, to add common pre-processing behavior
 * without needing to modify each handler implementation.
 *
 * <p>A <code>HandlerInterceptor</code> gets called before the appropriate
 * {@link org.springframework.web.portlet.HandlerAdapter} triggers the
 * execution of the handler itself. This mechanism can be used for a large
 * field of preprocessing aspects, e.g. for authorization checks,
 * or common handler behavior like locale or theme changes. Its main purpose
 * is to permit the factoring out of otherwise repetitive handler code.
 *
 * <p>Typically an interceptor chain is defined per
 * {@link org.springframework.web.portlet.HandlerMapping} bean, sharing its
 * granularity. To be able to apply a certain interceptor chain to a group of
 * handlers, one needs to map the desired handlers via one
 * <code>HandlerMapping</code> bean. The interceptors themselves are defined as
 * beans in the application context, referenced by the mapping bean definition
 * via its
 * {@link org.springframework.web.portlet.handler.AbstractHandlerMapping#setInterceptors "interceptors"}
 * property (in XML: a &lt;list&gt; of &lt;ref&gt; elements).
 *
 * <p>A <code>HandlerInterceptor</code> is basically similar to a Servlet
 * {@link javax.servlet.Filter}, but in contrast to the latter it allows
 * custom pre-processing with the option to prohibit the execution of the handler
 * itself, and custom post-processing. <code>Filters</code> are more powerful;
 * for example they allow for exchanging the request and response objects that
 * are handed down the chain. Note that a filter gets configured in
 * <code>web.xml</code>, a <code>HandlerInterceptor</code> in the application context.
 *
 * <p>As a basic guideline, fine-grained handler-related pre-processing tasks are
 * candidates for <code>HandlerInterceptor</code> implementations, especially
 * factored-out common handler code and authorization checks. On the other hand,
 * a <code>Filter</code> is well-suited for request content and view content
 * handling, like multipart forms and GZIP compression. This typically shows when
 * one needs to map the filter to certain content types (e.g. images), or to all
 * requests.
 *
 * <p>Be aware that filters cannot be applied to portlet requests (they
 * only operate on servlet requests), so for portlet requests interceptors are
 * essential.
 *
 * <p>If we assume a "sunny day" request cycle (i.e. a request where nothing goes wrong
 * and all is well), the workflow of a <code>HandlerInterceptor</code> will be as
 * follows:
 *
 * <p><b>Action Request:</b><p>
 * <ol>
 *   <li><code>DispatcherPortlet</code> maps the action request to a particular handler
 * 		 and assembles a handler execution chain consisting of the handler that
 * 		 is to be invoked and all of the <code>HandlerInterceptor</code>
 * 		 instances that apply to the request.</li>
 *   <li>{@link org.springframework.web.portlet.HandlerInterceptor#preHandleAction(javax.portlet.ActionRequest, javax.portlet.ActionResponse, Object) preHandleAction(..)}
 * 		 is called; if the invocation of this method returns <code>true</code> then
 *		 this workflow continues</li>
 *   <li>The target handler handles the action request (via
 * 		 {@link org.springframework.web.portlet.HandlerAdapter#handleAction(javax.portlet.ActionRequest, javax.portlet.ActionResponse, Object) HandlerAdapter.handleAction(..)})</li>
 *   <li>{@link org.springframework.web.portlet.HandlerInterceptor#afterActionCompletion(javax.portlet.ActionRequest, javax.portlet.ActionResponse, Object, Exception) afterActionCompletion(..)}
 * 		 is called</li>
 * </ol>
 *
 * <p><b>Render Request:</b><p>
 * <ol>
 *   <li><code>DispatcherPortlet</code> maps the render request to a particular handler
 * 		 and assembles a handler execution chain consisting of the handler that
 * 		 is to be invoked and all of the <code>HandlerInterceptor</code>
 * 		 instances that apply to the request.</li>
 *   <li>{@link org.springframework.web.portlet.HandlerInterceptor#preHandleRender(javax.portlet.RenderRequest, javax.portlet.RenderResponse, Object) preHandleRender(..)}
 * 		 is called; if the invocation of this method returns <code>true</code> then
 *		 this workflow continues</li>
 *   <li>The target handler handles the render request (via
 * 		 {@link org.springframework.web.portlet.HandlerAdapter#handleRender(javax.portlet.RenderRequest, javax.portlet.RenderResponse, Object) HandlerAdapter.handleRender(..)})</li>
 *   <li>{@link org.springframework.web.portlet.HandlerInterceptor#postHandleRender(javax.portlet.RenderRequest, javax.portlet.RenderResponse, Object, ModelAndView) postHandleRender(..)}
 * 		 is called</li>
 *   <li>If the <code>HandlerAdapter</code> returned a <code>ModelAndView</code>,
 *       then <code>DispatcherPortlet</code> renders the view accordingly
 *   <li>{@link org.springframework.web.portlet.HandlerInterceptor#afterRenderCompletion(javax.portlet.RenderRequest, javax.portlet.RenderResponse, Object, Exception) afterRenderCompletion(..)}
 * 		 is called</li>
 * </ol>
 *
 * @author Juergen Hoeller
 * @author John A. Lewis
 * @since 2.0
 * @see HandlerExecutionChain#getInterceptors
 * @see org.springframework.web.portlet.HandlerMapping
 * @see org.springframework.web.portlet.handler.AbstractHandlerMapping#setInterceptors
 * @see org.springframework.web.portlet.HandlerExecutionChain
 */
public interface HandlerInterceptor {

	/**
	 * Intercept the execution of a handler in the action phase.
	 * <p>Called after a HandlerMapping determines an appropriate handler object
	 * to handle an {@link ActionRequest}, but before said HandlerAdapter actually
	 * invokes the handler.
	 * <p>{@link DispatcherPortlet} processes a handler in an execution chain,
	 * consisting of any number of interceptors, with the handler itself at the end.
	 * With this method, each interceptor can decide to abort the execution chain,
	 * typically throwing an exception or writing a custom response.
	 * @param request current portlet action request
	 * @param response current portlet action response
	 * @param handler chosen handler to execute, for type and/or instance evaluation
	 * @return <code>true</code> if the execution chain should proceed with the
	 * next interceptor or the handler itself. Else, <code>DispatcherPortlet</code>
	 * assumes that this interceptor has already dealt with the response itself
	 * @throws Exception in case of errors
	 */
	boolean preHandleAction(ActionRequest request, ActionResponse response, Object handler)
			throws Exception;

	/**
	 * Callback after completion of request processing in the action phase, that is,
	 * after rendering the view. Will be called on any outcome of handler execution,
	 * thus allowing for proper resource cleanup.
	 * <p>Note: Will only be called if this interceptor's
	 * {@link #preHandleAction(javax.portlet.ActionRequest, javax.portlet.ActionResponse, Object)}
	 * method has successfully completed and returned <code>true</code>!
	 * @param request current portlet action request
	 * @param response current portlet action response
	 * @param handler chosen handler to execute, for type and/or instance examination
	 * @param ex exception thrown on handler execution, if any (only included as
	 * additional context information for the case where a handler threw an exception;
	 * request execution may have failed even when this argument is <code>null</code>)
	 * @throws Exception in case of errors
	 */
	void afterActionCompletion(
			ActionRequest request, ActionResponse response, Object handler, Exception ex)
			throws Exception;

	/**
	 * Intercept the execution of a handler in the render phase.
	 * <p>Called after a HandlerMapping determines an appropriate handler object
	 * to handle a {@link RenderRequest}, but before said HandlerAdapter actually
	 * invokes the handler.
	 * <p>{@link DispatcherPortlet} processes a handler in an execution chain,
	 * consisting of any number of interceptors, with the handler itself at the end.
	 * With this method, each interceptor can decide to abort the execution chain,
	 * typically throwing an exception or writing a custom response.
	 * @param request current portlet render request
	 * @param response current portlet render response
	 * @param handler chosen handler to execute, for type and/or instance evaluation
	 * @return <code>true</code> if the execution chain should proceed with the
	 * next interceptor or the handler itself. Else, <code>DispatcherPortlet</code>
	 * assumes that this interceptor has already dealt with the response itself
	 * @throws Exception in case of errors
	 */
	boolean preHandleRender(RenderRequest request, RenderResponse response, Object handler)
			throws Exception;

	/**
	 * Intercept the execution of a handler in the render phase.
	 * <p>Called after a {@link HandlerAdapter} actually invoked the handler, but
	 * before the <code>DispatcherPortlet</code> renders the view. Can thus expose
	 * additional model objects to the view via the given {@link ModelAndView}.
	 * <p><code>DispatcherPortlet</code> processes a handler in an execution chain,
	 * consisting of any number of interceptors, with the handler itself at the end.
	 * With this method, each interceptor can post-process an execution, getting
	 * applied in inverse order of the execution chain.
	 * @param request current portlet render request
	 * @param response current portlet render response
	 * @param handler chosen handler to execute, for type and/or instance examination
	 * @param modelAndView the <code>ModelAndView</code> that the handler returned
	 * (can also be <code>null</code>)
	 * @throws Exception in case of errors
	 */
	void postHandleRender(
			RenderRequest request, RenderResponse response, Object handler, ModelAndView modelAndView)
			throws Exception;

	/**
	 * Callback after completion of request processing, that is, after rendering
	 * the view. Will be called on any outcome of handler execution, thus allowing
	 * for proper resource cleanup.
	 * <p>Note: Will only be called if this interceptor's
	 * {@link #preHandleRender(javax.portlet.RenderRequest, javax.portlet.RenderResponse, Object)}
	 * method has successfully completed and returned <code>true</code>!
	 * @param request current portlet render request
	 * @param response current portlet render response
	 * @param handler chosen handler to execute, for type and/or instance examination
	 * @param ex exception thrown on handler execution, if any
	 * @throws Exception in case of errors
	 */
	void afterRenderCompletion(
			RenderRequest request, RenderResponse response, Object handler, Exception ex)
			throws Exception;

	/**
	 * Intercept the execution of a handler in the render phase.
	 * <p>Called after a HandlerMapping determines an appropriate handler object
	 * to handle a {@link RenderRequest}, but before said HandlerAdapter actually
	 * invokes the handler.
	 * <p>{@link DispatcherPortlet} processes a handler in an execution chain,
	 * consisting of any number of interceptors, with the handler itself at the end.
	 * With this method, each interceptor can decide to abort the execution chain,
	 * typically throwing an exception or writing a custom response.
	 * @param request current portlet render request
	 * @param response current portlet render response
	 * @param handler chosen handler to execute, for type and/or instance evaluation
	 * @return <code>true</code> if the execution chain should proceed with the
	 * next interceptor or the handler itself. Else, <code>DispatcherPortlet</code>
	 * assumes that this interceptor has already dealt with the response itself
	 * @throws Exception in case of errors
	 */
	boolean preHandleResource(ResourceRequest request, ResourceResponse response, Object handler)
			throws Exception;

	/**
	 * Intercept the execution of a handler in the render phase.
	 * <p>Called after a {@link HandlerAdapter} actually invoked the handler, but
	 * before the <code>DispatcherPortlet</code> renders the view. Can thus expose
	 * additional model objects to the view via the given {@link ModelAndView}.
	 * <p><code>DispatcherPortlet</code> processes a handler in an execution chain,
	 * consisting of any number of interceptors, with the handler itself at the end.
	 * With this method, each interceptor can post-process an execution, getting
	 * applied in inverse order of the execution chain.
	 * @param request current portlet render request
	 * @param response current portlet render response
	 * @param handler chosen handler to execute, for type and/or instance examination
	 * @param modelAndView the <code>ModelAndView</code> that the handler returned
	 * (can also be <code>null</code>)
	 * @throws Exception in case of errors
	 */
	void postHandleResource(
			ResourceRequest request, ResourceResponse response, Object handler, ModelAndView modelAndView)
			throws Exception;

	/**
	 * Callback after completion of request processing, that is, after rendering
	 * the view. Will be called on any outcome of handler execution, thus allowing
	 * for proper resource cleanup.
	 * <p>Note: Will only be called if this interceptor's
	 * {@link #preHandleRender(javax.portlet.RenderRequest, javax.portlet.RenderResponse, Object)}
	 * method has successfully completed and returned <code>true</code>!
	 * @param request current portlet render request
	 * @param response current portlet render response
	 * @param handler chosen handler to execute, for type and/or instance examination
	 * @param ex exception thrown on handler execution, if any
	 * @throws Exception in case of errors
	 */
	void afterResourceCompletion(
			ResourceRequest request, ResourceResponse response, Object handler, Exception ex)
			throws Exception;


	/**
	 * Intercept the execution of a handler in the action phase.
	 * <p>Called after a HandlerMapping determines an appropriate handler object
	 * to handle an {@link ActionRequest}, but before said HandlerAdapter actually
	 * invokes the handler.
	 * <p>{@link DispatcherPortlet} processes a handler in an execution chain,
	 * consisting of any number of interceptors, with the handler itself at the end.
	 * With this method, each interceptor can decide to abort the execution chain,
	 * typically throwing an exception or writing a custom response.
	 * @param request current portlet action request
	 * @param response current portlet action response
	 * @param handler chosen handler to execute, for type and/or instance evaluation
	 * @return <code>true</code> if the execution chain should proceed with the
	 * next interceptor or the handler itself. Else, <code>DispatcherPortlet</code>
	 * assumes that this interceptor has already dealt with the response itself
	 * @throws Exception in case of errors
	 */
	boolean preHandleEvent(EventRequest request, EventResponse response, Object handler)
			throws Exception;

	/**
	 * Callback after completion of request processing in the action phase, that is,
	 * after rendering the view. Will be called on any outcome of handler execution,
	 * thus allowing for proper resource cleanup.
	 * <p>Note: Will only be called if this interceptor's
	 * {@link #preHandleAction(javax.portlet.ActionRequest, javax.portlet.ActionResponse, Object)}
	 * method has successfully completed and returned <code>true</code>!
	 * @param request current portlet action request
	 * @param response current portlet action response
	 * @param handler chosen handler to execute, for type and/or instance examination
	 * @param ex exception thrown on handler execution, if any (only included as
	 * additional context information for the case where a handler threw an exception;
	 * request execution may have failed even when this argument is <code>null</code>)
	 * @throws Exception in case of errors
	 */
	void afterEventCompletion(
			EventRequest request, EventResponse response, Object handler, Exception ex)
			throws Exception;

}
