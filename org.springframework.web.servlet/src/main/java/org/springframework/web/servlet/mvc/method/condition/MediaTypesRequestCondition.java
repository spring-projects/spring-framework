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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;

/**
 * @author Arjen Poutsma
 */
class MediaTypesRequestCondition<T extends MediaTypesRequestCondition.MediaTypeRequestCondition>
		extends LogicalDisjunctionRequestCondition<T> {

	private final List<T> sortedConditions;

	public MediaTypesRequestCondition(Collection<T> conditions) {
		super(conditions);
		Assert.notEmpty(conditions, "'conditions' must not be empty");
		sortedConditions = new ArrayList<T>(conditions);
		Collections.sort(sortedConditions);
	}

	private MediaTypeRequestCondition getMostSpecificCondition(Collection<T> conditions) {
		List<MediaTypeRequestCondition> conditionList = new ArrayList<MediaTypeRequestCondition>(conditions);
		Collections.sort(conditionList);
		return conditionList.get(0);
	}

	protected MediaTypeRequestCondition getMostSpecificCondition() {
		return sortedConditions.get(0);
	}

	protected List<T> getSortedConditions() {
		return sortedConditions;
	}

	/**
	 * Returns all {@link MediaType}s contained in this condition.
	 */
	public Set<MediaType> getMediaTypes() {
		Set<MediaType> result = new LinkedHashSet<MediaType>();
		for (MediaTypeRequestCondition condition : getConditions()) {
			result.add(condition.getMediaType());
		}
		return result;
	}


	/**
	 * @author Arjen Poutsma
	 */
	protected abstract static class MediaTypeRequestCondition
			implements RequestCondition, Comparable<MediaTypeRequestCondition> {

		private final MediaType mediaType;

		private final boolean isNegated;

		MediaTypeRequestCondition(MediaType mediaType, boolean negated) {
			this.mediaType = mediaType;
			isNegated = negated;
		}

		MediaTypeRequestCondition(String expression) {
			if (expression.startsWith("!")) {
				isNegated = true;
				expression = expression.substring(1);
			}
			else {
				isNegated = false;
			}
			this.mediaType = MediaType.parseMediaType(expression);
		}

		public boolean match(HttpServletRequest request) {
			boolean match = match(request, this.mediaType);
			return !isNegated ? match : !match;
		}

		protected abstract boolean match(HttpServletRequest request, MediaType mediaType);

		MediaType getMediaType() {
			return mediaType;
		}

		public int compareTo(MediaTypeRequestCondition other) {
			return MediaType.SPECIFICITY_COMPARATOR.compare(this.getMediaType(), other.getMediaType());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj != null && getClass().equals(obj.getClass())) {
				MediaTypeRequestCondition other = (MediaTypeRequestCondition) obj;
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
