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

package org.springframework.web.reactive.handler;

import java.util.HashMap;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.WebServerExchange;

/**
 * @author Rossen Stoyanchev
 */
public class SimpleUrlHandlerMapping implements HandlerMapping {

	private final Map<String, Object> handlerMap = new HashMap<>();


	public void setHandlers(Map<String, Object> handlers) {
		this.handlerMap.clear();
		if (handlers != null) {
			this.handlerMap.putAll(handlers);
		}
	}


	@Override
	public Mono<Object> getHandler(WebServerExchange exchange) {
		return Flux.create(subscriber -> {
			String path = exchange.getRequest().getURI().getPath();
			Object handler = this.handlerMap.get(path);
			if (handler != null) {
				subscriber.onNext(handler);
			}
			subscriber.onComplete();
		}).next();
	}

}
