/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A convenient base class for a {@code ResponseBodyAdvice} to instruct the
 * {@link org.springframework.http.converter.json.MappingJackson2HttpMessageConverter}
 * to serialize with JSONP formatting.
 *
 * <p>Sub-classes must specify the query parameter name(s) to check for the name
 * of the JSONP callback function.
 *
 * <p>Sub-classes are likely to be annotated with the {@code @ControllerAdvice}
 * annotation and auto-detected or otherwise must be registered directly with the
 * {@code RequestMappingHandlerAdapter} and {@code ExceptionHandlerExceptionResolver}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 * @deprecated Will be removed as of Spring Framework 5.1, use
 * <a href="https://docs.spring.io/spring/docs/5.0.x/spring-framework-reference/web.html#mvc-cors">CORS</a> instead.
 */
@Deprecated
public abstract class AbstractJsonpResponseBodyAdvice extends AbstractMappingJacksonResponseBodyAdvice {

	/**
	 * Pattern for validating jsonp callback parameter values.
	 */
	private static final Pattern CALLBACK_PARAM_PATTERN = Pattern.compile("[0-9A-Za-z_\\.]*");


	private final Log logger = LogFactory.getLog(getClass());

	private final String[] jsonpQueryParamNames;


	protected AbstractJsonpResponseBodyAdvice(String... queryParamNames) {
		Assert.isTrue(!ObjectUtils.isEmpty(queryParamNames), "At least one query param name is required");
		this.jsonpQueryParamNames = queryParamNames;
	}


	@Override
	protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType,
			MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response) {

		HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

		for (String name : this.jsonpQueryParamNames) {
			String value = servletRequest.getParameter(name);
			if (value != null) {
				if (!isValidJsonpQueryParam(value)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring invalid jsonp parameter value: " + value);
					}
					continue;
				}
				MediaType contentTypeToUse = getContentType(contentType, request, response);
				response.getHeaders().setContentType(contentTypeToUse);
				bodyContainer.setJsonpFunction(value);
				break;
			}
		}
	}

	/**
	 * Validate the jsonp query parameter value. The default implementation
	 * returns true if it consists of digits, letters, or "_" and ".".
	 * Invalid parameter values are ignored.
	 * @param value the query param value, never {@code null}
	 * @since 4.1.8
	 */
	protected boolean isValidJsonpQueryParam(String value) {
		return CALLBACK_PARAM_PATTERN.matcher(value).matches();
	}

	/**
	 * Return the content type to set the response to.
	 * This implementation always returns "application/javascript".
	 * @param contentType the content type selected through content negotiation
	 * @param request the current request
	 * @param response the current response
	 * @return the content type to set the response to
	 */
	protected MediaType getContentType(MediaType contentType, ServerHttpRequest request, ServerHttpResponse response) {
		return new MediaType("application", "javascript");
	}

}
