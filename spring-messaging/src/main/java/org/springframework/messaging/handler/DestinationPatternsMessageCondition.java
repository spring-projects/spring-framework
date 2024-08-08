/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.messaging.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.RouteMatcher;
import org.springframework.util.SimpleRouteMatcher;
import org.springframework.util.StringUtils;

/**
 * {@link MessageCondition} to match the destination header of a Message
 * against one or more patterns through a {@link RouteMatcher}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DestinationPatternsMessageCondition
		extends AbstractMessageCondition<DestinationPatternsMessageCondition> {

	/**
	 * The name of the "lookup destination" header.
	 */
	public static final String LOOKUP_DESTINATION_HEADER = "lookupDestination";


	private final Set<String> patterns;

	private final RouteMatcher routeMatcher;


	/**
	 * Constructor with patterns only. Creates and uses an instance of
	 * {@link AntPathMatcher} with default settings.
	 * <p>Non-empty patterns that don't start with "/" are prepended with "/".
	 * @param patterns the URL patterns to match to, or if 0 then always match
	 */
	public DestinationPatternsMessageCondition(String... patterns) {
		this(patterns, (PathMatcher) null);
	}

	/**
	 * Constructor with patterns and a {@code PathMatcher} instance.
	 * @param patterns the URL patterns to match to, or if 0 then always match
	 * @param matcher the {@code PathMatcher} to use
	 */
	public DestinationPatternsMessageCondition(String[] patterns, @Nullable PathMatcher matcher) {
		this(patterns, new SimpleRouteMatcher(matcher != null ? matcher : new AntPathMatcher()));
	}

	/**
	 * Constructor with patterns and a {@code RouteMatcher} instance.
	 * @param patterns the URL patterns to match to, or if 0 then always match
	 * @param routeMatcher the {@code RouteMatcher} to use
	 * @since 5.2
	 */
	public DestinationPatternsMessageCondition(String[] patterns, RouteMatcher routeMatcher) {
		this(Collections.unmodifiableSet(prependLeadingSlash(patterns, routeMatcher)), routeMatcher);
	}

	private static Set<String> prependLeadingSlash(String[] patterns, RouteMatcher routeMatcher) {
		boolean slashSeparator = routeMatcher.combine("a", "a").equals("a/a");
		Set<String> result = CollectionUtils.newLinkedHashSet(patterns.length);
		for (String pattern : patterns) {
			if (slashSeparator && StringUtils.hasLength(pattern) && !pattern.startsWith("/")) {
				pattern = "/" + pattern;
			}
			result.add(pattern);
		}
		return result;
	}

	private DestinationPatternsMessageCondition(Set<String> patterns, RouteMatcher routeMatcher) {
		this.patterns = patterns;
		this.routeMatcher = routeMatcher;
	}



	public Set<String> getPatterns() {
		return this.patterns;
	}

	@Override
	protected Collection<String> getContent() {
		return this.patterns;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}


	/**
	 * Returns a new instance with URL patterns from the current instance ("this") and
	 * the "other" instance as follows:
	 * <ul>
	 * <li>If there are patterns in both instances, combine the patterns in "this" with
	 * the patterns in "other" using {@link org.springframework.util.PathMatcher#combine(String, String)}.
	 * <li>If only one instance has patterns, use them.
	 * <li>If neither instance has patterns, use an empty String (i.e. "").
	 * </ul>
	 */
	@Override
	public DestinationPatternsMessageCondition combine(DestinationPatternsMessageCondition other) {
		Set<String> result = new LinkedHashSet<>();
		if (!this.patterns.isEmpty() && !other.patterns.isEmpty()) {
			for (String pattern1 : this.patterns) {
				for (String pattern2 : other.patterns) {
					result.add(this.routeMatcher.combine(pattern1, pattern2));
				}
			}
		}
		else if (!this.patterns.isEmpty()) {
			result.addAll(this.patterns);
		}
		else if (!other.patterns.isEmpty()) {
			result.addAll(other.patterns);
		}
		else {
			result.add("");
		}
		return new DestinationPatternsMessageCondition(result, this.routeMatcher);
	}

	/**
	 * Check if any of the patterns match the given Message destination and return an instance
	 * that is guaranteed to contain matching patterns, sorted via
	 * {@link org.springframework.util.PathMatcher#getPatternComparator(String)}.
	 * @param message the message to match to
	 * @return the same instance if the condition contains no patterns;
	 * or a new condition with sorted matching patterns;
	 * or {@code null} either if a destination can not be extracted or there is no match
	 */
	@Override
	@Nullable
	public DestinationPatternsMessageCondition getMatchingCondition(Message<?> message) {
		Object destination = message.getHeaders().get(LOOKUP_DESTINATION_HEADER);
		if (destination == null) {
			return null;
		}
		if (this.patterns.isEmpty()) {
			return this;
		}

		List<String> matches = null;
		for (String pattern : this.patterns) {
			if (pattern.equals(destination) || matchPattern(pattern, destination)) {
				if (matches == null) {
					matches = new ArrayList<>();
				}
				matches.add(pattern);
			}
		}
		if (CollectionUtils.isEmpty(matches)) {
			return null;
		}

		matches.sort(getPatternComparator(destination));
		return new DestinationPatternsMessageCondition(new LinkedHashSet<>(matches), this.routeMatcher);
	}

	private boolean matchPattern(String pattern, Object destination) {
		return destination instanceof RouteMatcher.Route route ? this.routeMatcher.match(pattern, route) :
				((SimpleRouteMatcher) this.routeMatcher).getPathMatcher().match(pattern, (String) destination);
	}

	private Comparator<String> getPatternComparator(Object destination) {
		return destination instanceof RouteMatcher.Route route ? this.routeMatcher.getPatternComparator(route) :
			((SimpleRouteMatcher) this.routeMatcher).getPathMatcher().getPatternComparator((String) destination);
	}

	/**
	 * Compare the two conditions based on the destination patterns they contain.
	 * Patterns are compared one at a time, from top to bottom via
	 * {@link org.springframework.util.PathMatcher#getPatternComparator(String)}.
	 * If all compared patterns match equally, but one instance has more patterns,
	 * it is considered a closer match.
	 * <p>It is assumed that both instances have been obtained via
	 * {@link #getMatchingCondition(Message)} to ensure they contain only patterns
	 * that match the request and are sorted with the best matches on top.
	 */
	@Override
	public int compareTo(DestinationPatternsMessageCondition other, Message<?> message) {
		Object destination = message.getHeaders().get(LOOKUP_DESTINATION_HEADER);
		if (destination == null) {
			return 0;
		}

		Comparator<String> patternComparator = getPatternComparator(destination);
		Iterator<String> iterator = this.patterns.iterator();
		Iterator<String> iteratorOther = other.patterns.iterator();
		while (iterator.hasNext() && iteratorOther.hasNext()) {
			int result = patternComparator.compare(iterator.next(), iteratorOther.next());
			if (result != 0) {
				return result;
			}
		}

		if (iterator.hasNext()) {
			return -1;
		}
		else if (iteratorOther.hasNext()) {
			return 1;
		}
		else {
			return 0;
		}
	}

}
