/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Represents a collection of consumes conditions, typically obtained from {@link
 * org.springframework.web.bind.annotation.RequestMapping#consumes() @RequestMapping.consumes()}.
 *
 * @author Arjen Poutsma
 * @see RequestConditionFactory#parseHeaders(String...)
 * @since 3.1
 */
public class ConsumesRequestCondition
		extends LogicalDisjunctionRequestCondition<ConsumesRequestCondition.ConsumeRequestCondition>
		implements Comparable<ConsumesRequestCondition> {

	private final ConsumeRequestCondition mostSpecificCondition;

	ConsumesRequestCondition(Collection<ConsumeRequestCondition> conditions) {
		super(conditions);
		Assert.notEmpty(conditions, "'conditions' must not be empty");
		mostSpecificCondition = getMostSpecificCondition();
	}

	private ConsumeRequestCondition getMostSpecificCondition() {
		List<ConsumeRequestCondition> conditions = new ArrayList<ConsumeRequestCondition>(getConditions());
		Collections.sort(conditions);
		return conditions.get(0);
	}

	ConsumesRequestCondition(String... consumes) {
		this(parseConditions(Arrays.asList(consumes)));
	}

	private static Set<ConsumeRequestCondition> parseConditions(List<String> consumes) {
		if (consumes.isEmpty()) {
			consumes = Collections.singletonList("*/*");
		}
		Set<ConsumeRequestCondition> conditions = new LinkedHashSet<ConsumeRequestCondition>(consumes.size());
		for (String consume : consumes) {
			conditions.add(new ConsumeRequestCondition(consume));
		}
		return conditions;
	}

	/**
	 * Creates an default set of consumes request conditions.
	 */
	public ConsumesRequestCondition() {
		this(Collections.singleton(new ConsumeRequestCondition(MediaType.ALL, false)));
	}

	/**
	 * Returns a new {@code RequestCondition} that contains all conditions of this key that match the request.
	 *
	 * @param request the request
	 * @return a new request condition that contains all matching attributes, or {@code null} if not all conditions match
	 */
	public ConsumesRequestCondition getMatchingCondition(HttpServletRequest request) {
		Set<ConsumeRequestCondition> matchingConditions = new LinkedHashSet<ConsumeRequestCondition>(getConditions());
		for (Iterator<ConsumeRequestCondition> iterator = matchingConditions.iterator(); iterator.hasNext();) {
			ConsumeRequestCondition condition = iterator.next();
			if (!condition.match(request)) {
				iterator.remove();
			}
		}
		if (matchingConditions.isEmpty()) {
			return null;
		}
		else {
			return new ConsumesRequestCondition(matchingConditions);
		}
	}

	/**
	 * Combines this collection of request condition with another. Returns {@code other}, unless it has the default
	 * value (i.e. <code>&#42;/&#42;</code>).
	 *
	 * @param other the condition to combine with
	 */
	public ConsumesRequestCondition combine(ConsumesRequestCondition other) {
		return !other.hasDefaultValue() ? other : this;
	}

	private boolean hasDefaultValue() {
		Set<ConsumeRequestCondition> conditions = getConditions();
		if (conditions.size() == 1) {
			ConsumeRequestCondition condition = conditions.iterator().next();
			return condition.getMediaType().equals(MediaType.ALL);
		}
		else {
			return false;
		}
	}

	public int compareTo(ConsumesRequestCondition other) {
		return this.mostSpecificCondition.compareTo(other.mostSpecificCondition);
	}

	private static MediaType getContentType(HttpServletRequest request) {
		if (StringUtils.hasLength(request.getContentType())) {
			return MediaType.parseMediaType(request.getContentType());
		}
		else {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}

	static class ConsumeRequestCondition implements RequestCondition, Comparable<ConsumeRequestCondition> {

		private final MediaType mediaType;

		private final boolean isNegated;

		ConsumeRequestCondition(String expression) {
			if (expression.startsWith("!")) {
				isNegated = true;
				expression = expression.substring(1);
			}
			else {
				isNegated = false;
			}
			this.mediaType = MediaType.parseMediaType(expression);
		}

		ConsumeRequestCondition(MediaType mediaType, boolean isNegated) {
			this.mediaType = mediaType;
			this.isNegated = isNegated;
		}

		public boolean match(HttpServletRequest request) {
			MediaType contentType = getContentType(request);
			boolean match = this.mediaType.includes(contentType);
			return !isNegated ? match : !match;
		}

		public int compareTo(ConsumeRequestCondition other) {
			return MediaType.SPECIFICITY_COMPARATOR.compare(this.mediaType, other.mediaType);
		}

		MediaType getMediaType() {
			return mediaType;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj != null && obj instanceof ConsumeRequestCondition) {
				ConsumeRequestCondition other = (ConsumeRequestCondition) obj;
				return (this.mediaType.equals(other.mediaType)) && (this.isNegated == other.isNegated);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return mediaType.hashCode();
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			if (isNegated) {
				builder.append('!');
			}
			builder.append(mediaType.toString());
			return builder.toString();
		}

	}
}
