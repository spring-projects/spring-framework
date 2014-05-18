/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.method.ControllerAdviceBean;

import java.util.List;

/**
 * Invokes a a list of ResponseBodyInterceptor's.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
class ResponseBodyInterceptorChain {

	private static Log logger = LogFactory.getLog(ResponseBodyInterceptorChain.class);

	private final List<Object> interceptors;


	public ResponseBodyInterceptorChain(List<Object> interceptors) {
		this.interceptors = interceptors;
	}


	public <T> T invoke(T body, MediaType contentType, Class<HttpMessageConverter<T>> converterType,
			MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response) {

		if (this.interceptors != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Invoking ResponseBody interceptor chain for body=" + body);
			}
			for (Object interceptor : this.interceptors) {
				if (interceptor instanceof ControllerAdviceBean) {
					ControllerAdviceBean adviceBean = (ControllerAdviceBean) interceptor;
					if (!adviceBean.isApplicableToBeanType(returnType.getContainingClass())) {
						continue;
					}
					interceptor = adviceBean.resolveBean();
				}
				Assert.state(interceptor instanceof  ResponseBodyInterceptor);
				body = ((ResponseBodyInterceptor) interceptor).beforeBodyWrite(
						body, contentType, converterType, returnType, request, response);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("After interceptor chain body=" + body);
			}
		}
		return body;
	}

}
