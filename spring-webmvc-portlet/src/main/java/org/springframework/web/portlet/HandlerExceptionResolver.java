/*
 * Copyright 2002-2010 the original author or authors.
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

import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

/**
 * Interface to be implemented by objects than can resolve exceptions thrown
 * during handler mapping or execution, in the typical case to error views.
 * Implementors are typically registered as beans in the application context.
 *
 * <p>Error views are analogous to the error page JSPs, but can be used with
 * any kind of exception including any checked exception, with potentially
 * fine-granular mappings for specific handlers.
 *
 * @author Juergen Hoeller
 * @author John A. Lewis
 * @since 2.0
 */
public interface HandlerExceptionResolver {

	/**
	 * Try to resolve the given exception that got thrown during on handler execution,
	 * returning a ModelAndView that represents a specific error page if appropriate.
	 * @param request current portlet request
	 * @param response current portlet response
	 * @param handler the executed handler, or null if none chosen at the time of
	 * the exception (for example, if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding ModelAndView to forward to,
	 * or {@code null} for default processing
	 */
	ModelAndView resolveException(
			RenderRequest request, RenderResponse response, Object handler, Exception ex);

	/**
	 * Try to resolve the given exception that got thrown during on handler execution,
	 * returning a ModelAndView that represents a specific error page if appropriate.
	 * @param request current portlet request
	 * @param response current portlet response
	 * @param handler the executed handler, or null if none chosen at the time of
	 * the exception (for example, if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding ModelAndView to forward to,
	 * or {@code null} for default processing
	 */
	ModelAndView resolveException(
			ResourceRequest request, ResourceResponse response, Object handler, Exception ex);

}
