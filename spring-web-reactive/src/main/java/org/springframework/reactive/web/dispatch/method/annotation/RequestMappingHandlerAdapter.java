/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.reactive.web.dispatch.method.annotation;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.reactive.codec.decoder.ByteBufferDecoder;
import org.springframework.reactive.codec.decoder.ByteToMessageDecoder;
import org.springframework.reactive.codec.decoder.JacksonJsonDecoder;
import org.springframework.reactive.codec.decoder.JsonObjectDecoder;
import org.springframework.reactive.codec.decoder.StringDecoder;
import org.springframework.reactive.web.dispatch.HandlerAdapter;
import org.springframework.reactive.web.dispatch.HandlerResult;
import org.springframework.reactive.web.dispatch.method.HandlerMethodArgumentResolver;
import org.springframework.reactive.web.dispatch.method.InvocableHandlerMethod;
import org.springframework.reactive.web.http.ServerHttpRequest;
import org.springframework.reactive.web.http.ServerHttpResponse;
import org.springframework.web.method.HandlerMethod;


/**
 * @author Rossen Stoyanchev
 */
public class RequestMappingHandlerAdapter implements HandlerAdapter, InitializingBean {

	private List<HandlerMethodArgumentResolver> argumentResolvers;


	public void setHandlerMethodArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		this.argumentResolvers.clear();
		this.argumentResolvers.addAll(resolvers);
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.argumentResolvers == null) {
			this.argumentResolvers = new ArrayList<>();
			this.argumentResolvers.add(new RequestParamArgumentResolver());
			List<ByteToMessageDecoder<?>> deserializers = Arrays.asList(new ByteBufferDecoder(),
					new StringDecoder(), new JacksonJsonDecoder());
			List<ByteToMessageDecoder<ByteBuffer>> preProcessors = Arrays.asList(new JsonObjectDecoder());
			this.argumentResolvers.add(new RequestBodyArgumentResolver(deserializers,
					new DefaultConversionService(), preProcessors));
		}
	}

	@Override
	public boolean supports(Object handler) {
		return HandlerMethod.class.equals(handler.getClass());
	}

	@Override
	public HandlerResult handle(ServerHttpRequest request, ServerHttpResponse response,
			Object handler) throws Exception {

		final InvocableHandlerMethod invocable = new InvocableHandlerMethod((HandlerMethod) handler);
		invocable.setHandlerMethodArgumentResolvers(this.argumentResolvers);

		Object result = invocable.invokeForRequest(request);

		return new HandlerResult(invocable, result);
	}

}
