/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.resource;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;

/**
 * Base {@link ResourceResolver} providing consistent logging.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractResourceResolver implements ResourceResolver {

	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public Mono<Resource> resolveResource(@Nullable ServerWebExchange exchange, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		return resolveResourceInternal(exchange, requestPath, locations, chain);
	}

	@Override
	public Mono<String> resolveUrlPath(String resourceUrlPath, List<? extends Resource> locations,
			ResourceResolverChain chain) {

		return resolveUrlPathInternal(resourceUrlPath, locations, chain);
	}


	protected abstract Mono<Resource> resolveResourceInternal(@Nullable ServerWebExchange exchange,
			String requestPath, List<? extends Resource> locations, ResourceResolverChain chain);

	protected abstract Mono<String> resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain);

}
