/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.handler;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.http.server.reactive.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Registry that holds {@code PathPattern}s instances
 * and allows matching against them with a lookup path.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public class PathPatternRegistry<T> {

	private final PathPatternParser pathPatternParser;

	private final Map<PathPattern, T> patternsMap;


	/**
	 * Create a new {@code PathPatternRegistry} with
	 * a default instance of {@link PathPatternParser}.
	 */
	public PathPatternRegistry() {
		this(new PathPatternParser());
	}

	/**
	 * Create a new {@code PathPatternRegistry} using
	 * the provided instance of {@link PathPatternParser}.
	 * @param patternParser the {@link PathPatternParser} to use
	 */
	public PathPatternRegistry(PathPatternParser patternParser) {
		this(patternParser, Collections.emptyMap());
	}

	/**
	 * Create a new {@code PathPatternRegistry} using
	 * the provided instance of {@link PathPatternParser}
	 * and the given map of {@link PathPattern}.
	 * @param patternParser the {@link PathPatternParser} to use
	 * @param patternsMap the map of {@link PathPattern} to use
	 */
	public PathPatternRegistry(PathPatternParser patternParser, Map<PathPattern, T> patternsMap) {
		this.pathPatternParser = patternParser;
		this.patternsMap = new HashMap<>(patternsMap);
	}


	/**
	 * Return a (read-only) map of all patterns and associated values.
	 */
	public Map<PathPattern, T> getPatternsMap() {
		return Collections.unmodifiableMap(this.patternsMap);
	}


	/**
	 * Parse the given {@code rawPattern} and adds it to this registry.
	 * @param rawPattern raw path pattern to parse and register
	 * @param handler the associated handler object
	 */
	public void register(String rawPattern, T handler) {
		String fixedPattern = prependLeadingSlash(rawPattern);
		PathPattern newPattern = this.pathPatternParser.parse(fixedPattern);
		this.patternsMap.put(newPattern, handler);
	}

	private static String prependLeadingSlash(String pattern) {
		if (StringUtils.hasLength(pattern) && !pattern.startsWith("/")) {
			return "/" + pattern;
		}
		else {
			return pattern;
		}
	}


	/**
	 * Return patterns matching the given {@code lookupPath}.
	 * <p>The returned set sorted with the most specific
	 * patterns first, according to the given {@code lookupPath}.
	 * @param lookupPath the URL lookup path to be matched against
	 */
	public SortedSet<PathMatchResult<T>> findMatches(String lookupPath) {
		return this.patternsMap.entrySet().stream()
				.filter(entry -> entry.getKey().matches(lookupPath))
				.sorted(Comparator.comparing(Map.Entry::getKey))
				.map(entry -> new PathMatchResult<>(entry.getKey(), entry.getValue()))
				.collect(Collectors.toCollection(TreeSet::new));
	}

	/**
	 * Return, if any, the most specific {@code PathPattern} matching the given {@code lookupPath}.
	 * @param lookupPath the URL lookup path to be matched against
	 */
	@Nullable
	public PathMatchResult<T> findFirstMatch(PathContainer lookupPath) {
		return this.patternsMap.entrySet().stream()
				.filter(entry -> entry.getKey().matches(lookupPath))
				.sorted(Comparator.comparing(Map.Entry::getKey))
				.findFirst()
				.map(entry -> new PathMatchResult<>(entry.getKey(), entry.getValue()))
				.orElse(null);
	}

	/**
	 * Remove all {@link PathPattern}s from this registry
	 */
	public void clear() {
		this.patternsMap.clear();
	}

}
