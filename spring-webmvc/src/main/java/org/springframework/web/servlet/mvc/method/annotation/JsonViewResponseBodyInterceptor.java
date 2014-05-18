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

import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;

/**
 * A {@code ResponseBodyInterceptor} implementation that adds support for the
 * Jackson {@code @JsonView} annotation on a Spring MVC {@code @RequestMapping}
 * or {@code @ExceptionHandler} method.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class JsonViewResponseBodyInterceptor implements ResponseBodyInterceptor {


	@Override
	@SuppressWarnings("unchecked")
	public <T> T beforeBodyWrite(T body, MediaType contentType, Class<? extends HttpMessageConverter<T>> converterType,
			MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response) {

		if (!MappingJackson2HttpMessageConverter.class.equals(converterType)) {
			return body;
		}

		JsonView annotation = returnType.getMethodAnnotation(JsonView.class);
		if (annotation == null) {
			return body;
		}

		Assert.isTrue(annotation.value().length != 0,
				"Expected at least one serialization view class in JsonView annotation on " + returnType);

		return (T) new MappingJacksonValue(body, annotation.value()[0]);
	}

}
