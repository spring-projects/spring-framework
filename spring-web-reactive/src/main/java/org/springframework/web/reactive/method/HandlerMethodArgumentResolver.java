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

package org.springframework.web.reactive.method;

import reactor.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.http.server.reactive.ServerHttpRequest;


/**
 * @author Rossen Stoyanchev
 */
public interface HandlerMethodArgumentResolver {


	boolean supportsParameter(MethodParameter parameter);

	/**
	 * The returned {@link Mono} may produce one or zero values if the argument
	 * does not resolve to any value, which will result in {@code null} passed
	 * as the argument value.
	 */
	Mono<Object> resolveArgument(MethodParameter parameter, ServerHttpRequest request);

}
