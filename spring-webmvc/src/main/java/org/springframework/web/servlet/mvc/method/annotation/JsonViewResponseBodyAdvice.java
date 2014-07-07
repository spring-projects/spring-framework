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
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;

/**
 * A {@code ResponseBodyAdvice} implementation that adds support for
 * Jackson's {@code @JsonView} annotation declared on a Spring MVC
 * {@code @RequestMapping} or {@code @ExceptionHandler} method. The serialization
 * view specified in the annotation will be passed in to the
 * {@code MappingJackson2HttpMessageConverter} which will then use it to
 * serialize the response body with.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see com.fasterxml.jackson.databind.ObjectMapper#writerWithView(Class)
 */
public class JsonViewResponseBodyAdvice extends AbstractMappingJacksonResponseBodyAdvice {

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return (super.supports(returnType, converterType) && returnType.getMethodAnnotation(JsonView.class) != null);
	}

	@Override
	protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType,
			MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response) {

		JsonView annotation = returnType.getMethodAnnotation(JsonView.class);
		Assert.isTrue(annotation.value().length != 0, "No view class in JsonView annotation on " + returnType);
		bodyContainer.setSerializationView(annotation.value()[0]);
	}

}
