/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.ui.Model;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolver for a controller method argument of type {@link Model} that can
 * also be resolved as a {@link java.util.Map}.
 *
 * <p>A Map return value can be interpreted in more than one way depending
 * on the presence of annotations like {@code @ModelAttribute} or
 * {@code @ResponseBody}. As of 5.2 this resolver returns false if a
 * parameter of type {@code Map} is also annotated.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class ModelMethodArgumentResolver extends HandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	public ModelMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry) {
		super(adapterRegistry);
	}


	@Override
	public boolean supportsParameter(MethodParameter param) {
		return checkParameterTypeNoReactiveWrapper(param, type ->
				Model.class.isAssignableFrom(type) ||
						(Map.class.isAssignableFrom(type) && param.getParameterAnnotations().length == 0));
	}

	@Override
	public Object resolveArgumentValue(
			MethodParameter parameter, BindingContext context, ServerWebExchange exchange) {

		Class<?> type = parameter.getParameterType();
		if (Model.class.isAssignableFrom(type)) {
			return context.getModel();
		}
		else if (Map.class.isAssignableFrom(type)) {
			return context.getModel().asMap();
		}
		else {
			// Should never happen..
			throw new IllegalStateException("Unexpected method parameter type: " + type);
		}
	}

}
