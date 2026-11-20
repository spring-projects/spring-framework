/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

/**
 * Assist with configuring {@code HandlerMapping}'s with path matching options.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class PathMatchConfigurer {

	private @Nullable Boolean caseSensitiveMatch;

	private @Nullable Map<String, Predicate<Class<?>>> pathPrefixes;


	/**
	 * Whether to match to URLs irrespective of their case.
	 * If enabled a method mapped to "/users" won't match to "/Users/".
	 * <p>The default value is {@code false}.
	 */
	public PathMatchConfigurer setUseCaseSensitiveMatch(Boolean caseSensitiveMatch) {
		this.caseSensitiveMatch = caseSensitiveMatch;
		return this;
	}

	/**
	 * Configure a path prefix to apply to matching controller methods.
	 * <p>Prefixes are used to enrich the mappings of every {@code @RequestMapping}
	 * method whose controller type is matched by the corresponding
	 * {@code Predicate}. The prefix for the first matching predicate is used.
	 * <p>Consider using {@link org.springframework.web.method.HandlerTypePredicate
	 * HandlerTypePredicate} to group controllers.
	 * @param prefix the path prefix to apply
	 * @param predicate a predicate for matching controller types
	 * @since 5.1
	 */
	public PathMatchConfigurer addPathPrefix(String prefix, Predicate<Class<?>> predicate) {
		if (this.pathPrefixes == null) {
			this.pathPrefixes = new LinkedHashMap<>();
		}
		this.pathPrefixes.put(prefix, predicate);
		return this;
	}


	protected @Nullable Boolean isUseCaseSensitiveMatch() {
		return this.caseSensitiveMatch;
	}

	protected @Nullable Map<String, Predicate<Class<?>>> getPathPrefixes() {
		return this.pathPrefixes;
	}
}
