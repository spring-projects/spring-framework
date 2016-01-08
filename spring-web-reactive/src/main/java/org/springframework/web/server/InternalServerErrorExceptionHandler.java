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
package org.springframework.web.server;

import reactor.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * Handle any exception by setting the response status to 500.
 *
 * @author Rossen Stoyanchev
 */
public class InternalServerErrorExceptionHandler implements HttpExceptionHandler {


	@Override
	public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response, Throwable ex) {
		response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
		return Mono.empty();
	}

}
