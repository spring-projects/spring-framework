/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
