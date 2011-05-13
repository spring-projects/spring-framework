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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

/**
 * Represents a collection of consumes conditions, typically obtained from {@link org.springframework.web.bind.annotation.RequestMapping#consumes()
 * &#64;RequestMapping.consumes()}.
 *
 * @author Arjen Poutsma
 * @see RequestConditionFactory#parseConsumes(String...)
 * @see RequestConditionFactory#parseConsumes(String[], String[])
 * @since 3.1
 */
public class ConsumesRequestCondition
		extends MediaTypesRequestCondition<ConsumesRequestCondition.ConsumeRequestCondition>
		implements Comparable<ConsumesRequestCondition> {

	ConsumesRequestCondition(Collection<ConsumeRequestCondition> conditions) {
		super(conditions);
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
	 * Creates a default set of consumes request conditions.
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
	 * Combines this collection of request condition with another. Returns {@code other}, unless it has the default value
	 * (i.e. <code>&#42;/&#42;</code>).
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
		return this.getMostSpecificCondition().compareTo(other.getMostSpecificCondition());
	}

	static class ConsumeRequestCondition extends MediaTypesRequestCondition.MediaTypeRequestCondition {

		ConsumeRequestCondition(String expression) {
			super(expression);
		}

		ConsumeRequestCondition(MediaType mediaType, boolean negated) {
			super(mediaType, negated);
		}

		@Override
		protected boolean match(HttpServletRequest request, MediaType mediaType) {
			MediaType contentType = getContentType(request);
			return mediaType.includes(contentType);
		}

		private MediaType getContentType(HttpServletRequest request) {
			if (StringUtils.hasLength(request.getContentType())) {
				return MediaType.parseMediaType(request.getContentType());
			}
			else {
				return MediaType.APPLICATION_OCTET_STREAM;
			}
		}

	}
}
