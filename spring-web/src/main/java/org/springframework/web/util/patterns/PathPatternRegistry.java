/*
 * Copyright 2002-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.web.util.patterns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

/**
 * Registry that holds {@code PathPattern}s instances
 * sorted according to their specificity (most specific patterns first).
 * <p>For a given path pattern string, {@code PathPattern} variants
 * can be generated and registered automatically, depending
 * on the {@code useTrailingSlashMatch}, {@code useSuffixPatternMatch}
 * and {@code fileExtensions} properties.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public class PathPatternRegistry {

	private final PathPatternParser pathPatternParser;

	private final HashSet<PathPattern> patterns;

	private boolean useSuffixPatternMatch = false;

	private boolean useTrailingSlashMatch = false;

	private Set<String> fileExtensions = Collections.emptySet();

	/**
	 * Create a new {@code PathPatternRegistry} with defaults options for
	 * pattern variants generation.
	 * <p>By default, no pattern variant will be generated.
	 */
	public PathPatternRegistry() {
		this.pathPatternParser = new PathPatternParser();
		this.patterns = new HashSet<>();
	}

	public PathPatternRegistry(Set<PathPattern> patterns) {
		this();
		this.patterns.addAll(patterns);
	}

	/**
	 * Whether to match to paths irrespective of the presence of a trailing slash.
	 */
	public boolean useSuffixPatternMatch() {
		return useSuffixPatternMatch;
	}

	/**
	 * Whether to use suffix pattern match (".*") when matching patterns to
	 * requests. If enabled a path pattern such as "/users" will also
	 * generate the following pattern variant: "/users.*".
	 * <p>By default this is set to {@code false}.
	 */
	public void setUseSuffixPatternMatch(boolean useSuffixPatternMatch) {
		this.useSuffixPatternMatch = useSuffixPatternMatch;
	}

	/**
	 * Whether to generate path pattern variants with a trailing slash.
	 */
	public boolean useTrailingSlashMatch() {
		return useTrailingSlashMatch;
	}

	/**
	 * Whether to match to paths irrespective of the presence of a trailing slash.
	 * If enabled a path pattern such as "/users" will also generate the
	 * following pattern variant: "/users/".
	 * <p>The default value is {@code false}.
	 */
	public void setUseTrailingSlashMatch(boolean useTrailingSlashMatch) {
		this.useTrailingSlashMatch = useTrailingSlashMatch;
	}

	/**
	 * Return the set of file extensions to use for suffix pattern matching.
	 */
	public Set<String> getFileExtensions() {
		return fileExtensions;
	}

	/**
	 * Configure the set of file extensions to use for suffix pattern matching.
	 * For a given path "/users", each file extension will be used to
	 * generate a path pattern variant such as "json" -> "/users.json".
	 * <p>The default value is an empty {@code Set}
	 */
	public void setFileExtensions(Set<String> fileExtensions) {
		Set<String> fixedFileExtensions = (fileExtensions != null) ? fileExtensions.stream()
				.map(ext -> (ext.charAt(0) != '.') ? "." + ext : ext)
				.collect(Collectors.toSet()) : Collections.emptySet();
		this.fileExtensions = fixedFileExtensions;
	}

	/**
	 * Return a (read-only) set of all patterns for matching (including generated pattern variants).
	 */
	public Set<PathPattern> getPatterns() {
		return Collections.unmodifiableSet(this.patterns);
	}

	/**
	 * Return a {@code SortedSet} of {@code PathPattern}s matching the given {@code lookupPath}.
	 *
	 * <p>The returned set sorted with the most specific
	 * patterns first, according to the given {@code lookupPath}.
	 * @param lookupPath the URL lookup path to be matched against
	 */
	public SortedSet<PathPattern> findMatches(String lookupPath) {
		return this.patterns.stream()
				.filter(pattern -> pattern.matches(lookupPath))
				.collect(Collectors.toCollection(() ->
						new TreeSet<>(new PatternSetComparator(lookupPath))));
	}

	/**
	 * Process the path pattern data using the internal {@link PathPatternParser}
	 * instance, producing a {@link PathPattern} object that can be used for fast matching
	 * against paths.
	 *
	 * @param pathPattern the input path pattern, e.g. /foo/{bar}
	 * @return a PathPattern for quickly matching paths against the specified path pattern
	 */
	public PathPattern parsePattern(String pathPattern) {
		return this.pathPatternParser.parse(pathPattern);
	}

	/**
	 * Remove all {@link PathPattern}s from this registry
	 */
	public void clear() {
		this.patterns.clear();
	}

	/**
	 * Parse the given {@code rawPattern} and adds it to this registry,
	 * as well as pattern variants, depending on the given options and
	 * the nature of the input pattern.
	 * <p>The following set of patterns will be added:
	 * <ul>
	 * <li>the pattern given as input, e.g. "/foo/{bar}"
	 * <li>if {@link #useSuffixPatternMatch()}, variants for each given
	 * {@link #getFileExtensions()}, such as "/foo/{bar}.pdf" or a variant for all extensions,
	 * such as "/foo/{bar}.*"
	 * <li>if {@link #useTrailingSlashMatch()}, a variant such as "/foo/{bar}/"
	 * </ul>
	 * @param rawPattern raw path pattern to parse and register
	 * @return the list of {@link PathPattern} that were registered as a result
	 */
	public List<PathPattern> register(String rawPattern) {
		List<PathPattern> newPatterns = generatePathPatterns(rawPattern);
		this.patterns.addAll(newPatterns);
		return newPatterns;
	}

	private String prependLeadingSlash(String pattern) {
		if (StringUtils.hasLength(pattern) && !pattern.startsWith("/")) {
			return "/" + pattern;
		}
		else {
			return pattern;
		}
	}

	private List<PathPattern> generatePathPatterns(String rawPattern) {
		String fixedPattern = prependLeadingSlash(rawPattern);
		List<PathPattern> patterns = new ArrayList<>();
		PathPattern pattern = this.pathPatternParser.parse(fixedPattern);
		patterns.add(pattern);
		if (StringUtils.hasLength(fixedPattern) && !pattern.isCatchAll()) {
			if (this.useSuffixPatternMatch) {
				if (this.fileExtensions != null && !this.fileExtensions.isEmpty()) {
					for (String extension : this.fileExtensions) {
						patterns.add(this.pathPatternParser.parse(fixedPattern + extension));
					}
				}
				else {
					patterns.add(this.pathPatternParser.parse(fixedPattern + ".*"));
				}
			}
			if (this.useTrailingSlashMatch && !fixedPattern.endsWith("/")) {
				patterns.add(this.pathPatternParser.parse(fixedPattern + "/"));
			}
		}
		return patterns;
	}

	/**
	 * Parse the given {@code rawPattern} and removes it to this registry,
	 * as well as pattern variants, depending on the given options and
	 * the nature of the input pattern.
	 *
	 * @param rawPattern raw path pattern to parse and unregister
	 * @return the list of {@link PathPattern} that were unregistered as a result
	 */
	public List<PathPattern> unregister(String rawPattern) {
		List<PathPattern> unregisteredPatterns = generatePathPatterns(rawPattern);
		this.patterns.removeAll(unregisteredPatterns);
		return unregisteredPatterns;
	}


	/**
	 * Combine the patterns contained in the current registry
	 * with the ones in the other, into a new {@code PathPatternRegistry} instance.
	 * <p>Given the current registry contains "/prefix" and the other contains
	 * "/foo" and "/bar/{item}", the combined result will be: a new registry
	 * containing "/prefix/foo" and "/prefix/bar/{item}".
	 * @param other other {@code PathPatternRegistry} to combine with
	 * @return a new instance of {@code PathPatternRegistry} that combines both
	 * @see PathPattern#combine(String)
	 */
	public PathPatternRegistry combine(PathPatternRegistry other) {
		PathPatternRegistry result = new PathPatternRegistry();
		result.setUseSuffixPatternMatch(this.useSuffixPatternMatch);
		result.setUseTrailingSlashMatch(this.useTrailingSlashMatch);
		result.setFileExtensions(this.fileExtensions);
		if (!this.patterns.isEmpty() && !other.patterns.isEmpty()) {
			for (PathPattern pattern1 : this.patterns) {
				for (PathPattern pattern2 : other.patterns) {
					String combined = pattern1.combine(pattern2.getPatternString());
					result.register(combined);
				}
			}
		}
		else if (!this.patterns.isEmpty()) {
			result.patterns.addAll(this.patterns);
		}
		else if (!other.patterns.isEmpty()) {
			result.patterns.addAll(other.patterns);
		}
		else {
			result.register("");
		}
		return result;
	}

	/**
	 * Given a full path, returns a {@link Comparator} suitable for sorting pattern
	 * registries in order of explicitness for that path.
	 * <p>The returned {@code Comparator} will
	 * {@linkplain java.util.Collections#sort(java.util.List, java.util.Comparator) sort}
	 * a list so that more specific patterns registries come before generic ones.
	 * @param path the full path to use for comparison
	 * @return a comparator capable of sorting patterns in order of explicitness
	 */
	public Comparator<PathPatternRegistry> getComparator(final String path) {
		return (r1, r2) -> {
			PatternSetComparator comparator = new PatternSetComparator(path);
			Iterator<PathPattern> it1 = r1.patterns.stream()
					.sorted(comparator).collect(Collectors.toList()).iterator();
			Iterator<PathPattern> it2 = r2.patterns.stream()
					.sorted(comparator).collect(Collectors.toList()).iterator();
			while (it1.hasNext() && it2.hasNext()) {
				int result = comparator.compare(it1.next(), it2.next());
				if (result != 0) {
					return result;
				}
			}
			if (it1.hasNext()) {
				return -1;
			}
			else if (it2.hasNext()) {
				return 1;
			}
			else {
				return 0;
			}
		};
	}

	private class PatternSetComparator implements Comparator<PathPattern> {

		private final String path;

		public PatternSetComparator(String path) {
			this.path = path;
		}

		@Override
		public int compare(PathPattern o1, PathPattern o2) {
			// Nulls get sorted to the end
			if (o1 == null) {
				return (o2 == null ? 0 : +1);
			}
			else if (o2 == null) {
				return -1;
			}
			// exact matches get sorted first
			if (o1.getPatternString().equals(path)) {
				return (o2.getPatternString().equals(path)) ? 0 : -1;
			}
			else if (o2.getPatternString().equals(path)) {
				return +1;
			}
			// compare pattern specificity
			int result = o1.compareTo(o2);
			// if equal specificity, sort using pattern string
			if (result == 0) {
				return o1.getPatternString().compareTo(o2.getPatternString());
			}
			return result;
		}

	}

}
