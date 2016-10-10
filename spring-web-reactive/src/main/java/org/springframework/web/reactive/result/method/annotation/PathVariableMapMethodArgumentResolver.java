/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.method.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolver for {@link Map} method arguments also annotated with
 * {@link PathVariable @PathVariable} where the annotation does not specify a
 * path variable name. The resulting {@link Map} argument is a coyp of all URI
 * template name-value pairs.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see PathVariableMethodArgumentResolver
 */
public class PathVariableMapMethodArgumentResolver implements HandlerMethodArgumentResolver {


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		PathVariable ann = parameter.getParameterAnnotation(PathVariable.class);
		return (ann != null && (Map.class.isAssignableFrom(parameter.getParameterType()))
				&& !StringUtils.hasText(ann.value()));
	}

	/**
	 * Return a Map with all URI template variables or an empty map.
	 */
	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext bindingContext,
			ServerWebExchange exchange) {

		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		Optional<Object> value = exchange.getAttribute(name);
		return (value.isPresent() ? Mono.just(value.get()) : Mono.just(Collections.emptyMap()));
	}

}
