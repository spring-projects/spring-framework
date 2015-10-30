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

package org.springframework.reactive.web.dispatch.method;

import org.reactivestreams.Publisher;

import org.springframework.core.MethodParameter;
import org.springframework.http.server.ReactiveServerHttpRequest;


/**
 * @author Rossen Stoyanchev
 */
public interface HandlerMethodArgumentResolver {

	boolean supportsParameter(MethodParameter parameter);

	/**
	 * The returned Publisher must produce a single value. As Reactive Streams
	 * does not allow publishing null values, if the value may be {@code null}
	 * use {@link java.util.Optional#ofNullable(Object)} to wrap it.
	 */
	Publisher<Object> resolveArgument(MethodParameter parameter, ReactiveServerHttpRequest request);


}
