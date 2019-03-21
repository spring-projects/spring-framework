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

/**
 * Interceptor to forward a request parameter from the {@code ActionRequest} to the
 * {@code RenderRequest}.
 *
 * <p>This can be useful when using {@link ParameterHandlerMapping ParameterHandlerMapping}
 * or {@link PortletModeParameterHandlerMapping PortletModeParameterHandlerMapping}.
 * It will ensure that the parameter that was used to map the {@code ActionRequest}
 * to a handler will be forwarded to the {@code RenderRequest} so that it will also be
 * mapped the same way.
 *
 * <p>When using this Interceptor, you can still change the value of the mapping parameter
 * in your handler in order to change where the render request will get mapped.
 *
 * <p>Be aware that this Interceptor does call {@code ActionResponse.setRenderParameter},
 * which means that you will not be able to call {@code ActionResponse.sendRedirect} in
 * your handler.  If you may need to issue a redirect, then you should avoid this Interceptor
 * and either write a different one that does this in a different way, or manually forward
 * the parameter from within your handler(s).
 *
 * <p>Thanks to Rainer Schmitz for suggesting this mapping strategy!
 *
 * @author John A. Lewis
 * @since 2.0
 * @see ParameterHandlerMapping
 * @see PortletModeParameterHandlerMapping
 */
public class ParameterMappingInterceptor extends HandlerInterceptorAdapter {

	/** Request parameter name to use for mapping to handlers */
	public final static String DEFAULT_PARAMETER_NAME = "action";

	private String parameterName = DEFAULT_PARAMETER_NAME;


	/**
	 * Set the name of the parameter used for mapping.
	 */
	public void setParameterName(String parameterName) {
		this.parameterName = (parameterName != null ? parameterName : DEFAULT_PARAMETER_NAME);
	}


	/**
	 * If request is an {@link javax.portlet.ActionRequest ActionRequest},
	 * get handler mapping parameter and add it to the ActionResponse.
	 */
	@Override
	public boolean preHandleAction(ActionRequest request, ActionResponse response, Object handler) {
		String mappingParameter = request.getParameter(this.parameterName);
		if (mappingParameter != null) {
			response.setRenderParameter(parameterName, mappingParameter);
		}
		return true;
	}

}
