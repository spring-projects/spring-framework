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
 * Represents a collection of produces conditions, typically obtained from {@link
 * org.springframework.web.bind.annotation.RequestMapping#produces() &#64;RequestMapping.produces()}.
 *
 * @author Arjen Poutsma
 * @see RequestConditionFactory#parseProduces(String...)
 * @see RequestConditionFactory#parseProduces(String[], String[])
 * @since 3.1
 */
public class ProducesRequestCondition
		extends MediaTypesRequestCondition<ProducesRequestCondition.ProduceRequestCondition> {

	ProducesRequestCondition(Collection<ProduceRequestCondition> conditions) {
		super(conditions);
	}


	ProducesRequestCondition(String... consumes) {
		this(parseConditions(Arrays.asList(consumes)));
	}

	private static Set<ProduceRequestCondition> parseConditions(List<String> consumes) {
		if (consumes.isEmpty()) {
			consumes = Collections.singletonList("*/*");
		}
		Set<ProduceRequestCondition> conditions = new LinkedHashSet<ProduceRequestCondition>(consumes.size());
		for (String consume : consumes) {
			conditions.add(new ProduceRequestCondition(consume));
		}
		return conditions;
	}

	/**
	 * Creates an default set of consumes request conditions.
	 */
	public ProducesRequestCondition() {
		this(Collections.singleton(new ProduceRequestCondition(MediaType.ALL, false)));
	}

	/**
	 * Returns a new {@code RequestCondition} that contains all conditions of this key that match the request.
	 *
	 * @param request the request
	 * @return a new request condition that contains all matching attributes, or {@code null} if not all conditions match
	 */
	public ProducesRequestCondition getMatchingCondition(HttpServletRequest request) {
		Set<ProduceRequestCondition> matchingConditions = new LinkedHashSet<ProduceRequestCondition>(getConditions());
		for (Iterator<ProduceRequestCondition> iterator = matchingConditions.iterator(); iterator.hasNext();) {
			ProduceRequestCondition condition = iterator.next();
			if (!condition.match(request)) {
				iterator.remove();
			}
		}
		if (matchingConditions.isEmpty()) {
			return null;
		}
		else {
			return new ProducesRequestCondition(matchingConditions);
		}
	}

	/**
	 * Combines this collection of request condition with another. Returns {@code other}, unless it has the default
	 * value (i.e. {@code &#42;/&#42;}).
	 *
	 * @param other the condition to combine with
	 */
	public ProducesRequestCondition combine(ProducesRequestCondition other) {
		return !other.hasDefaultValue() ? other : this;
	}

	private boolean hasDefaultValue() {
		Set<ProduceRequestCondition> conditions = getConditions();
		if (conditions.size() == 1) {
			ProduceRequestCondition condition = conditions.iterator().next();
			return condition.getMediaType().equals(MediaType.ALL);
		}
		else {
			return false;
		}
	}

	public int compareTo(ProducesRequestCondition other, List<MediaType> acceptedMediaTypes) {
		for (MediaType acceptedMediaType : acceptedMediaTypes) {
			int thisIndex = this.indexOfMediaType(acceptedMediaType);
			int otherIndex = other.indexOfMediaType(acceptedMediaType);
			if (thisIndex != otherIndex) {
				return otherIndex - thisIndex;
			} else if (thisIndex != -1 && otherIndex != -1) {
				ProduceRequestCondition thisCondition = this.getSortedConditions().get(thisIndex);
				ProduceRequestCondition otherCondition = other.getSortedConditions().get(otherIndex);
				int result = thisCondition.compareTo(otherCondition);
				if (result != 0) {
					return result;
				}
			}
		}
		return 0;
	}

	private int indexOfMediaType(MediaType mediaType) {
		List<ProduceRequestCondition> sortedConditions = getSortedConditions();
		for (int i = 0; i < sortedConditions.size(); i++) {
			ProduceRequestCondition condition = sortedConditions.get(i);
			if (mediaType.includes(condition.getMediaType())) {
				return i;
			}
		}
		return -1;
	}

	static class ProduceRequestCondition extends MediaTypesRequestCondition.MediaTypeRequestCondition {

		ProduceRequestCondition(MediaType mediaType, boolean negated) {
			super(mediaType, negated);
		}

		ProduceRequestCondition(String expression) {
			super(expression);
		}

		@Override
		protected boolean match(HttpServletRequest request, MediaType mediaType) {
			List<MediaType> acceptedMediaTypes = getAccept(request);
			for (MediaType acceptedMediaType : acceptedMediaTypes) {
				if (mediaType.isCompatibleWith(acceptedMediaType)) {
					return true;
				}
			}
			return false;
		}

		private List<MediaType> getAccept(HttpServletRequest request) {
			String acceptHeader = request.getHeader("Accept");
			if (StringUtils.hasLength(acceptHeader)) {
				return MediaType.parseMediaTypes(acceptHeader);
			}
			else {
				return Collections.singletonList(MediaType.ALL);
			}
		}
	}
}
