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

import java.lang.reflect.Method;
import java.nio.charset.Charset;

import org.reactivestreams.Publisher;
import reactor.rx.Streams;

import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.reactive.web.dispatch.HandlerResult;
import org.springframework.reactive.web.dispatch.HandlerResultHandler;
import org.springframework.reactive.web.http.ServerHttpRequest;
import org.springframework.reactive.web.http.ServerHttpResponse;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;


/**
 * For now a simple {@code String} or {@code Publisher<String>} to
 * "text/plain;charset=UTF-8" conversion.
 *
 * @author Rossen Stoyanchev
 */
public class ResponseBodyResultHandler implements HandlerResultHandler, Ordered {

	private static final Charset UTF_8 = Charset.forName("UTF-8");


	private int order = Ordered.LOWEST_PRECEDENCE;


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	public boolean supports(HandlerResult result) {
		Object handler = result.getHandler();
		if (handler instanceof HandlerMethod) {
			Method method = ((HandlerMethod) handler).getMethod();
			return AnnotatedElementUtils.isAnnotated(method, ResponseBody.class.getName());
		}
		return false;
	}

	@Override
	public Publisher<Void> handleResult(ServerHttpRequest request, ServerHttpResponse response,
			HandlerResult result) {

		Object value = result.getValue();
		HandlerMethod handlerMethod = (HandlerMethod) result.getHandler();
		MethodParameter returnType = handlerMethod.getReturnValueType(value);

		if (value == null) {
			return Streams.empty();
		}

		if (value instanceof String) {
			response.getHeaders().setContentType(new MediaType("text", "plain", UTF_8));
			return response.writeWith(Streams.just(((String) value).getBytes(UTF_8)));
		}
		else if (value instanceof Publisher) {
			Class<?> type = ResolvableType.forMethodParameter(returnType).resolveGeneric(0);
			if (String.class.equals(type)) {
				@SuppressWarnings("unchecked")
				Publisher<String> content = (Publisher<String>) value;
				return response.writeWith(Streams.wrap(content).map(value1 -> value1.getBytes(UTF_8)));
			}
		}

		return Streams.fail(new IllegalStateException("Return value type not supported: " + returnType));
	}

}
