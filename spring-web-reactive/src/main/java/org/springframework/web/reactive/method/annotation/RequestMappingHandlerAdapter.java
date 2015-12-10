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

package org.springframework.web.reactive.method.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.Publishers;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.core.codec.support.ByteBufferDecoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.support.JacksonJsonDecoder;
import org.springframework.core.codec.support.JsonObjectDecoder;
import org.springframework.core.codec.support.StringDecoder;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.method.InvocableHandlerMethod;
import org.springframework.web.method.HandlerMethod;


/**
 * @author Rossen Stoyanchev
 */
public class RequestMappingHandlerAdapter implements HandlerAdapter, InitializingBean {

	private final List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();

	private ConversionService conversionService = new DefaultConversionService();


	public void setArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		this.argumentResolvers.clear();
		this.argumentResolvers.addAll(resolvers);
	}

	public List<HandlerMethodArgumentResolver> getArgumentResolvers() {
		return this.argumentResolvers;
	}

	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public ConversionService getConversionService() {
		return this.conversionService;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (ObjectUtils.isEmpty(this.argumentResolvers)) {

			List<Decoder<?>> decoders = Arrays.asList(new ByteBufferDecoder(),
					new StringDecoder(), new JacksonJsonDecoder(new JsonObjectDecoder()));

			this.argumentResolvers.add(new RequestParamArgumentResolver());
			this.argumentResolvers.add(new RequestBodyArgumentResolver(decoders, this.conversionService));
		}
	}

	@Override
	public boolean supports(Object handler) {
		return HandlerMethod.class.equals(handler.getClass());
	}

	@Override
	public Publisher<HandlerResult> handle(ServerHttpRequest request,
			ServerHttpResponse response, Object handler) {

		InvocableHandlerMethod handlerMethod = new InvocableHandlerMethod((HandlerMethod) handler);
		handlerMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
		return handlerMethod.invokeForRequest(request);
	}

}