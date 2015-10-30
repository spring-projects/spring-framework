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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.Publishers;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.server.ReactiveServerHttpRequest;
import org.springframework.http.server.ReactiveServerHttpResponse;
import org.springframework.reactive.codec.decoder.ByteBufferDecoder;
import org.springframework.reactive.codec.decoder.ByteToMessageDecoder;
import org.springframework.reactive.codec.decoder.JacksonJsonDecoder;
import org.springframework.reactive.codec.decoder.StringDecoder;
import org.springframework.reactive.web.dispatch.HandlerAdapter;
import org.springframework.reactive.web.dispatch.HandlerResult;
import org.springframework.reactive.web.dispatch.method.HandlerMethodArgumentResolver;
import org.springframework.reactive.web.dispatch.method.InvocableHandlerMethod;
import org.springframework.web.method.HandlerMethod;


/**
 * @author Rossen Stoyanchev
 */
public class RequestMappingHandlerAdapter implements HandlerAdapter, InitializingBean {

	private List<HandlerMethodArgumentResolver> argumentResolvers;

	private ConversionService conversionService;


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
		if (this.argumentResolvers == null) {

			List<ByteToMessageDecoder<?>> decoders = Arrays.asList(new ByteBufferDecoder(),
					new StringDecoder(), new JacksonJsonDecoder());

			this.argumentResolvers = new ArrayList<>();
			this.argumentResolvers.add(new RequestParamArgumentResolver());
			this.argumentResolvers.add(new RequestBodyArgumentResolver(decoders, this.conversionService));
		}
	}

	@Override
	public boolean supports(Object handler) {
		return HandlerMethod.class.equals(handler.getClass());
	}

	@Override
	public Publisher<HandlerResult> handle(ReactiveServerHttpRequest request,
			ReactiveServerHttpResponse response, Object handler) {

		InvocableHandlerMethod handlerMethod = new InvocableHandlerMethod((HandlerMethod) handler);
		handlerMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);

		Publisher<Object> resultPublisher = handlerMethod.invokeForRequest(request);
		return Publishers.map(resultPublisher, result -> new HandlerResult(handlerMethod, result));
	}

}