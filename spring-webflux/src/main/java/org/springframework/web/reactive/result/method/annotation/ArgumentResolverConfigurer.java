/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;

/**
 * Helps to configure resolvers for Controller method arguments.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ArgumentResolverConfigurer {

	private final List<HandlerMethodArgumentResolver> customResolvers = new ArrayList<>(8);


	/**
	 * Configure resolvers for custom controller method arguments.
	 * @param resolver the resolver(s) to add
	 */
	public void addCustomResolver(HandlerMethodArgumentResolver... resolver) {
		Assert.notNull(resolver, "'resolvers' must not be null");
		this.customResolvers.addAll(Arrays.asList(resolver));
	}


	List<HandlerMethodArgumentResolver> getCustomResolvers() {
		return this.customResolvers;
	}

}
