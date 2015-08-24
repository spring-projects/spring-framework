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
package org.springframework.reactive.web;

import java.util.ArrayList;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.core.reactivestreams.PublisherFactory;
import reactor.core.reactivestreams.SubscriberWithContext;
import reactor.rx.Streams;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

/**
 * @author Rossen Stoyanchev
 */
public class DispatcherHttpHandler implements HttpHandler {

	private List<HandlerMapping> handlerMappings;

	private List<HandlerAdapter> handlerAdapters;

	private List<HandlerResultHandler> resultHandlers;


	protected void initStrategies(ApplicationContext context) {

		this.handlerMappings = new ArrayList<>(BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerMapping.class, true, false).values());

		this.handlerAdapters = new ArrayList<>(BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerAdapter.class, true, false).values());

		this.resultHandlers = new ArrayList<>(BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerResultHandler.class, true, false).values());
	}


	@Override
	public Publisher<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {

		Object handler = getHandler(request);
		if (handler == null) {
			// No exception handling mechanism yet
			response.setStatusCode(HttpStatus.NOT_FOUND);
			return PublisherFactory.forEach(SubscriberWithContext::onComplete);
		}

		HandlerAdapter handlerAdapter = getHandlerAdapter(handler);
		Publisher<HandlerResult> resultPublisher = handlerAdapter.handle(request, response, handler);

		return Streams.wrap(resultPublisher).concatMap((HandlerResult result) -> {
			for (HandlerResultHandler resultHandler : resultHandlers) {
				if (resultHandler.supports(result)) {
					return resultHandler.handleResult(request, response, result);
				}
			}
			return Streams.fail(new IllegalStateException(
					"No HandlerResultHandler for " + result.getReturnValue()));
		});
	}

	protected Object getHandler(ServerHttpRequest request) {
		Object handler = null;
		for (HandlerMapping handlerMapping : this.handlerMappings) {
			handler = handlerMapping.getHandler(request);
			if (handler != null) {
				break;
			}
		}
		return handler;
	}

	protected HandlerAdapter getHandlerAdapter(Object handler) {
		for (HandlerAdapter handlerAdapter : this.handlerAdapters) {
			if (handlerAdapter.supports(handler)) {
				return handlerAdapter;
			}
		}
		// more specific exception
		throw new IllegalStateException("No HandlerAdapter for " + handler);
	}

}
