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
package org.springframework.http.server.reactive;

import java.util.Arrays;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.Publishers;
import reactor.core.publisher.convert.RxJava1Converter;
import rx.Observable;

import org.springframework.util.Assert;

/**
 * {@link HttpHandler} that delegates to a target {@link HttpHandler} and handles
 * any errors from it by invoking one or more {@link HttpExceptionHandler}s
 * sequentially until one of them completes successfully.
 *
 * @author Rossen Stoyanchev
 */
public class ErrorHandlingHttpHandler extends HttpHandlerDecorator {

	private final List<HttpExceptionHandler> exceptionHandlers;


	public ErrorHandlingHttpHandler(HttpHandler targetHandler, HttpExceptionHandler... exceptionHandlers) {
		super(targetHandler);
		Assert.notEmpty(exceptionHandlers, "At least one exception handler is required");
		this.exceptionHandlers = Arrays.asList(exceptionHandlers);
	}


	@Override
	public Publisher<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
		Publisher<Void> publisher;
		try {
			publisher = getDelegate().handle(request, response);
		}
		catch (Throwable ex) {
			publisher = Publishers.error(ex);
		}
		for (HttpExceptionHandler handler : this.exceptionHandlers) {
			publisher = applyExceptionHandler(publisher, handler, request, response);
		}
		return publisher;
	}

	private static Publisher<Void> applyExceptionHandler(Publisher<Void> publisher,
			HttpExceptionHandler handler, ServerHttpRequest request, ServerHttpResponse response) {

		// see https://github.com/reactor/reactor/issues/580

		Observable<Void> observable = RxJava1Converter.from(publisher).onErrorResumeNext(ex -> {
			return RxJava1Converter.from(handler.handle(request, response, ex));
		});
		return RxJava1Converter.from(observable);
	}

}
