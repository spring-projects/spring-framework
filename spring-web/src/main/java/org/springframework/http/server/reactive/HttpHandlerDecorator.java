/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.server.reactive;

import org.springframework.context.ApplicationContext;

import java.util.function.UnaryOperator;

/**
 * Allows registering a bean that will decorate the instance of {@link HttpHandler},
 * used by {@link org.springframework.web.server.adapter.WebHttpHandlerBuilder#applicationContext(ApplicationContext)};
 */
public interface HttpHandlerDecorator extends UnaryOperator<HttpHandler> {

	static HttpHandlerDecorator identity() {
		return x -> x;
	}

}
